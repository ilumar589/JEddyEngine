package edd.engine;

import org.libsdl3.SDL3;
import org.libsdl3.SDL_Event;
import org.libsdl3.SDL_KeyboardEvent;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;


public class Main {
    static void main() {
        final var sdlInitOk = SDL3.SDL_Init(SDL3.SDL_INIT_VIDEO());
        assert sdlInitOk;

        try (final var arena = Arena.ofConfined()) {
            final var windowTitle = arena.allocateFrom("Hello SDL3");

            final var window = SDL3.SDL_CreateWindow(windowTitle, 1280, 780, 0);

            assert window != MemorySegment.NULL;

            final var sdlEvent = SDL_Event.allocate(arena);

            boolean running = true;
            while (running) {
                while (SDL3.SDL_PollEvent(sdlEvent)) {
                    int type = SDL_Event.type(sdlEvent);

                    // QUIT
                    int quit = SDL3.SDL_EVENT_QUIT();
                    if (type == quit) {
                        running = false;
                        break;
                    }

                    // SOME KEY DOWN
                    int keyDown = SDL3.SDL_EVENT_KEY_DOWN();
                    if (type == keyDown) {
                        final var key = SDL_Event.key(sdlEvent);
                        int scancode = SDL_KeyboardEvent.key(key);

                        IO.println("Key pressed: " + scancode);
                        break;
                    }

                    // window closed
                    int windowsClosedRequested = SDL3.SDL_EVENT_WINDOW_CLOSE_REQUESTED();
                    if (type == windowsClosedRequested) {
                        running = false;
                        break;
                    }

                    // default
//                    IO.println("Unknown event type: " + type);
                    break;
                }
            }

            SDL3.SDL_DestroyWindow(window);
            SDL3.SDL_Quit();
        }


    }
}
