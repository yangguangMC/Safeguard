package top.yangguangmc.safeguard.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.EntityTickProgress;
import top.yangguangmc.safeguard.injection.mixin.KeyBindingAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class Utils {
    private static final Map<KeyBinding, Integer> SIMULATE_RELEASE_TICKS = new HashMap<>();

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (KeyBinding keyBinding : SIMULATE_RELEASE_TICKS.keySet()) {
                int ticks = SIMULATE_RELEASE_TICKS.get(keyBinding) - 1;
                if (ticks > 0) SIMULATE_RELEASE_TICKS.put(keyBinding, ticks);
                else {
                    keyBinding.setPressed(false);
                    SIMULATE_RELEASE_TICKS.remove(keyBinding);
                }
            }
        });
    }

    private Utils() {
        throw new AssertionError();
    }

    public static String getDirectionIndicator(MinecraftClient client, ClientWorld world, Entity target, Camera camera) {
        EntityTickProgress tickProgress = e -> client.getRenderTickCounter()
                .getTickProgress(!world.getTickManager().shouldSkipTick(e));
        Vec3d targetPos = target.getCameraPosVec(tickProgress.getTickProgress(target));
        return getDirectionIndicator(camera, targetPos);
    }

    /**
     * 根据相机状态与目标世界坐标返回方位指示字符串（含水平+垂直方向）。
     *
     * @param camera    玩家相机
     * @param targetPos 目标世界坐标
     * @return 方向指示文本，如 "N"、"NE↑"、"SW↓"
     */
    public static String getDirectionIndicator(Camera camera, Vec3d targetPos) {
        Vec3d cameraPos = camera.getCameraPos();
        double relativeYaw = computeRelativeYaw(cameraPos, camera.getCameraYaw(), targetPos);
        String horizontal = directionFromRelativeYaw(relativeYaw);

        double dx = targetPos.getX() - cameraPos.getX();
        double dy = targetPos.getY() - cameraPos.getY();
        double dz = targetPos.getZ() - cameraPos.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1e-6) return horizontal;

        double pitchToTarget = -Math.toDegrees(Math.atan2(dy, horizontalDist));
        double relativePitch = pitchToTarget - camera.getPitch();
        if (relativePitch > 30.0)
            return horizontal + Text.translatable("gui.safeguard.direction.below").getString();
        if (relativePitch < -30.0)
            return horizontal + Text.translatable("gui.safeguard.direction.above").getString();
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
    public static double computeRelativeYaw(Vec3d cameraPos, float yaw, Vec3d targetPos) {
        Vec3d vec3d = cameraPos.subtract(targetPos).rotateYClockwise();
        float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float) Math.PI);
        return MathHelper.subtractAngles(yaw, f);
    }

    /**
     * 将相对偏航角映射为水平方向指示字符串。
     *
     * @param relativeYaw 相对偏航角
     * @return N/NE/E/SE/S/SW/W/NW 之一
     */
    public static String directionFromRelativeYaw(double relativeYaw) {
        if (relativeYaw < -180 || relativeYaw > 180) throw new AssertionError();
        if (relativeYaw >= -157.5 && relativeYaw < -112.5)
            return Text.translatable("gui.safeguard.direction.sw").getString();
        if (relativeYaw >= -112.5 && relativeYaw < -67.5)
            return Text.translatable("gui.safeguard.direction.w").getString();
        if (relativeYaw >= -67.5 && relativeYaw < -22.5)
            return Text.translatable("gui.safeguard.direction.nw").getString();
        if (relativeYaw >= -22.5 && relativeYaw < 22.5)
            return Text.translatable("gui.safeguard.direction.n").getString();
        if (relativeYaw >= 22.5 && relativeYaw < 67.5)
            return Text.translatable("gui.safeguard.direction.ne").getString();
        if (relativeYaw >= 67.5 && relativeYaw < 112.5)
            return Text.translatable("gui.safeguard.direction.e").getString();
        if (relativeYaw >= 112.5 && relativeYaw < 157.5)
            return Text.translatable("gui.safeguard.direction.se").getString();
        return Text.translatable("gui.safeguard.direction.s").getString();  // relativeYaw <= -157.5 || relativeYaw >= 157.5
    }

    public static double getRelativeYaw(Vec3d cameraPos, float yaw, Entity entity, EntityTickProgress tickProgress) {
        return computeRelativeYaw(cameraPos, yaw, entity.getCameraPosVec(tickProgress.getTickProgress(entity)));
    }

    /**
     * 判断目标实体是否在玩家背后（相对偏航角绝对值 > 90°）。
     */
    public static boolean isBehindPlayer(MinecraftClient client, ClientWorld world, Entity entity, Camera camera) {
        EntityTickProgress tickProgress = e -> client.getRenderTickCounter()
                .getTickProgress(!world.getTickManager().shouldSkipTick(e));
        double relativeYaw = computeRelativeYaw(camera.getCameraPos(), camera.getCameraYaw(),
                entity.getCameraPosVec(tickProgress.getTickProgress(entity)));
        return Math.abs(relativeYaw) > 90.0;
    }

    /**
     * 判断目标实体是否正在朝玩家靠近。
     * 即移动速度不为 0，且加上速度后的末位置比初位置更靠近玩家。
     */
    public static boolean isApproaching(LivingEntity entity, ClientPlayerEntity player) {
        Vec3d velocity = entity.getVelocity();
        if (velocity.lengthSquared() == 0) return false;
        Vec3d futurePos = entity.getEntityPos().add(velocity);
        return futurePos.squaredDistanceTo(player.getEntityPos()) < entity.squaredDistanceTo(player);
    }

    public static void simulatePress(KeyBinding keyBinding) {
        InputUtil.Key key = ((KeyBindingAccessor) keyBinding).safeguard$getBoundKey();
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
        SIMULATE_RELEASE_TICKS.put(keyBinding, 8);
    }

    public static boolean hasDestroyIntention(MinecraftClient client, ClientWorld world, ClientPlayerEntity player, Predicate<BlockPos> predicate) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return false;
        if (client.options.useKey.isPressed()) return false;
        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        if (!predicate.test(pos)) return false;
        if (client.options.attackKey.isPressed()) return true;
        ItemStack item = player.getActiveOrMainHandStack();
        ToolComponent component = item.get(DataComponentTypes.TOOL);
        return component != null && component.isCorrectForDrops(world.getBlockState(pos));
    }
}
