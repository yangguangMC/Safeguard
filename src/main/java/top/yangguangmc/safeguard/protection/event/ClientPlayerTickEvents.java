package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public final class ClientPlayerTickEvents {
    private ClientPlayerTickEvents() {
    }

    /**
     * Called at the start of the client player tick.
     */
    public static final Event<StartTick> START_TICK = EventFactory.createArrayBacked(StartTick.class, callbacks -> (client, world, player) -> {
        for (StartTick event : callbacks) {
            event.onStartTick(client, world, player);
        }
    });

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player);
    }
}
