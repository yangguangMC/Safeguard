package top.yangguangmc.safeguard.protection.detection;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.*;

public class AntiFallDetection extends Detection {
    public AntiFallDetection() {
        super("environment/anti_fall", new ActionBarTitleAction(), new QuitAction(), new PauseAction(), new MLGAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        // 防挖掘坠落
        if (player.getPitch() > 0 && Utils.hasDestroyIntention(client, world, player, pos -> pos.equals(player.supportingBlockPos.orElse(null)))) {
            List<SafeResult> result = checkSafety(8, ((BlockHitResult) Objects.requireNonNull(client.crosshairTarget)).getBlockPos(), world, player);
            result.removeIf(r -> r.unsafety() <= 0);
            if (!result.isEmpty())
                tryExecuteAction(ActionBarTitleAction.class, action -> action.updateTitle(client, result));
        }
        // 已坠落保护
        if (!player.isOnGround() && player.fallDistance > 1.5 && checkSafety(5, player.getBlockPos(), world, player).stream().allMatch(result -> result.unsafety() > 0)) {
            tryExecuteAction(PauseAction.class, action -> action.pause(client));
            tryExecuteAction(QuitAction.class, action -> action.quit(client));
        }
        // MLG
        tryExecuteAction(MLGAction.class, action -> action.tick(client, world, player));
    }

    private List<SafeResult> checkSafety(int checkHeight, BlockPos pos, ClientWorld world, ClientPlayerEntity player) {
        List<SafeResult> results = new ArrayList<>();
        for (int i = 1; i <= checkHeight; i++) {
            BlockState state = world.getBlockState(pos.down(i));
            if (!state.hasSolidTopSurface(world, pos, player)) {
                if (state.isAir()) {
                    results.add(new SafeResult(Text.translatable("detection.safeguard.environment.anti_fall.cliff"), i, 2));
                    continue;
                } else if (state.isOf(Blocks.WATER)) {
                    results.add(new SafeResult(Blocks.WATER.getName(), i, 1));
                    continue;
                } else if (state.isOf(Blocks.LAVA)) {
                    results.add(new SafeResult(Blocks.LAVA.getName(), i, 3));
                    continue;
                }
            }
            results.add(new SafeResult(Text.translatable("detection.safeguard.environment.anti_fall.solid"), i, 0));
        }
        return results;
    }

    private record SafeResult(Text name, int posBelow, int unsafety) {
    }

    private static class MLGAction extends Action {
        private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);
        private static final Collection<Item> MLG_ITEMS_NETHER = ImmutableSet.of(
                Items.SCAFFOLDING,
                Items.COBWEB,
                Items.POWDER_SNOW_BUCKET,
                Items.HAY_BLOCK,
                Items.SLIME_BLOCK,
                Items.HONEY_BLOCK,
                Items.TWISTING_VINES
        );
        private static final Collection<Item> MLG_ITEMS_NORMAL = ImmutableSet.of(
                Items.WATER_BUCKET,
                Items.SCAFFOLDING,
                Items.COBWEB,
                Items.POWDER_SNOW_BUCKET,
                Items.HAY_BLOCK,
                Items.SLIME_BLOCK,
                Items.HONEY_BLOCK,
                Items.TWISTING_VINES
        );
        @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
        private double minFallDistance = 3;
        @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
        private double minHeight = 6;
        private int placementSchedule = -1;

        public MLGAction() {
            super("active/other/mlg");
        }

        @Override
        public boolean defaultEnabled() {
            return false;
        }

        public void tick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
            if (placementSchedule != -1) LOGGER.debug("[MLG Action] Schedule: {}", placementSchedule);
            // 若已经计划好位置了
            if (placementSchedule >= 0) {
                if (placementSchedule <= 10) {
                    List<ItemStack> hotbar = player.getInventory().getMainStacks().stream().limit(9).toList();
                    int slot = 8;
                    for (int i = 0; i < hotbar.size(); i++) {
                        if (world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY)
                                ? MLG_ITEMS_NETHER.contains(hotbar.get(i).getItem())
                                : MLG_ITEMS_NORMAL.contains(hotbar.get(i).getItem())) {
                            slot = i;
                            break;
                        }
                    }
                    player.getInventory().setSelectedSlot(slot);
                    player.setPitch(Math.min(player.getPitch() + 22.5F, 90.0F));
                }
                if (placementSchedule == 0) {
                    LOGGER.debug("[MLG Action] Simulated use.");
                    client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
                    Utils.simulatePress(client.options.useKey);
                    placementSchedule = -1;
                    return;
                }
                placementSchedule--;
                return;
            }
            if (player.isOnGround() || player.fallDistance < minFallDistance || player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                    || player.isGliding() || player.getInventory().getMainStacks().stream().limit(9).map(ItemStack::getItem)
                    .noneMatch(world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY)
                            ? MLG_ITEMS_NETHER::contains : MLG_ITEMS_NORMAL::contains)) {
                placementSchedule = -1;
                return;
            }
            // 开始进行位置计划
            BlockPos blockPos = player.getBlockPos();
            while (world.getBlockState(blockPos).isAir() && !world.isOutOfHeightLimit(blockPos))
                blockPos = blockPos.down();
            if (world.getBlockState(blockPos).isAir() || world.getBlockState(blockPos).isOf(Blocks.WATER)) {
                placementSchedule = -1;
                return;
            }
            blockPos = blockPos.up();
            if (player.getY() - blockPos.getY() < minHeight) {
                placementSchedule = -1;
                return;
            }
            LOGGER.debug("[MLG Action] Start placement reasoning. CurrentY: {}, groundY: {}", player.getY(), blockPos.getY());
            double posY = player.getEyeY();
            double targetY = blockPos.getY();
            double velocityY = player.getVelocity().y;
            double lastDistance = posY - targetY;
            for (int i = 1; ; i++) {
                posY = posY + velocityY;
                velocityY = (velocityY - player.getFinalGravity()) * 0.98;
                double distance = posY - targetY;
                if (distance <= player.getBlockInteractionRange() || (distance > lastDistance && velocityY < 0)) {
                    i -= Math.abs(velocityY) < 1.85 ? 1 : 2; // 不知道是响应延迟还是模拟误差，必须根据速度提前 1 ~ 2 刻以防摔死
                    LOGGER.debug("[MLG Action] Scheduled water placement at {} ticks later. TargetY: {}, endPlayerY: {}, endDistance: {}, endVelocityY: {}",
                            i, targetY, posY, distance, velocityY);
                    placementSchedule = i;
                    break;
                }
                if (distance < lastDistance) lastDistance = distance;
                if (i > 320) throw new AssertionError("Too high iteration count! This should not happen!");
            }
        }
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, List<SafeResult> results) {
            MutableText message = Text.empty();
            results.stream().max(Comparator.comparing(SafeResult::unsafety)).ifPresent(result -> {
                message.append(Text.translatable("detection.safeguard.environment.anti_fall.warning",
                        result.posBelow(), result.name()).styled(style -> {
                    if (result.unsafety() > 2) return style.withColor(Formatting.RED).withBold(true);
                    return style.withColor(Formatting.RED);
                }));
                results.stream()
                        .min(Comparator.comparing(SafeResult::posBelow))
                        .filter(o -> !result.equals(o)).ifPresent(result2 ->
                                message.append(Text.translatable("detection.safeguard.environment.anti_fall.warning_secondary",
                                        result2.name(), result2.posBelow()).styled(style -> {
                                    if (result2.unsafety() > 2) return style.withColor(Formatting.RED).withBold(true);
                                    return style.withColor(Formatting.RED);
                                }))
                        );
            });
            client.inGameHud.setOverlayMessage(message, false);
        }
    }
}
