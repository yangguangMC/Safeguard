package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.EntityTickProgress;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.PlaySoundAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

import java.util.function.UnaryOperator;

public class AntiCreeperDetection extends Detection {
    private final double distance = 8;

    public AntiCreeperDetection() {
        super("combat/anti_creeper",
                new ActionBarTitleAction(),
                new PlaySoundAction(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 1.414214F, 3),
                new PauseAction(),
                new QuitAction());
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        double minD = Double.POSITIVE_INFINITY;
        CreeperEntity e = null;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof CreeperEntity c && !c.isDead()) {
                double d = player.squaredDistanceTo(entity);
                if (d < minD) {
                    minD = d;
                    e = c;
                }
            }
        }
        if (e != null) {
            if (minD <= distance * distance) {
                double d2 = Math.sqrt(minD);
                ActionBarTitleAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title"));
                if (isActionEffectivelyEnabled(action)) action.updateTitle(client, world, e, d2, style -> {
                    if (d2 <= 1 / 2.0 * distance) return style.withColor(Formatting.RED).withBold(true);
                    else if (d2 <= 3 / 4.0 * distance) return style.withColor(Formatting.GOLD);
                    else return style.withColor(Formatting.YELLOW);
                });
                PlaySoundAction action2 = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/other/play_sound"));
                if (isActionEffectivelyEnabled(action2)) {
                    action2.tick(client);
                    action2.setPlaying(e.getLerpedFuseTime(client.getRenderTickCounter().getTickProgress(world.getTickManager().shouldSkipTick(e))) > 0);
                }
                if (d2 <= 2 / 3.0 * distance) {
                    QuitAction action3 = getBoundAction(Identifier.of(ModContext.MOD_ID, "active/afk/quit"));
                    if (isActionEffectivelyEnabled(action3)) action3.quit(client, world, getName());
                    PauseAction action4 = getBoundAction(Identifier.of(ModContext.MOD_ID, "active/afk/pause"));
                    if (isActionEffectivelyEnabled(action4)) action4.pause(client, getName());
                }
            }
        }
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientWorld world, CreeperEntity creeper, double distance, UnaryOperator<Style> styleProvider) {
            Camera camera = client.gameRenderer.getCamera();
            double relativeYaw = getRelativeYaw(camera.getCameraPos(), camera.getCameraYaw(), creeper, e -> client.getRenderTickCounter().getTickProgress(!world.getTickManager().shouldSkipTick(e)));
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
            client.inGameHud.setOverlayMessage(Text.literal("警告：苦力怕距离你 %.1f 方块 %s".formatted(distance, directionIndicator)).styled(styleProvider), false);
        }

        private double getRelativeYaw(Vec3d cameraPos, float yaw, Entity entity, EntityTickProgress tickProgress) {
            Vec3d vec3d = cameraPos.subtract(entity.getCameraPosVec(tickProgress.getTickProgress(entity))).rotateYClockwise();
            float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float) Math.PI);
            return MathHelper.subtractAngles(yaw, f);
        }
    }
}
