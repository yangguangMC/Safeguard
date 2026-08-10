package top.yangguangmc.safeguard.protection;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 全局保护前置条件管理器。
 * <p>
 * 维护一组 {@link Predicate}{@code <ClientPlayerEntity>}，
 * 只有当<b>所有条件均满足</b>时，检测项才被允许工作。
 * 这些条件是"运行时"的——它们随玩家状态（游戏模式、药水效果等）动态变化，
 * 与用户可配置的持久化开关（{@link SwitchTreeNode}）是正交的。
 * </p>
 * <p>
 * 条件在静态初始化块中注册，类加载即生效，无需手动调用任何初始化方法。
 * 可通过 {@link #addCondition(Predicate)} 在任意时机追加新条件。
 * </p>
 */
public final class GlobalProtectionConditions {

    private static final List<Predicate<LocalPlayer>> CONDITIONS = new ArrayList<>();

    static {
        // 创造/旁观模式 → 不可能受伤，不需要保护
        addCondition(player -> !player.isCreative() && !player.isSpectator());
        // invulnerable 无敌状态 → 不需要保护
        addCondition(player -> !player.isInvulnerable());
        // 抗性提升 255 (amplifier >= 254) → 高版本玩法数据包常用，完全免疫伤害，不需要保护
        addCondition(player -> {
            MobEffectInstance effect = player.getEffect(MobEffects.RESISTANCE);
            return effect == null || effect.getAmplifier() < 254;
        });
    }

    private GlobalProtectionConditions() {
        throw new AssertionError();
    }

    /**
     * 追加一个全局保护前置条件。
     * 所有已注册的条件会以 AND 逻辑进行判断。
     *
     * @param condition 返回 {@code true} 表示该条件允许保护继续进行
     */
    public static void addCondition(Predicate<LocalPlayer> condition) {
        CONDITIONS.add(condition);
    }

    /**
     * 判断当前玩家是否处于"需要保护"的状态。
     * 遍历所有注册的条件，全部返回 {@code true} 才认为需要保护。
     *
     * @param player 客户端玩家实例
     * @return {@code true} 当所有全局条件均满足
     */
    public static boolean shouldProtect(LocalPlayer player) {
        for (Predicate<LocalPlayer> condition : CONDITIONS) {
            if (!condition.test(player)) return false;
        }
        return true;
    }
}