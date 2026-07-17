package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.OutlineAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.Comparator;
import java.util.List;

public class AntiAmbushDetection extends Detection {
    public AntiAmbushDetection() {
        super("combat/anti_ambush", new OutlineAction());
        ClientPlayerTickEvents.START_TICK.register((client, world, player) -> {
            if (getStateNode().isEffectivelyEnabled()) onStartTick(client, world, player);
        });
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        final int checkInterval = 5;
        List<LivingEntity> entities = world.getOtherEntities(
                        player,
                        player.getBoundingBox().expand(16),
                        entity -> entity instanceof LivingEntity && entity.isAlive()
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity.age % checkInterval == 0)
                .filter(entity -> player.squaredDistanceTo(entity) <= 16 * 16)
                .filter(entity -> (entity.isInvisible() && entity.isInvisibleTo(player)) || !player.canSee(entity))
                .sorted(Comparator.comparing(player::squaredDistanceTo))
                .toList();
        @SuppressWarnings("DataFlowIssue") final int color = Formatting.GOLD.getColorValue();
        if (!entities.isEmpty())
            client.inGameHud.setOverlayMessage(
                    Text.literal("警告：发现潜在偷袭者，数量：")
                            .append(String.valueOf(entities.size()))
                            .append(" 最近者：")
                            .append(entities.getFirst().getDisplayName())
                            .append(" ")
                            .append(Utils.getDirectionIndicator(client, world, entities.getFirst(), client.gameRenderer.getCamera()))
                            .withColor(color),
                    false
            );
        for (LivingEntity entity : entities) {
            OutlineAction action = getBoundAction(Identifier.of(ModContext.MOD_ID, "passive/other/outline"));
            action.outline(entity, 60, color);
        }
    }
}
