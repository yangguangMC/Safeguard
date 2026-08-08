package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;

public class ClientPlayerTickEvents {
    private ClientPlayerTickEvents() {
        throw new AssertionError();
    }

    public static final Event<StartTick> START_TICK = EventFactory.createArrayBacked(StartTick.class, callbacks -> (client, world, player) -> {
        for (StartTick event : callbacks) event.onStartTick(client, world, player);
    });

    public static final GatedEvent<StartTick> GATED_START_TICK = new GatedEvent<>(
            START_TICK,
            active -> (client, world, player) -> {
                if (!GlobalProtectionConditions.shouldProtect(player)) return;
                ActionBarTitleAction.resetForTick();
                for (StartTick cb : active.get()) cb.onStartTick(client, world, player);
            }
    );

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player);
    }
}
