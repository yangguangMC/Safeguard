package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import top.yangguangmc.safeguard.protection.GlobalProtectionConditions;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;

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
     * Gated version of {@link #START_TICK} that supports suspending/resuming listeners through owner.
     * It's recommended for subclasses of {@link top.yangguangmc.safeguard.protection.detection.Detection}
     * to register listeners through this constant.
     *
     * @see GatedEvent
     */
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
        void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player);
    }
}
