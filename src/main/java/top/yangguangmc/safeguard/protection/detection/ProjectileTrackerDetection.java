package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

public class ProjectileTrackerDetection extends Detection {
    public ProjectileTrackerDetection() {
        super("combat/projectile_tracker", new ActionBarTitleAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        ProjectileEntity projectile = null;
        float relativeAngle = Float.POSITIVE_INFINITY;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ProjectileEntity proj) {
                if (player.equals(proj.getOwner())) continue;
                Vec3d arrowPos = proj.getPos();
                Vec3d velocity = proj.getVelocity();
                if (velocity.lengthSquared() < 0.0001) continue;
                if (velocity.lengthSquared() < 1.5 && velocity.horizontalLengthSquared() < 0.05) continue;
                if (proj instanceof FireworkRocketEntity fireworkRocket && !fireworkRocket.wasShotAtAngle()) continue;
                if (player.squaredDistanceTo(arrowPos.add(velocity)) >= player.squaredDistanceTo(arrowPos)) continue;
                if (proj.getOwner() != null
                        && proj.getOwner().squaredDistanceTo(player) < 144
                        && Math.abs(Utils.computeRelativeYaw(
                        client.gameRenderer.getCamera().getPos(),
                        client.gameRenderer.getCamera().getYaw(),
                        proj.getPos()
                )) < 15)
                    continue;
                Vec3d playerPos = player.getPos();
                float yaw1 = (float) (MathHelper.atan2(velocity.getZ(), velocity.getX()) * (180.0F / (float) Math.PI));
                Vec3d vec3d = playerPos.subtract(arrowPos);
                float yaw2 = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float) Math.PI);
                float relativeAngle1 = Math.abs(MathHelper.subtractAngles(yaw1, yaw2));
                if (relativeAngle1 < relativeAngle) {
                    projectile = proj;
                    relativeAngle = relativeAngle1;
                    break;
                }
            }
        }
        if (projectile != null && relativeAngle < 10) {
            Entity source = projectile.getOwner();
            Text message;
            if (source == null)
                message = Text.translatable("detection.safeguard.combat.projectile_tracker.warning_unknown",
                        projectile.getDisplayName(),
                        Utils.getDirectionIndicator(client, world, projectile, client.gameRenderer.getCamera()));
            else
                message = Text.translatable("detection.safeguard.combat.projectile_tracker.warning_known",
                        source.getDisplayName(),
                        projectile.getDisplayName(),
                        Utils.getDirectionIndicator(client, world, source, client.gameRenderer.getCamera()));
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.HIGH, message, client));
        }
    }
}
