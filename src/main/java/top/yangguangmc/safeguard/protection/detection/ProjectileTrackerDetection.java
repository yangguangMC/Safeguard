package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

public class ProjectileTrackerDetection extends Detection {
    public ProjectileTrackerDetection() {
        super("combat/projectile_tracker", new ActionBarTitleAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        Projectile projectile = null;
        float relativeAngle = Float.POSITIVE_INFINITY;
        for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof Projectile proj) {
                if (player.equals(proj.getOwner())) continue;
                Vec3 arrowPos = proj.position();
                Vec3 velocity = proj.getDeltaMovement();
                if (velocity.lengthSqr() < 0.0001) continue;
                if (velocity.lengthSqr() < 1.5 && velocity.horizontalDistanceSqr() < 0.05) continue;
                if (proj instanceof FireworkRocketEntity fireworkRocket && !fireworkRocket.isShotAtAngle()) continue;
                if (player.distanceToSqr(arrowPos.add(velocity)) >= player.distanceToSqr(arrowPos)) continue;
                if (proj.getOwner() != null
                        && proj.getOwner().distanceToSqr(player) < 144
                        && Math.abs(Utils.computeRelativeYaw(
                        client.gameRenderer.getMainCamera().getPosition(),
                        client.gameRenderer.getMainCamera().getYRot(),
                        proj.position()
                )) < 15)
                    continue;
                Vec3 playerPos = player.position();
                float yaw1 = (float) (Mth.atan2(velocity.z(), velocity.x()) * (180.0F / (float) Math.PI));
                Vec3 vec3d = playerPos.subtract(arrowPos);
                float yaw2 = (float) Mth.atan2(vec3d.z(), vec3d.x()) * (180.0F / (float) Math.PI);
                float relativeAngle1 = Math.abs(Mth.degreesDifference(yaw1, yaw2));
                if (relativeAngle1 < relativeAngle) {
                    projectile = proj;
                    relativeAngle = relativeAngle1;
                    break;
                }
            }
        }
        if (projectile != null && relativeAngle < 10) {
            Entity source = projectile.getOwner();
            Component message;
            if (source == null)
                message = Component.translatable("detection.safeguard.combat.projectile_tracker.warning_unknown",
                        projectile.getDisplayName(),
                        Utils.getDirectionIndicator(client, world, projectile, client.gameRenderer.getMainCamera()));
            else
                message = Component.translatable("detection.safeguard.combat.projectile_tracker.warning_known",
                        source.getDisplayName(),
                        projectile.getDisplayName(),
                        Utils.getDirectionIndicator(client, world, source, client.gameRenderer.getMainCamera()));
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.HIGH, message, client));
        }
    }
}
