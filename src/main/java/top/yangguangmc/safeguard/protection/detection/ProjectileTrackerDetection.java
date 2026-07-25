package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import top.yangguangmc.safeguard.protection.action.Action;
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
                Vec3d arrowPos = proj.getEntityPos();
                Vec3d velocity = proj.getVelocity();
                if (velocity.lengthSquared() < 0.0001) continue;
                if (player.squaredDistanceTo(arrowPos.add(velocity)) >= player.squaredDistanceTo(arrowPos)) continue;
                Vec3d playerPos = player.getEntityPos();
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
            ProjectileEntity target = projectile;
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(client, world, target, target.getOwner()));
        }
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientWorld world, ProjectileEntity projectile, @Nullable Entity projectileSource) {
            if (projectileSource == null)
                client.inGameHud.setOverlayMessage(Text.literal("警告：").append(projectile.getDisplayName()).append(" 靠近 ").append(Utils.getDirectionIndicator(client, world, projectile, client.gameRenderer.getCamera())).styled(style -> style.withColor(Formatting.RED)), false);
            else
                client.inGameHud.setOverlayMessage(Text.literal("警告：").append(projectileSource.getDisplayName()).append(" 向你发射 ").append(projectile.getDisplayName()).append(" ").append(Utils.getDirectionIndicator(client, world, projectileSource, client.gameRenderer.getCamera())).styled(style -> style.withColor(Formatting.RED)), false);
        }
    }
}