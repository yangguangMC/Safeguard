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
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.PlaySoundAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.function.UnaryOperator;

public class AntiCreeperDetection extends Detection {
    private final double distance = 12;

    public AntiCreeperDetection() {
        super("combat/anti_creeper",
                new ActionBarTitleAction(),
                new PlaySoundAction(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 1.414214F, 3),
                new PauseAction(),
                new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
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
        if (e != null && minD <= distance * distance) {
            CreeperEntity creeper = e;
            double d2 = Math.sqrt(minD);
            float fuseTime = creeper.getLerpedFuseTime(client.getRenderTickCounter().getTickProgress(world.getTickManager().shouldSkipTick(e)));
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(client, world, creeper, d2, fuseTime, style -> {
                        if (d2 <= 1 / 3.0 * distance) return style.withColor(Formatting.RED).withBold(fuseTime > 0);
                        else if (d2 <= 1 / 2.0 * distance) return style.withColor(Formatting.GOLD);
                        else return style.withColor(Formatting.YELLOW);
                    }));
            tryExecuteAction(PlaySoundAction.class, action -> {
                action.tick(client);
                action.setPlaying(fuseTime > 0);
            });
            if (d2 <= 2 / 3.0 * distance) {
                tryExecuteAction(QuitAction.class, action -> action.quit(client));
                tryExecuteAction(PauseAction.class, action -> action.pause(client));
            }
        }
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientWorld world, CreeperEntity creeper, double distance, float fuseTime, UnaryOperator<Style> styleProvider) {
            Camera camera = client.gameRenderer.getCamera();
            String directionIndicator = Utils.getDirectionIndicator(client, world, creeper, camera);
            client.inGameHud.setOverlayMessage(Text.translatable("detection.safeguard.combat.anti_creeper.warning", String.format("%.1f", distance), String.format("%.0f%%", fuseTime * 100), directionIndicator).styled(styleProvider), false);
        }
    }
}