package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class EntityDamagedEvents {
    private EntityDamagedEvents() {
        throw new AssertionError();
    }

    /**
     * Called when any of the entities in ClientWorld (including ClientPlayerEntity) gets damage.
     */
    public static final Event<EntityDamagedListener> PRE = EventFactory.createArrayBacked(
            EntityDamagedListener.class,
            listeners -> (victim, source) -> {
                for (EntityDamagedListener listener : listeners) listener.onEntityDamaged(victim, source);
            }
    );

    /**
     * Gated version of {@link #PRE}.
     * Please note that this gated event <b>will not</b> check
     * {@link top.yangguangmc.safeguard.protection.GlobalProtectionConditions#shouldProtect(ClientPlayerEntity)}.
     */
    public static final GatedEvent<EntityDamagedListener> GATED_PRE = new GatedEvent<>(
            PRE,
            active -> (victim, source) -> {
                for (EntityDamagedListener listener : active.get()) listener.onEntityDamaged(victim, source);
            }
    );

    @FunctionalInterface
    public interface EntityDamagedListener {
        void onEntityDamaged(LivingEntity victim, DamageSource source);
    }
}
