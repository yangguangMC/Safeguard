package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.OutlineAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.Comparator;
import java.util.List;

public class AntiAmbushDetection extends Detection {
    public AntiAmbushDetection() {
        super("combat/anti_ambush", new ActionBarTitleAction(), new OutlineAction());
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        final int checkInterval = 5;
        List<LivingEntity> entities = world.getOtherEntities(
                        player,
                        player.getBoundingBox().expand(16),
                        entity -> entity.isAlive() && (
                                entity instanceof HostileEntity
                                        || entity instanceof PlayerEntity
                                        || entity instanceof SlimeEntity
                                        || entity instanceof PhantomEntity
                                        || entity instanceof GhastEntity
                        )
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity.age % checkInterval == 0)
                .filter(entity -> player.squaredDistanceTo(entity) <= 16 * 16)
                .filter(other -> !player.isTeammate(other))
                .filter(entity -> (entity.isInvisible() && entity.isInvisibleTo(player)) || !player.canSee(entity))
                .sorted(Comparator.comparing(player::squaredDistanceTo))
                .toList();
        @SuppressWarnings("DataFlowIssue") final int color = Formatting.GOLD.getColorValue();
        if (!entities.isEmpty()) {
            ActionBarTitleAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/hud/action_bar_title"));
            if (isActionEffectivelyEnabled(action))
                action.updateTitle(client, world, entities.size(), entities.getFirst(), color);
        }
        for (LivingEntity entity : entities) {
            OutlineAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/other/outline"));
            if (isActionEffectivelyEnabled(action)) action.outline(entity, 60, color);
        }
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientWorld world, int amount, Entity nearest, int color) {
            client.inGameHud.setOverlayMessage(
                    Text.literal("警告：发现潜在偷袭者，数量：")
                            .append(String.valueOf(amount))
                            .append(" 最近者：")
                            .append(nearest.getDisplayName())
                            .append(" ")
                            .append(Utils.getDirectionIndicator(client, world, nearest, client.gameRenderer.getCamera()))
                            .withColor(color),
                    false
            );
        }
    }
}
