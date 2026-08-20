package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.BlockOutlineAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.protection.option.ColorOption;
import top.yangguangmc.safeguard.protection.option.ConfigOption;
import top.yangguangmc.safeguard.protection.option.IntOption;
import top.yangguangmc.safeguard.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LavaDetection extends Detection {
    private final IntOption checkInterval = registerOption(IntOption.of("checkInterval", 5).range(1, 40));
    private final IntOption checkRange = registerOption(IntOption.of("checkRange", 3).range(2, 8));
    private final IntOption checkRangeInNether = registerOption(IntOption.of("checkRangeInNether", 6).range(2, 16));
    private int tickCounter;

    public LavaDetection() {
        super("environment/lava", new ActionBarTitleAction(), new LavaBlockOutlineAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    @Override
    public void applyActiveState(boolean active) {
        super.applyActiveState(active);
        if (!active) modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        tickCounter++;
        if (tickCounter % checkInterval.get() != 0) return;

        if (!Utils.hasDestroyIntention(client, world, player, pos -> true)) {
            modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
            return;
        }

        boolean isNether = world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
        int range = isNether ? checkRangeInNether.get() : checkRange.get();

        BlockPos playerPos = player.blockPosition();
        List<BlockPos> lavaPositions = new ArrayList<>();

        for (int dx = -range; dx <= range; dx++)
            for (int dy = -range; dy <= range; dy++)
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (world.getBlockState(pos).is(Blocks.LAVA))
                        lavaPositions.add(pos.immutable());
                }

        if (lavaPositions.isEmpty()) {
            modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        BlockPos nearestPos = lavaPositions.stream()
                .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(eyePos)))
                .orElseThrow();

        Camera camera = client.gameRenderer.getMainCamera();
        double distance = Math.sqrt(Vec3.atCenterOf(nearestPos).distanceToSqr(player.getEyePosition()));
        String direction = Utils.getDirectionIndicator(camera, Vec3.atCenterOf(nearestPos));
        MutableComponent message = Component.translatable("detection.safeguard.environment.lava.warning", String.format("%.0f", distance), direction);

        tryExecuteAction(ActionBarTitleAction.class,
                action -> action.updateTitle(DangerLevel.MEDIUM, message, client));
        tryExecuteAction(LavaBlockOutlineAction.class,
                action -> action.outline(lavaPositions));
    }

    /**
     * 本检测项专属的方块高亮：颜色默认为半透明橙红色，作为"检测项-动作对专属"配置项可单独调整
     * （见 {@link BlockOutlineAction} 类文档）。
     */
    private static class LavaBlockOutlineAction extends BlockOutlineAction {
        private final ConfigOption<Integer> color =
                registerOption(ColorOption.of("color", 0x66FF4500, true).pairScoped());

        public void outline(Collection<BlockPos> positions) {
            outline(positions, color.get());
        }
    }
}