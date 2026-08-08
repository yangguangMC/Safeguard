package top.yangguangmc.safeguard.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

    /**
     * 1.20.6: Uses client.getTickDelta() instead of EntityTickProgress/RenderTickCounter chain.
     */
    public static String getDirectionIndicator(MinecraftClient client, ClientWorld world, Entity target, Camera camera) {
        // 1.20.6: getCameraPosVec uses simple tickDelta
        Vec3d targetPos = target.getCameraPosVec(client.getTickDelta());
        return getDirectionIndicator(camera, targetPos);
    }

    public static String getDirectionIndicator(Camera camera, Vec3d targetPos) {
        Vec3d cameraPos = camera.getPos();  // 1.20.6: getPos() not getCameraPos()
        double relativeYaw = computeRelativeYaw(cameraPos, camera.getYaw(), targetPos); // 1.20.6: getYaw() not getCameraYaw()
        String horizontal = directionFromRelativeYaw(relativeYaw);

        double dx = targetPos.getX() - cameraPos.getX();
        double dy = targetPos.getY() - cameraPos.getY();
        double dz = targetPos.getZ() - cameraPos.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1e-6) return horizontal;

        double pitchToTarget = -Math.toDegrees(Math.atan2(dy, horizontalDist));
        double relativePitch = pitchToTarget - camera.getPitch();
        if (relativePitch > 30.0)
            return horizontal + " " + Text.translatable("gui.safeguard.direction.below").getString();
        if (relativePitch < -30.0)
            return horizontal + " " + Text.translatable("gui.safeguard.direction.above").getString();
        return horizontal;
    }

    public static double computeRelativeYaw(Vec3d cameraPos, float yaw, Vec3d targetPos) {
        // 1.20.6: rotateYClockwise()
        Vec3d vec3d = rotateYClockwise(cameraPos.subtract(targetPos));
        float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float) Math.PI);
        return MathHelper.subtractAngles(yaw, f);
    }

    private static Vec3d rotateYClockwise(Vec3d vec3d) {
        return new Vec3d(-vec3d.getZ(), vec3d.getY(), vec3d.getX());
    }

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
        return Text.translatable("gui.safeguard.direction.s").getString();
    }

    public static boolean isBehindPlayer(MinecraftClient client, ClientWorld world, Entity entity, Camera camera) {
        // 1.20.6: simplified - use client.getTickDelta() directly
        double relativeYaw = computeRelativeYaw(camera.getPos(), camera.getYaw(),
                entity.getCameraPosVec(client.getTickDelta()));
        return Math.abs(relativeYaw) > 90.0;
    }

    public static boolean isApproaching(LivingEntity entity, ClientPlayerEntity player) {
        Vec3d velocity = entity.getVelocity();
        if (velocity.lengthSquared() == 0) return false;
        Vec3d futurePos = entity.getPos().add(velocity);  // 1.20.6: getPos() not getEntityPos()
        return futurePos.squaredDistanceTo(player.getPos()) < entity.squaredDistanceTo(player);
    }

    public static void simulatePress(KeyBinding keyBinding) {
        InputUtil.Key key = ((KeyBindingAccessor) keyBinding).safeguard$getBoundKey();
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
        SIMULATE_RELEASE_TICKS.put(keyBinding, 8);
    }

    /**
     * 1.20.6: Uses Item.isSuitableFor() instead of DataComponentTypes.TOOL
     */
    public static boolean hasDestroyIntention(MinecraftClient client, ClientWorld world, ClientPlayerEntity player, Predicate<BlockPos> predicate) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return false;
        if (client.options.useKey.isPressed()) return false;
        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        if (!predicate.test(pos)) return false;
        if (client.options.attackKey.isPressed()) return true;
        // 1.20.6: getActiveOrMainHandStack()
        ItemStack item = player.isUsingItem() ? player.getActiveItem() : player.getMainHandStack();
        // 1.20.6: Use Item.isSuitableFor() instead of DataComponentTypes.TOOL
        return item.isSuitableFor(world.getBlockState(pos));
    }

    public static Text getInventoryPosIndicator(int slot) {
        if (slot < 0)
            throw new IndexOutOfBoundsException("%d out of [0-%d]".formatted(slot, PlayerInventory.OFF_HAND_SLOT));
        // 1.20.6: PlayerInventory.HOTBAR_SIZE is private
        if (slot < 9) return Text.translatable("gui.safeguard.slot.hotbar", slot + 1);
        else if (slot < PlayerInventory.MAIN_SIZE) return Text.translatable("gui.safeguard.slot.inventory");
        else if (slot == PlayerInventory.OFF_HAND_SLOT) return Text.translatable("gui.safeguard.slot.offhand");
        else if (slot == PlayerInventory.MAIN_SIZE) return Text.translatable("gui.safeguard.slot.armor.head");
        else if (slot == PlayerInventory.MAIN_SIZE + 1) return Text.translatable("gui.safeguard.slot.armor.chest");
        else if (slot == PlayerInventory.MAIN_SIZE + 2) return Text.translatable("gui.safeguard.slot.armor.legs");
        else if (slot == PlayerInventory.MAIN_SIZE + 3) return Text.translatable("gui.safeguard.slot.armor.feet");
        else throw new IndexOutOfBoundsException("%d out of [0-%d]".formatted(slot, PlayerInventory.OFF_HAND_SLOT));
    }
}
