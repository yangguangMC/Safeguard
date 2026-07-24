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
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.PlaySoundAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.function.UnaryOperator;

public class AntiCreeperDetection extends Detection {
    private static final Identifier ACTION_BAR_TITLE_ID = Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title");
    private static final Identifier PLAY_SOUND_ID = Identifier.of(ModContext.MOD_ID, "passive/other/play_sound");
    private static final Identifier QUIT_ID = Identifier.of(ModContext.MOD_ID, "active/afk/quit");
    private static final Identifier PAUSE_ID = Identifier.of(ModContext.MOD_ID, "active/afk/pause");

    private final double distance = 8;

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
            tryExecuteAction(ACTION_BAR_TITLE_ID, action ->
                    ((ActionBarTitleAction) action).updateTitle(client, world, creeper, d2, fuseTime, style -> {
                        if (d2 <= 1 / 2.0 * distance) return style.withColor(Formatting.RED).withBold(true);
                        else if (d2 <= 3 / 4.0 * distance) return style.withColor(Formatting.GOLD);
                        else return style.withColor(Formatting.YELLOW);
                    }));
            tryExecuteAction(PLAY_SOUND_ID, action -> {
                PlaySoundAction ps = (PlaySoundAction) action;
                ps.tick(client);
                ps.setPlaying(fuseTime > 0);
            });
            if (d2 <= 2 / 3.0 * distance) {
                tryExecuteAction(QUIT_ID, action ->
                        ((QuitAction) action).quit(client, world, getName()));
                tryExecuteAction(PAUSE_ID, action ->
                        ((PauseAction) action).pause(client, getName()));
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
            client.inGameHud.setOverlayMessage(Text.literal("警告：苦力怕距离你 %.1f 方块 倒计时：%.0f%% %s".formatted(distance, fuseTime * 100, directionIndicator)).styled(styleProvider), false);
        }
    }
}