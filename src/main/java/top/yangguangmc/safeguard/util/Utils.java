package top.yangguangmc.safeguard.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import top.yangguangmc.safeguard.injection.mixin.KeyMappingAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class Utils {
    private static final Map<KeyMapping, Integer> SIMULATE_RELEASE_TICKS = new HashMap<>();

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (KeyMapping keyBinding : SIMULATE_RELEASE_TICKS.keySet()) {
                int ticks = SIMULATE_RELEASE_TICKS.get(keyBinding) - 1;
                if (ticks > 0) SIMULATE_RELEASE_TICKS.put(keyBinding, ticks);
                else {
                    keyBinding.setDown(false);
                    SIMULATE_RELEASE_TICKS.remove(keyBinding);
                }
            }
        });
    }

    private Utils() {
        throw new AssertionError();
    }

    /**
     * 获取实体相对于玩家的方位指示字符串（含水平+垂直方向），带帧间插值。
     *
     * @param client Minecraft 客户端实例
     * @param world  当前客户端世界
     * @param target 目标实体
     * @param camera 玩家相机
     * @return 方向指示文本，如 "↑"、"↓"、"↖ 上方"
     */
    public static String getDirectionIndicator(Minecraft client, ClientLevel world, Entity target, Camera camera) {
        // 1.20.6: getCameraPosVec uses simple tickDelta
        Vec3 targetPos = target.getEyePosition(client.getFrameTime());
        return getDirectionIndicator(camera, targetPos);
    }

    /**
     * 根据相机状态与目标世界坐标返回方位指示字符串（含水平+垂直方向）。
     *
     * @param camera    玩家相机
     * @param targetPos 目标世界坐标
     * @return 方向指示文本，如 "↑"、"↓"、"↖ 上方"
     */
    public static String getDirectionIndicator(Camera camera, Vec3 targetPos) {
        Vec3 cameraPos = camera.getPosition();  // 1.20.6: getPos() not getCameraPos()
        double relativeYaw = computeRelativeYaw(cameraPos, camera.getYRot(), targetPos); // 1.20.6: getYaw() not getCameraYaw()
        String horizontal = directionFromRelativeYaw(relativeYaw);

        double dx = targetPos.x() - cameraPos.x();
        double dy = targetPos.y() - cameraPos.y();
        double dz = targetPos.z() - cameraPos.z();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1e-6) return horizontal;

        double pitchToTarget = -Math.toDegrees(Math.atan2(dy, horizontalDist));
        double relativePitch = pitchToTarget - camera.getXRot();
        if (relativePitch > 30.0)
            return horizontal + " " + Component.translatable("gui.safeguard.direction.below").getString();
        if (relativePitch < -30.0)
            return horizontal + " " + Component.translatable("gui.safeguard.direction.above").getString();
        return horizontal;
    }

    /**
     * 核心公式：计算从相机看向目标位置的相对偏航角。
     *
     * @param cameraPos 相机世界坐标
     * @param yaw       相机偏航角（度）
     * @param targetPos 目标世界坐标
     * @return 相对偏航角，范围 [-180, 180]
     */
    public static double computeRelativeYaw(Vec3 cameraPos, float yaw, Vec3 targetPos) {
        // 1.20.6: rotateYClockwise()
        Vec3 vec3d = rotateYClockwise(cameraPos.subtract(targetPos));
        float f = (float) Mth.atan2(vec3d.z(), vec3d.x()) * (180.0F / (float) Math.PI);
        return Mth.degreesDifference(yaw, f);
    }

    private static Vec3 rotateYClockwise(Vec3 vec3d) {
        return new Vec3(-vec3d.z(), vec3d.y(), vec3d.x());
    }

    /**
     * 将相对偏航角映射为水平方向指示字符串。
     *
     * @param relativeYaw 相对偏航角
     * @return ↑/↗/→/↘/↓/↙/←/↖ 之一
     */
    public static String directionFromRelativeYaw(double relativeYaw) {
        if (relativeYaw < -180 || relativeYaw > 180) throw new AssertionError();
        if (relativeYaw >= -157.5 && relativeYaw < -112.5)
            return Component.translatable("gui.safeguard.direction.sw").getString();
        if (relativeYaw >= -112.5 && relativeYaw < -67.5)
            return Component.translatable("gui.safeguard.direction.w").getString();
        if (relativeYaw >= -67.5 && relativeYaw < -22.5)
            return Component.translatable("gui.safeguard.direction.nw").getString();
        if (relativeYaw >= -22.5 && relativeYaw < 22.5)
            return Component.translatable("gui.safeguard.direction.n").getString();
        if (relativeYaw >= 22.5 && relativeYaw < 67.5)
            return Component.translatable("gui.safeguard.direction.ne").getString();
        if (relativeYaw >= 67.5 && relativeYaw < 112.5)
            return Component.translatable("gui.safeguard.direction.e").getString();
        if (relativeYaw >= 112.5 && relativeYaw < 157.5)
            return Component.translatable("gui.safeguard.direction.se").getString();
        return Component.translatable("gui.safeguard.direction.s").getString();
    }

    /**
     * 计算相机看向实体的相对偏航角（带帧间插值），委托给 {@link #computeRelativeYaw}。
     *
     * @param cameraPos    相机世界坐标
     * @param yaw          相机偏航角（度）
     * @param entity       目标实体
     * @param tickProgress 帧间插值函数
     * @return 相对偏航角，范围 [-180, 180]
     */
    public static double getRelativeYaw(Vec3 cameraPos, float yaw, Entity entity, float tickProgress) {
        return computeRelativeYaw(cameraPos, yaw, entity.getEyePosition(tickProgress));
    }

    /**
     * 判断目标实体是否在玩家背后（相对偏航角绝对值 > 90°）。
     *
     * @param client Minecraft 客户端实例
     * @param world  当前客户端世界
     * @param entity 目标实体
     * @param camera 玩家相机
     * @return 在背后返回 {@code true}
     */
    public static boolean isBehindPlayer(Minecraft client, ClientLevel world, Entity entity, Camera camera) {
        // 1.20.6: simplified - use client.getTickDelta() directly
        double relativeYaw = computeRelativeYaw(camera.getPosition(), camera.getYRot(),
                entity.getEyePosition(client.getFrameTime()));
        return Math.abs(relativeYaw) > 90.0;
    }

    /**
     * 判断目标实体是否正在朝玩家靠近。
     * 即移动速度不为 0，且加上速度后的末位置比初位置更靠近玩家。
     *
     * @param entity 目标实体
     * @param player 玩家
     * @return 正在靠近返回 {@code true}
     */
    public static boolean isApproaching(LivingEntity entity, LocalPlayer player) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() == 0) return false;
        Vec3 futurePos = entity.position().add(velocity);  // 1.20.6: getPos() not getEntityPos()
        return futurePos.distanceToSqr(player.position()) < entity.distanceToSqr(player);
    }

    /**
     * 模拟按下一次按键（持续 8 tick 后自动松开）。
     * 使用 Accessor Mixin 读取绑定键码，避免直接依赖 KeyBinding 内部 API。
     *
     * @param keyBinding 要模拟的按键绑定
     */
    public static void simulatePress(KeyMapping keyBinding) {
        InputConstants.Key key = ((KeyMappingAccessor) keyBinding).safeguard$getKey();
        KeyMapping.set(key, true);
        KeyMapping.click(key);
        SIMULATE_RELEASE_TICKS.put(keyBinding, 8);
    }

    /**
     * 判断玩家当前是否有破坏/挖掘方块的意图。
     * 满足以下任一条件返回 {@code true}：
     * <ul>
     *   <li>玩家正在按攻击键且准星指向符合谓词的方块</li>
     *   <li>玩家手持正确工具且准星指向符合谓词的可挖掘方块</li>
     * </ul>
     *
     * @param client    Minecraft 客户端实例
     * @param world     当前客户端世界
     * @param player    玩家
     * @param predicate 对目标方块的额外条件（如判断是否为特定方块类型）
     * @return 有挖掘意图返回 {@code true}
     */
    public static boolean hasDestroyIntention(Minecraft client, ClientLevel world, LocalPlayer player, Predicate<BlockPos> predicate) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) return false;
        if (client.options.keyUse.isDown()) return false;
        BlockPos pos = ((BlockHitResult) client.hitResult).getBlockPos();
        if (!predicate.test(pos)) return false;
        if (client.options.keyAttack.isDown()) return true;
        // 1.20.6: getActiveOrMainHandStack()
        ItemStack item = player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        // 1.20.6: Use Item.isSuitableFor() instead of DataComponentTypes.TOOL
        return item.isCorrectToolForDrops(world.getBlockState(pos));
    }

    /**
     * 根据背包槽位索引返回该槽位所在区域的翻译文本指示。
     * 若槽位不在有效范围 [0, 40] 内，抛出 {@link IndexOutOfBoundsException}。
     *
     * @param slot 背包槽位索引
     * @return 对应区域的翻译文本
     * @throws IndexOutOfBoundsException 若 {@code slot} 不在 [0, 40] 内
     */
    public static Component getInventoryPosIndicator(int slot) {
        if (slot < 0)
            throw new IndexOutOfBoundsException("%d out of [0-%d]".formatted(slot, Inventory.SLOT_OFFHAND));
        // 1.20.6: PlayerInventory.HOTBAR_SIZE is private
        if (slot < 9) return Component.translatable("gui.safeguard.slot.hotbar", slot + 1);
        else if (slot < Inventory.INVENTORY_SIZE) return Component.translatable("gui.safeguard.slot.inventory");
        else if (slot == Inventory.SLOT_OFFHAND) return Component.translatable("gui.safeguard.slot.offhand");
        else if (slot == Inventory.INVENTORY_SIZE) return Component.translatable("gui.safeguard.slot.armor.head");
        else if (slot == Inventory.INVENTORY_SIZE + 1) return Component.translatable("gui.safeguard.slot.armor.chest");
        else if (slot == Inventory.INVENTORY_SIZE + 2) return Component.translatable("gui.safeguard.slot.armor.legs");
        else if (slot == Inventory.INVENTORY_SIZE + 3) return Component.translatable("gui.safeguard.slot.armor.feet");
        else throw new IndexOutOfBoundsException("%d out of [0-%d]".formatted(slot, Inventory.SLOT_OFFHAND));
    }
}
