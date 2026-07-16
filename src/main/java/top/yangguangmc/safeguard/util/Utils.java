package top.yangguangmc.safeguard.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.EntityTickProgress;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class Utils {
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
        // 反射是不是不如 Mixin？但 Access Widener 又相当复杂，委曲求全用反射。
        try {
            Field boundKeyField = Arrays.stream(KeyBinding.class.getDeclaredFields())
                    .filter(field -> field.getType() == InputUtil.Key.class)
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .filter(field -> !Modifier.isFinal(field.getModifiers()))
                    .findAny().orElseThrow();
            boundKeyField.setAccessible(true);
            InputUtil.Key key = (InputUtil.Key) boundKeyField.get(keyBinding);
            KeyBinding.setKeyPressed(key, true);
            KeyBinding.onKeyPressed(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
