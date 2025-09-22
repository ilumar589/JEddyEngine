package edd.engine;

import org.libsdl3.SDL3;
import org.libsdl3.SDL_Event;
import org.libsdl3.SDL_KeyboardEvent;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.libsdl3.SDL3_1.SDLK_ESCAPE;


public class Main {
    static void main() {
        final var sdlInitOk = SDL3.SDL_Init(SDL3.SDL_INIT_VIDEO());
        assert sdlInitOk;

        try (final var arena = Arena.ofConfined()) {
            final var windowTitle = arena.allocateFrom("Hello SDL3");

            final var window = SDL3.SDL_CreateWindow(windowTitle, 1280, 780, 0);

            assert window != MemorySegment.NULL;


            boolean running = true;
            while (running) {
                // poll events
                running = pollEvents(arena);
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
            int quit = SDL3.SDL_EVENT_QUIT();
            if (type == quit) {
                return false;
            }

            if (isEscapePressed(type, sdlEvent)) {
                return false;
            }

            // window closed
            int windowsClosedRequested = SDL3.SDL_EVENT_WINDOW_CLOSE_REQUESTED();
            if (type == windowsClosedRequested) {
                return false;
            }

            return true;
        }

        return true;
    }

    private static boolean isEscapePressed(int sdlEventType, MemorySegment sdlEvent) {
        final int keyDownType = SDL3.SDL_EVENT_KEY_DOWN();
        if (sdlEventType == keyDownType) {
            final var key = SDL_Event.key(sdlEvent);
            int scancode = SDL_KeyboardEvent.key(key);
            return scancode == SDLK_ESCAPE();
        }
        return false;
    }
}
