package top.yangguangmc.safeguard.protection;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class GlobalProtectionConditions {

    private static final List<Predicate<ClientPlayerEntity>> CONDITIONS = new ArrayList<>();

    static {
        addCondition(player -> !player.isCreative() && !player.isSpectator());
        addCondition(player -> !player.isInvulnerable());
        addCondition(player -> {
            StatusEffectInstance effect = player.getStatusEffect(StatusEffects.RESISTANCE);
            return effect == null || effect.getAmplifier() < 254;
        });
    }

    private GlobalProtectionConditions() {
        throw new AssertionError();
    }

    public static void addCondition(Predicate<ClientPlayerEntity> condition) {
        CONDITIONS.add(condition);
    }

    public static boolean shouldProtect(ClientPlayerEntity player) {
        for (Predicate<ClientPlayerEntity> condition : CONDITIONS) {
            if (!condition.test(player)) return false;
        }
        return true;
    }
}
