package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class GameRendererCloseEvent {
    public static final Event<GameRendererCloseListener> CALLBACK = EventFactory.createArrayBacked(
            GameRendererCloseListener.class,
            (listeners) -> () -> {
                for (GameRendererCloseListener listener : listeners) listener.onClose();
            }
    );

    private GameRendererCloseEvent() {
        throw new AssertionError();
    }

    @FunctionalInterface
    public interface GameRendererCloseListener {
        void onClose();
    }
}
