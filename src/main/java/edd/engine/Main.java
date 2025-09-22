package edd.engine;

import edd.engine.util.SdlGpu;
import org.libsdl3.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.libsdl3.SDL3_1.SDLK_ESCAPE;
import static org.libsdl3.SDL3_1.SDL_GPU_SHADERFORMAT_SPIRV;
import static org.libsdl3.SDL3_2.SDL_GPU_LOADOP_CLEAR;
import static org.libsdl3.SDL3_2.SDL_GPU_STOREOP_STORE;


public class Main {
    static void main() {
        if (!SDL3.SDL_Init(SDL3.SDL_INIT_VIDEO())) throw new IllegalStateException("SDL_Init failed");

        try (var app = Arena.ofConfined()) {
            var windowTitle = app.allocateFrom("Hello SDL3");
            var window = SDL3.SDL_CreateWindow(windowTitle, 1280, 780, 0);
            if (window.equals(MemorySegment.NULL)) throw new IllegalStateException("SDL_CreateWindow failed");

            var gpuDevice = SDL3.SDL_CreateGPUDevice(SDL_GPU_SHADERFORMAT_SPIRV(), true, MemorySegment.NULL);
            if (gpuDevice.equals(MemorySegment.NULL)) throw new IllegalStateException("SDL_CreateGPUDevice failed");

            if (!SDL3.SDL_ClaimWindowForGPUDevice(gpuDevice, window))
                throw new IllegalStateException("SDL_ClaimWindowForGPUDevice failed");

            boolean running = true;
            while (running) {
                running = pollEvents();

                // ——— Render ———
                var cmdBuffer = SDL3.SDL_AcquireGPUCommandBuffer(gpuDevice);
                if (cmdBuffer.equals(MemorySegment.NULL)) {
                    // rare, just skip this frame
                    continue;
                }

                try (var frame = Arena.ofConfined()) {
                    var acquired = SdlGpu.acquireSwapchainTexture(cmdBuffer, window, frame);
                    if (acquired == null) {
                        // minimized / not ready; helper already canceled the cmd buffer
                        continue;
                    }

                    var colorTarget = SdlGpu.makeColorTarget(frame, acquired.texture(),
                            0.0f, 0.2f, 0.4f, 1.0f);

                    var renderPass = SDL3.SDL_BeginGPURenderPass(cmdBuffer, colorTarget, 1, MemorySegment.NULL);

                    // TODO: draw calls here…

                    SDL3.SDL_EndGPURenderPass(renderPass);

                    if (!SDL3.SDL_SubmitGPUCommandBuffer(cmdBuffer)) {
                        throw new IllegalStateException("SDL_SubmitGPUCommandBuffer failed");
                    }
                }
            }

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

                if (type == SDL3.SDL_EVENT_QUIT()) return false;
                if (type == SDL3.SDL_EVENT_WINDOW_CLOSE_REQUESTED()) return false;

                if (type == SDL3.SDL_EVENT_KEY_DOWN()) {
                    var key = SDL_Event.key(ev);
                    if (SDL_KeyboardEvent.key(key) == SDLK_ESCAPE()) return false;
                }
            }
            return true;
        }
    }
}
