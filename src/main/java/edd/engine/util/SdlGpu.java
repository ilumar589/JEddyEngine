package edd.engine.util;

import org.libsdl3.SDL3;
import org.libsdl3.SDL_FColor;
import org.libsdl3.SDL_GPUColorTargetInfo;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static edd.engine.util.FFMUtil.*;
import static org.libsdl3.SDL3_2.SDL_GPU_LOADOP_CLEAR;
import static org.libsdl3.SDL3_2.SDL_GPU_STOREOP_STORE;

public final class SdlGpu {
    private SdlGpu() {}

    public record SwapchainTex(MemorySegment texture, int width, int height) {}

    /** Acquire the swapchain texture (returns null if minimized). Cancels the cmd on failure/minimize. */
    public static SwapchainTex acquireSwapchainTexture(MemorySegment cmd, MemorySegment window, Arena a) {
        var texOut = outPtr(a);   // SDL_GPUTexture**
        var wOut   = outU32(a);   // Uint32*
        var hOut   = outU32(a);   // Uint32*

        boolean ok = SDL3.SDL_WaitAndAcquireGPUSwapchainTexture(cmd, window, texOut, wOut, hOut);
        if (!ok) {
            SDL3.SDL_CancelGPUCommandBuffer(cmd);
            throw new IllegalStateException("SDL_WaitAndAcquireGPUSwapchainTexture failed");
        }

        var tex = readPtr(texOut);
        if (tex.equals(MemorySegment.NULL)) {
            SDL3.SDL_CancelGPUCommandBuffer(cmd); // minimized/not ready → skip frame
            return null;
        }
        return new SwapchainTex(tex, readU32(wOut), readU32(hOut));
    }

    /** One color target with clear+store and a given clear color. */
    public static MemorySegment makeColorTarget(Arena a, MemorySegment texture, float r, float g, float b, float a_) {
        requireNonNullAddr(texture, "swapchain texture");

        var info = FFMUtil.calloc(a, SDL_GPUColorTargetInfo.layout());
        SDL_GPUColorTargetInfo.texture(info, texture);
        SDL_GPUColorTargetInfo.load_op(info, SDL_GPU_LOADOP_CLEAR());
        SDL_GPUColorTargetInfo.store_op(info, SDL_GPU_STOREOP_STORE());

        var clr = FFMUtil.calloc(a, SDL_FColor.layout());
        SDL_FColor.r(clr, r); SDL_FColor.g(clr, g); SDL_FColor.b(clr, b); SDL_FColor.a(clr, a_);
        SDL_GPUColorTargetInfo.clear_color(info, clr);

        return info; // pass as colorTargets ptr with count=1
    }
}
