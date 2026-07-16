package top.yangguangmc.safeguard.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.EntityTickProgress;
import top.yangguangmc.safeguard.injection.mixin.KeyBindingAccessor;

import java.util.HashMap;
import java.util.Map;

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
        if (relativeYaw >= -157.5 && relativeYaw < -112.5) directionIndicator = "↙";
        else if (relativeYaw >= -112.5 && relativeYaw < -67.5) directionIndicator = "←";
        else if (relativeYaw >= -67.5 && relativeYaw < -22.5) directionIndicator = "↖";
        else if (relativeYaw >= -22.5 && relativeYaw < 22.5) directionIndicator = "↑";
        else if (relativeYaw >= 22.5 && relativeYaw < 67.5) directionIndicator = "↗";
        else if (relativeYaw >= 67.5 && relativeYaw < 112.5) directionIndicator = "→";
        else if (relativeYaw >= 112.5 && relativeYaw < 157.5) directionIndicator = "↘";
        else directionIndicator = "↓";  // relativeYaw <= -157.5 || relativeYaw >= 157.5
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
}
