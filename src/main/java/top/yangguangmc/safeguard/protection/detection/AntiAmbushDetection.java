package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.action.OutlineAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.Comparator;
import java.util.List;

public class AntiAmbushDetection extends Detection {
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int checkInterval = 5;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int checkDistance = 12;
    private int tickCounter;

    public AntiAmbushDetection() {
        super("combat/anti_ambush", new ActionBarTitleAction(), new OutlineAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        tickCounter++;
        if (tickCounter % checkInterval != 0) return;
        List<LivingEntity> entities = world.getEntities(
                        player,
                        player.getBoundingBox().inflate(checkDistance),
                        entity -> entity.isAlive() && (
                                entity instanceof Monster
                                        || entity instanceof Player
                                        || entity instanceof Slime
                                        || entity instanceof Phantom
                                        || entity instanceof Ghast
                        )
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> player.distanceToSqr(entity) <= checkDistance * checkDistance)
                .filter(other -> !player.isAlliedTo(other))
                .filter(entity -> {
                    // 规则A: 非玩家且坐在载具中 → 排除（无法发起偷袭）
                    if (!(entity instanceof Player) && entity.isPassenger()) return false;
                    // 规则B: 非玩家处于洞穴而玩家不处于 → 排除
                    if (!(entity instanceof Player)
                            && !world.canSeeSky(entity.blockPosition())
                            && world.canSeeSky(player.blockPosition())) return false;
                    // 规则C: 除苦力怕外在背后且正在靠近且速度快于玩家 → 即使视野可见也强制计入
                    Camera camera = client.gameRenderer.getMainCamera();
                    if (!(entity instanceof Creeper)
                            && Utils.isApproaching(entity, player)
                            && entity.getDeltaMovement().lengthSqr() > player.getDeltaMovement().lengthSqr()
                            && Utils.isBehindPlayer(client, world, entity, camera)) return true;
                    // 基础规则: 隐身/不可见才计入
                    return (entity.isInvisible() && entity.isInvisibleTo(player)) || !player.hasLineOfSight(entity);
                })
                .sorted(Comparator.comparing(player::distanceToSqr))
                .toList();
        if (!entities.isEmpty()) {
            Entity nearest = entities.getFirst();
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.MEDIUM,
                            Component.translatable("detection.safeguard.combat.anti_ambush.warning",
                                    String.valueOf(entities.size()),
                                    nearest.getDisplayName(),
                                    Utils.getDirectionIndicator(client, world, nearest, client.gameRenderer.getMainCamera())),
                            client));
        }
        @SuppressWarnings("DataFlowIssue") int outlineColor = ChatFormatting.GOLD.getColor();
        for (LivingEntity entity : entities) {
            tryExecuteAction(OutlineAction.class, action -> action.outline(entity, 60, outlineColor));
        }
    }
}
