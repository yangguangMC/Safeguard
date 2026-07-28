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
        double relativeYaw = getRelativeYaw(camera.getCameraPos(), camera.getCameraYaw(), target, e -> client.getRenderTickCounter().getTickProgress(!world.getTickManager().shouldSkipTick(e)));
        String directionIndicator;
        if (relativeYaw < -180 || relativeYaw > 180) throw new AssertionError();
        if (relativeYaw >= -157.5 && relativeYaw < -112.5) directionIndicator = Text.translatable("gui.safeguard.direction.sw").getString();
        else if (relativeYaw >= -112.5 && relativeYaw < -67.5) directionIndicator = Text.translatable("gui.safeguard.direction.w").getString();
        else if (relativeYaw >= -67.5 && relativeYaw < -22.5) directionIndicator = Text.translatable("gui.safeguard.direction.nw").getString();
        else if (relativeYaw >= -22.5 && relativeYaw < 22.5) directionIndicator = Text.translatable("gui.safeguard.direction.n").getString();
        else if (relativeYaw >= 22.5 && relativeYaw < 67.5) directionIndicator = Text.translatable("gui.safeguard.direction.ne").getString();
        else if (relativeYaw >= 67.5 && relativeYaw < 112.5) directionIndicator = Text.translatable("gui.safeguard.direction.e").getString();
        else if (relativeYaw >= 112.5 && relativeYaw < 157.5) directionIndicator = Text.translatable("gui.safeguard.direction.se").getString();
        else directionIndicator = Text.translatable("gui.safeguard.direction.s").getString();  // relativeYaw <= -157.5 || relativeYaw >= 157.5
        return directionIndicator;
    }

    private static double getRelativeYaw(Vec3d cameraPos, float yaw, Entity entity, EntityTickProgress tickProgress) {
        Vec3d vec3d = cameraPos.subtract(entity.getCameraPosVec(tickProgress.getTickProgress(entity))).rotateYClockwise();
        float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float) Math.PI);
        return MathHelper.subtractAngles(yaw, f);
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
