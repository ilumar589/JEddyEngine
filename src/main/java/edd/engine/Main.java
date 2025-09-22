package edd.engine;

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
        final var sdlInitOk = SDL3.SDL_Init(SDL3.SDL_INIT_VIDEO());
        assert sdlInitOk;

        try (final var arena = Arena.ofConfined()) {
            final var windowTitle = arena.allocateFrom("Hello SDL3");

            final var window = SDL3.SDL_CreateWindow(windowTitle, 1280, 780, 0);
            assert window != MemorySegment.NULL;

            final var gpuDevice = SDL3.SDL_CreateGPUDevice(SDL_GPU_SHADERFORMAT_SPIRV(),
                    true,
                    MemorySegment.NULL);
            assert gpuDevice != MemorySegment.NULL;

            final var gpuClaimed = SDL3.SDL_ClaimWindowForGPUDevice(gpuDevice, window);
            assert gpuClaimed;

            boolean running = true;
            while (running) {
                // poll events
                running = pollEvents(arena);

                // update game state

                // render

                final var cmdBuffer = SDL3.SDL_AcquireGPUCommandBuffer(gpuDevice);
                final var swapChainTexture = arena.allocate(ValueLayout.ADDRESS); //SDL_GPU_Texture**
                final var acquireOk = SDL3.SDL_WaitAndAcquireGPUSwapchainTexture(cmdBuffer,
                        window,
                        swapChainTexture,
                        MemorySegment.NULL,
                        MemorySegment.NULL);
                assert acquireOk;



                final var colorTargetInfo = SDL_GPUColorTargetInfo.allocate(arena);
                SDL_GPUColorTargetInfo.texture(colorTargetInfo, swapChainTexture.get(ValueLayout.ADDRESS, 0));
                SDL_GPUColorTargetInfo.load_op(colorTargetInfo, SDL_GPU_LOADOP_CLEAR());

                final var clearColor = SDL_FColor.allocate(arena);
                SDL_FColor.r(clearColor, 0.0f);
                SDL_FColor.g(clearColor, 0.2f);
                SDL_FColor.b(clearColor, 0.4f);
                SDL_FColor.a(clearColor, 1.0f);

                SDL_GPUColorTargetInfo.clear_color(colorTargetInfo, clearColor);
                SDL_GPUColorTargetInfo.store_op(colorTargetInfo, SDL_GPU_STOREOP_STORE());

                final var renderPass = SDL3.SDL_BeginGPURenderPass(cmdBuffer, colorTargetInfo, 1, MemorySegment.NULL);

                // draw stuff

                SDL3.SDL_EndGPURenderPass(renderPass);

                // more render passes

                // submit command buffer
                final var submitOk = SDL3.SDL_SubmitGPUCommandBuffer(cmdBuffer);
                assert submitOk;

            }

            SDL3.SDL_DestroyWindow(window);
            SDL3.SDL_Quit();
        }
    }

    // returns true if the events keep going
    private static boolean pollEvents(Arena arena) {
        final var sdlEvent = SDL_Event.allocate(arena);

        while (SDL3.SDL_PollEvent(sdlEvent)) {
            int type = SDL_Event.type(sdlEvent);

            // QUIT
            if (type == SDL3.SDL_EVENT_QUIT()) {
                return false;
            }

            if (isEscapePressed(type, sdlEvent)) {
                return false;
            }

            // window closed
            if (type == SDL3.SDL_EVENT_WINDOW_CLOSE_REQUESTED()) {
                return false;
            }

            return true;
        }

        return true;
    }

    private static boolean isEscapePressed(int sdlEventType, MemorySegment sdlEvent) {
        if (sdlEventType == SDL3.SDL_EVENT_KEY_DOWN()) {
            final var key = SDL_Event.key(sdlEvent);
            int scancode = SDL_KeyboardEvent.key(key);
            return scancode == SDLK_ESCAPE();
        }
        return false;
    }
}
