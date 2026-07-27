package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class ClientPlayerTickEvents {
    private ClientPlayerTickEvents() {
        throw new AssertionError();
    }

    /**
     * Called at the start of the client player tick.
     */
    public static final Event<StartTick> START_TICK = EventFactory.createArrayBacked(StartTick.class, callbacks -> (client, world, player) -> {
        for (StartTick event : callbacks) event.onStartTick(client, world, player);
    });


    /**
     * {@link #START_TICK} 的门控版本，支持按所有者挂起/恢复。
     * 推荐 Detection 子类通过此常量注册监听器。
     *
     * @see GatedEvent
     */
    public static final GatedEvent<StartTick> GATED_START_TICK = new GatedEvent<>(
            START_TICK,
            active -> (client, world, player) -> {
                for (StartTick cb : active.get()) cb.onStartTick(client, world, player);
            }
    );


    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player);
    }
}
