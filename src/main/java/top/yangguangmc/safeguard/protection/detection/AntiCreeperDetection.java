package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.PlaySoundAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

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
                if (e.getVelocity().lengthSquared() >= 0.0001) {
                    ActionBarTitleAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title"));
                    if (isActionEffectivelyEnabled(action)) action.updateTitle(client, d2, style -> {
                        if (d2 <= 1 / 2.0 * distance) return style.withColor(Formatting.RED).withBold(true);
                        else if (d2 <= 3 / 4.0 * distance) return style.withColor(Formatting.GOLD);
                        else return style.withColor(Formatting.YELLOW);
                    });
                }
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
}
