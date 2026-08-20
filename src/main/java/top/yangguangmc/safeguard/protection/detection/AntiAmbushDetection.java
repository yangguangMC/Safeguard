package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.action.OutlineAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.protection.option.ColorOption;
import top.yangguangmc.safeguard.protection.option.ConfigOption;
import top.yangguangmc.safeguard.protection.option.IntOption;
import top.yangguangmc.safeguard.util.Utils;

import java.util.Comparator;
import java.util.List;

public class AntiAmbushDetection extends Detection {
    private final IntOption checkInterval = registerOption(IntOption.of("checkInterval", 5).range(1, 40));
    private final IntOption checkDistance = registerOption(IntOption.of("checkDistance", 12).range(4, 64));
    private int tickCounter;

    public AntiAmbushDetection() {
        super("combat/anti_ambush", new ActionBarTitleAction(), new AmbushOutlineAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        tickCounter++;
        if (tickCounter % checkInterval.get() != 0) return;
        int distance = checkDistance.get();
        List<LivingEntity> entities = world.getEntities(
                        player,
                        player.getBoundingBox().inflate(distance),
                        entity -> entity.isAlive() && (
                                entity instanceof Monster
                                        || entity instanceof Player
                                        || entity instanceof Slime
                                        || entity instanceof Phantom
                                        || entity instanceof Ghast
                        )
                ).stream()
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> player.distanceToSqr(entity) <= (double) distance * distance)
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
        for (LivingEntity entity : entities) {
            tryExecuteAction(AmbushOutlineAction.class, action -> action.outline(entity, 60));
        }
    }

    /**
     * 本检测项专属的描边高亮：颜色默认为金色，作为"检测项-动作对专属"配置项可单独调整
     * （见 {@link OutlineAction} 类文档）。
     */
    private static class AmbushOutlineAction extends OutlineAction {
        private final ConfigOption<Integer> color =
                registerOption(ColorOption.of("color", TextColor.GOLD.getValue(), false).pairScoped());

        public void outline(Entity entity, int ticks) {
            outline(entity, ticks, color.get());
        }
    }
}
