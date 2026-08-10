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
import top.yangguangmc.safeguard.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LavaDetection extends Detection {
    @SuppressWarnings("FieldMayBeFinal")
    private int highlightColor = 0x66FF4500;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int checkInterval = 5;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int checkRange = 3;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int checkRangeInNether = 6;
    private int tickCounter;

    public LavaDetection() {
        super("environment/lava", new ActionBarTitleAction(), new BlockOutlineAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    @Override
    public void applyActiveState(boolean active) {
        super.applyActiveState(active);
        if (!active) modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        tickCounter++;
        if (tickCounter % checkInterval != 0) return;

        if (!Utils.hasDestroyIntention(client, world, player, pos -> true)) {
            modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
            return;
        }

        boolean isNether = world.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
        int range = isNether ? checkRangeInNether : checkRange;

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
        tryExecuteAction(BlockOutlineAction.class,
                action -> action.outline(lavaPositions, highlightColor));
    }
}