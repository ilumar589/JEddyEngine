package edd.engine.util;

import org.libsdl3.SDL3;
import org.libsdl3.SDL3_2;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class SdlLogUtil {
    private SdlLogUtil() {}

    public static final int CAT = SDL3.SDL_LOG_CATEGORY_APPLICATION();

    /* --------- helpers ---------- */

    private static MemorySegment cstr(Arena a, String s) {
        return a.allocateFrom(s);
    }

    public static String lastSdlError() {
        try {
            MemorySegment err = SDL3.SDL_GetError();
            if (err != null && !err.equals(MemorySegment.NULL)) return readUtf8CString(err);
        } catch (Throwable ignored) {}
        return "";
    }

    /* --------- plain message (preformatted in Java) ---------- */

    public static void raw(Arena a, String msg) {
        // SDL_Log(const char* fmt, ...) — we pass a preformatted string and NO varargs.
        SDL3.SDL_Log.makeInvoker().apply(cstr(a, msg));
    }

    public static void info(Arena a, String msg)   { SDL3.SDL_LogInfo.makeInvoker().apply(CAT, cstr(a, msg)); }
    public static void warn(Arena a, String msg)   { SDL3.SDL_LogWarn.makeInvoker().apply(CAT, cstr(a, msg)); }
    public static void error(Arena a, String msg)  { SDL3.SDL_LogError.makeInvoker().apply(CAT, cstr(a, msg)); }
    public static void debug(Arena a, String msg)  { SDL3.SDL_LogDebug.makeInvoker().apply(CAT, cstr(a, msg)); }
    public static void trace(Arena a, String msg)  { SDL3.SDL_LogTrace.makeInvoker().apply(CAT, cstr(a, msg)); }

    /* --------- with Java-side formatting ---------- */

    public static void infof(Arena a, String fmt, Object... args)  { info(a, String.format(Locale.ROOT, fmt, args)); }
    public static void warnf(Arena a, String fmt, Object... args)  { warn(a, String.format(Locale.ROOT, fmt, args)); }
    public static void errorf(Arena a, String fmt, Object... args) { error(a, String.format(Locale.ROOT, fmt, args)); }
    public static void debugf(Arena a, String fmt, Object... args) { debug(a, String.format(Locale.ROOT, fmt, args)); }
    public static void tracef(Arena a, String fmt, Object... args) { trace(a, String.format(Locale.ROOT, fmt, args)); }

    /* --------- convenience for reporting SDL_GetError() ---------- */

    public static void errorWithLast(Arena a, String context) {
        String e = lastSdlError();
        if (!e.isEmpty()) error(a, context + " | SDL_Error: " + e);
        else error(a, context);
    }

    /* --------- category-aware overloads (if you use multiple categories) ---------- */

    public static void info(Arena a, int category, String msg)  { SDL3.SDL_LogInfo.makeInvoker().apply(category, cstr(a, msg)); }
    public static void warn(Arena a, int category, String msg)  { SDL3.SDL_LogWarn.makeInvoker().apply(category, cstr(a, msg)); }
    public static void error(Arena a, int category, String msg) { SDL3.SDL_LogError.makeInvoker().apply(category, cstr(a, msg)); }

    public static void infof(Arena a, int category, String fmt, Object... args)  { SDL3.SDL_LogInfo
            .makeInvoker()
            .apply(category, cstr(a, String.format(Locale.ROOT, fmt, args))); }
    public static void warnf(Arena a, int category, String fmt, Object... args)  { SDL3.SDL_LogWarn
            .makeInvoker()
            .apply(category, cstr(a, String.format(Locale.ROOT, fmt, args))); }
    public static void debugf(Arena a, int category, String fmt, Object... args)  { SDL3_2.SDL_LogDebug
            .makeInvoker()
            .apply(category, cstr(a, String.format(Locale.ROOT, fmt, args))); }
    public static void errorf(Arena a, int category, String fmt, Object... args) { SDL3.SDL_LogError
            .makeInvoker()
            .apply(category, cstr(a, String.format(Locale.ROOT, fmt, args))); }

    /** Read a NUL-terminated UTF-8 C string from native memory. */
    public static String readUtf8CString(MemorySegment addr) {
        if (addr == null || addr.equals(MemorySegment.NULL)) return "";
        long len = 0;
        while (addr.get(ValueLayout.JAVA_BYTE, len) != 0) len++;
        byte[] bytes = new byte[(int) len];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = addr.get(ValueLayout.JAVA_BYTE, i);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
