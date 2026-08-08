package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        tickCounter++;
        if (tickCounter % checkInterval != 0) return;

        if (!Utils.hasDestroyIntention(client, world, player, pos -> true)) {
            modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
            return;
        }

        // 1.20.6: ultrawarm() instead of EnvironmentAttributes
        boolean isNether = world.getDimension().ultrawarm();
        int range = isNether ? checkRangeInNether : checkRange;

        BlockPos playerPos = player.getBlockPos();
        List<BlockPos> lavaPositions = new ArrayList<>();

        for (int dx = -range; dx <= range; dx++)
            for (int dy = -range; dy <= range; dy++)
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    if (world.getBlockState(pos).isOf(Blocks.LAVA))
                        lavaPositions.add(pos.toImmutable());
                }

        if (lavaPositions.isEmpty()) {
            modContext.filledThroughWallsRenderer().clearByTag(getId().toString());
            return;
        }

        Vec3d eyePos = player.getEyePos();
        BlockPos nearestPos = lavaPositions.stream()
                .min(Comparator.comparingDouble(pos -> Vec3d.ofCenter(pos).squaredDistanceTo(eyePos)))
                .orElseThrow();

        Camera camera = client.gameRenderer.getCamera();
        double distance = Math.sqrt(Vec3d.ofCenter(nearestPos).squaredDistanceTo(player.getEyePos()));
        String direction = Utils.getDirectionIndicator(camera, Vec3d.ofCenter(nearestPos));
        MutableText message = Text.translatable("detection.safeguard.environment.lava.warning", String.format("%.0f", distance), direction);

        tryExecuteAction(ActionBarTitleAction.class,
                action -> action.updateTitle(DangerLevel.MEDIUM, message, client));
        tryExecuteAction(BlockOutlineAction.class,
                action -> action.outline(lavaPositions, highlightColor));
    }
}
