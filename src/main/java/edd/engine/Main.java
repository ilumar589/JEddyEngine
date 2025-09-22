package edd.engine;

import edd.engine.util.SdlGpuUtil;
import edd.engine.util.SdlLogUtil;
import org.libsdl3.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.libsdl3.SDL3_1.SDLK_ESCAPE;
import static org.libsdl3.SDL3_1.SDL_GPU_SHADERFORMAT_SPIRV;

public class Main {
    static void main(String[] args) {
        try (var app = Arena.ofConfined()) {
            if (!SDL3.SDL_Init(SDL3.SDL_INIT_VIDEO())) {
                SdlLogUtil.error(
                        app,
                        SDL3.SDL_LOG_CATEGORY_APPLICATION(),
                        String.format("SDL_Init failed: %s", SdlLogUtil.lastSdlError())
                );
                throw new IllegalStateException("SDL_Init failed");
            }
            SdlLogUtil.info(app, "SDL initialized.");
            var windowTitle = app.allocateFrom("Hello SDL3");

            var window = SDL3.SDL_CreateWindow(windowTitle, 1280, 780, 0);
            if (window.equals(MemorySegment.NULL)) {
                SdlLogUtil.error(
                        app,
                        SDL3.SDL_LOG_CATEGORY_APPLICATION(),
                        String.format("SDL_CreateWindow failed: %s", SdlLogUtil.lastSdlError())
                );
                throw new IllegalStateException("SDL_CreateWindow failed");
            }
            SdlLogUtil.info(app, "Window created (1280x780).");

            var gpuDevice = SDL3.SDL_CreateGPUDevice(SDL_GPU_SHADERFORMAT_SPIRV(), true, MemorySegment.NULL);
            if (gpuDevice.equals(MemorySegment.NULL)) {
                SdlLogUtil.error(
                        app,
                        SDL3.SDL_LOG_CATEGORY_RENDER(),
                        String.format("SDL_CreateGPUDevice failed: %s", SdlLogUtil.lastSdlError())
                );
                throw new IllegalStateException("SDL_CreateGPUDevice failed");
            }
            SdlLogUtil.info(app, "GPU device created (SPIR-V).");

            if (!SDL3.SDL_ClaimWindowForGPUDevice(gpuDevice, window)) {
                SdlLogUtil.error(
                        app,
                        SDL3.SDL_LOG_CATEGORY_RENDER(),
                        String.format("SDL_ClaimWindowForGPUDevice failed: %s", SdlLogUtil.lastSdlError())
                );
                throw new IllegalStateException("SDL_ClaimWindowForGPUDevice failed");
            }
            SdlLogUtil.debug(app, "Window claimed for GPU.");

            boolean running = true;
            while (running) {
                running = pollEvents();

                // ——— Render ———
                var cmdBuffer = SDL3.SDL_AcquireGPUCommandBuffer(gpuDevice);
                if (cmdBuffer.equals(MemorySegment.NULL)) {
                    SdlLogUtil.debug(
                            app,
                            String.format("AcquireGPUCommandBuffer returned NULL; skipping frame. Err: %s",
                                    SdlLogUtil.lastSdlError())
                    );
                    continue;
                }

                try (var frame = Arena.ofConfined()) {
                    var acquired = SdlGpuUtil.acquireSwapchainTexture(cmdBuffer, window, frame);
                    if (acquired == null) {
                        SdlLogUtil.trace(frame, "Swapchain not ready / window minimized; frame skipped.");
                        continue;
                    }

                    var colorTarget = SdlGpuUtil.makeColorTarget(
                            frame, acquired.texture(), 0.0f, 0.2f, 0.4f, 1.0f
                    );

                    var renderPass = SDL3.SDL_BeginGPURenderPass(cmdBuffer, colorTarget, 1, MemorySegment.NULL);
                    if (renderPass.equals(MemorySegment.NULL)) {
                        SdlLogUtil.error(
                                frame,
                                SDL3.SDL_LOG_CATEGORY_RENDER(),
                                String.format("SDL_BeginGPURenderPass failed: %s", SdlLogUtil.lastSdlError())
                        );
                    } else {
                        // TODO: draw calls…
                        SDL3.SDL_EndGPURenderPass(renderPass);
                    }

                    if (!SDL3.SDL_SubmitGPUCommandBuffer(cmdBuffer)) {
                        SdlLogUtil.error(
                                frame,
                                SDL3.SDL_LOG_CATEGORY_RENDER(),
                                String.format("SDL_SubmitGPUCommandBuffer failed: %s", SdlLogUtil.lastSdlError())
                        );
                        throw new IllegalStateException("SDL_SubmitGPUCommandBuffer failed");
                    } else {
                        SdlLogUtil.trace(frame, "Frame submitted.");
                    }
                }
            }

            SdlLogUtil.info(app, "Shutting down…");
            SDL3.SDL_DestroyWindow(window);
            SDL3.SDL_Quit();
        }
    }

    /** Drain the SDL event queue; return false to quit. */
    private static boolean pollEvents() {
        try (var arena = Arena.ofConfined()) {
            var ev = SDL_Event.allocate(arena);
            while (SDL3.SDL_PollEvent(ev)) {
                int type = SDL_Event.type(ev);

                if (type == SDL3.SDL_EVENT_QUIT()) {
                    SdlLogUtil.info(arena, "SDL_EVENT_QUIT received.");
                    return false;
                }
                if (type == SDL3.SDL_EVENT_WINDOW_CLOSE_REQUESTED()) {
                    SdlLogUtil.info(arena, "Window close requested.");
                    return false;
                }
                if (type == SDL3.SDL_EVENT_KEY_DOWN()) {
                    var key = SDL_Event.key(ev);
                    if (SDL_KeyboardEvent.key(key) == SDLK_ESCAPE()) {
                        SdlLogUtil.info(arena, "Escape pressed. Exiting.");
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
