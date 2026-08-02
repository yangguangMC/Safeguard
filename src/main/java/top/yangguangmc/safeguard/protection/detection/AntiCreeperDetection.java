package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.protection.action.*;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

public class AntiCreeperDetection extends Detection {
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int distance = 12;

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
            double d2 = Math.sqrt(minD);
            float fuseTime = e.getLerpedFuseTime(client.getRenderTickCounter().getTickProgress(world.getTickManager().shouldSkipTick(e)));
            Camera camera = client.gameRenderer.getCamera();
            String directionIndicator = Utils.getDirectionIndicator(client, world, e, camera);

            DangerLevel level;
            if (fuseTime > 0) level = DangerLevel.CRITICAL;
            else if (d2 <= 1 / 3.0 * distance) level = DangerLevel.HIGH;
            else if (d2 <= 1 / 2.0 * distance) level = DangerLevel.MEDIUM;
            else level = DangerLevel.LOW;

            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(level,
                            Text.translatable("detection.safeguard.combat.anti_creeper.warning",
                                    String.format("%.1f", d2), String.format("%.0f%%", fuseTime * 100), directionIndicator),
                            client));
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
}