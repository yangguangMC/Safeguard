package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        tickCounter++;
        if (tickCounter % checkInterval != 0) return;
        List<LivingEntity> entities = world.getOtherEntities(
                        player,
                        player.getBoundingBox().expand(checkDistance),
                        entity -> entity.isAlive() && (
                                entity instanceof HostileEntity
                                        || entity instanceof PlayerEntity
                                        || entity instanceof SlimeEntity
                                        || entity instanceof PhantomEntity
                                        || entity instanceof GhastEntity
                        )
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> player.squaredDistanceTo(entity) <= checkDistance * checkDistance)
                .filter(other -> !player.isTeammate(other))
                .filter(entity -> {
                    // 规则A: 非玩家且坐在载具中 → 排除（无法发起偷袭）
                    if (!(entity instanceof PlayerEntity) && entity.hasVehicle()) return false;
                    // 规则B: 非玩家处于洞穴而玩家不处于 → 排除
                    if (!(entity instanceof PlayerEntity)
                            && !world.isSkyVisible(entity.getBlockPos())
                            && world.isSkyVisible(player.getBlockPos())) return false;
                    // 规则C: 除苦力怕外在背后且正在靠近且速度快于玩家 → 即使视野可见也强制计入
                    Camera camera = client.gameRenderer.getCamera();
                    if (!(entity instanceof CreeperEntity)
                            && Utils.isApproaching(entity, player)
                            && entity.getVelocity().lengthSquared() > player.getVelocity().lengthSquared()
                            && Utils.isBehindPlayer(client, world, entity, camera)) return true;
                    // 基础规则: 隐身/不可见才计入
                    return (entity.isInvisible() && entity.isInvisibleTo(player)) || !player.canSee(entity);
                })
                .sorted(Comparator.comparing(player::squaredDistanceTo))
                .toList();
        if (!entities.isEmpty()) {
            Entity nearest = entities.getFirst();
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.MEDIUM,
                            Text.translatable("detection.safeguard.combat.anti_ambush.warning",
                                    String.valueOf(entities.size()),
                                    nearest.getDisplayName(),
                                    Utils.getDirectionIndicator(client, world, nearest, client.gameRenderer.getCamera())),
                            client));
        }
        @SuppressWarnings("DataFlowIssue") int outlineColor = Formatting.GOLD.getColorValue();
        for (LivingEntity entity : entities) {
            tryExecuteAction(OutlineAction.class, action -> action.outline(entity, 60, outlineColor));
        }
    }
}