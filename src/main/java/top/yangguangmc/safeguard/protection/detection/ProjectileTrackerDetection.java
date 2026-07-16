package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

public class ProjectileTrackerDetection extends Detection {
    public ProjectileTrackerDetection() {
        super("combat/arrow_tracker", new ActionBarTitleAction());
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
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
            ActionBarTitleAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title"));
            action.updateTitle(client, world, projectile, projectile.getOwner());
        }
    }


    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientWorld world, ProjectileEntity projectile, @Nullable Entity projectileSource) {
            if (projectileSource == null)
                client.inGameHud.setOverlayMessage(Text.literal("警告：").append(projectile.getDisplayName()).append(" 弹射物靠近 ").append(Utils.getDirectionIndicator(client, world, projectile, client.gameRenderer.getCamera())).styled(style -> style.withColor(Formatting.RED)), false);
            else
                client.inGameHud.setOverlayMessage(Text.literal("警告：").append(projectileSource.getDisplayName()).append(" 向你发射弹射物 ").append(Utils.getDirectionIndicator(client, world, projectileSource, client.gameRenderer.getCamera())).styled(style -> style.withColor(Formatting.RED)), false);
        }
    }
}
