package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import top.yangguangmc.safeguard.protection.action.*;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.protection.option.IntOption;
import top.yangguangmc.safeguard.util.Utils;

public class AntiCreeperDetection extends Detection {
    private final IntOption distance = registerOption(IntOption.of("distance", 12).range(6, 24));

    public AntiCreeperDetection() {
        super("combat/anti_creeper",
                new ActionBarTitleAction(),
                new PlaySoundAction(SoundEvents.NOTE_BLOCK_HARP, 1.414214F, 3),
                new PauseAction(),
                new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        double minD = Double.POSITIVE_INFINITY;
        Creeper e = null;
        for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof Creeper c && !c.isDeadOrDying()) {
                double d = player.distanceToSqr(entity);
                if (d < minD) {
                    minD = d;
                    e = c;
                }
            }
        }
        int dist = distance.get();
        if (e != null && minD <= (double) dist * dist) {
            double d2 = Math.sqrt(minD);
            // 1.20.6: getClientFuseTime uses simple tick delta
            float fuseTime = e.getSwelling(client.getFrameTime());
            Camera camera = client.gameRenderer.getMainCamera();
            String directionIndicator = Utils.getDirectionIndicator(client, world, e, camera);

            DangerLevel level;
            if (fuseTime > 0) level = DangerLevel.CRITICAL;
            else if (d2 <= 1 / 3.0 * dist) level = DangerLevel.HIGH;
            else if (d2 <= 1 / 2.0 * dist) level = DangerLevel.MEDIUM;
            else level = DangerLevel.LOW;

            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(level,
                            Component.translatable("detection.safeguard.combat.anti_creeper.warning",
                                    String.format("%.1f", d2), String.format("%.0f%%", fuseTime * 100), directionIndicator),
                            client));
            tryExecuteAction(PlaySoundAction.class, action -> {
                action.tick(client);
                action.setPlaying(fuseTime > 0);
            });
            if (d2 <= 2 / 3.0 * dist) {
                tryExecuteAction(QuitAction.class, action -> action.quit(client));
                tryExecuteAction(PauseAction.class, action -> action.pause(client));
            }
        }
    }
}
