package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.attribute.EnvironmentAttributes;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.action.BlockOutlineAction;
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

        boolean isNether = world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.FAST_LAVA_GAMEPLAY);
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

        tryExecuteAction(ActionBarTitleAction.class,
                action -> action.updateTitle(client, player, nearestPos));
        tryExecuteAction(BlockOutlineAction.class,
                action -> action.outline(lavaPositions, highlightColor));
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, ClientPlayerEntity player, BlockPos nearestLava) {
            double distance = Math.sqrt(Vec3d.ofCenter(nearestLava).squaredDistanceTo(player.getEyePos()));
            String direction = getDirectionIndicator(player, nearestLava);

            MutableText text = Text.literal("警告：最近岩浆距离 ")
                    .append(String.format("%.1f", distance))
                    .append("，方向 ")
                    .append(direction);
            client.inGameHud.setOverlayMessage(text.styled(style -> style.withColor(Formatting.GOLD)), false);
        }

        /**
         * 根据玩家朝向计算目标方块相对于玩家的方位指示符。
         */
        private static String getDirectionIndicator(ClientPlayerEntity player, BlockPos targetPos) {
            double dx = targetPos.getX() + 0.5 - player.getX();
            double dz = targetPos.getZ() + 0.5 - player.getZ();
            double angle = Math.toDegrees(Math.atan2(dz, dx)) - player.getYaw() - 90;   // 技术债：为什么要旋转 90 度？
            double relativeYaw = MathHelper.wrapDegrees(angle);

            String horizontal;
            if (relativeYaw >= -157.5 && relativeYaw < -112.5) horizontal = "↙";
            else if (relativeYaw >= -112.5 && relativeYaw < -67.5) horizontal = "←";
            else if (relativeYaw >= -67.5 && relativeYaw < -22.5) horizontal = "↖";
            else if (relativeYaw >= -22.5 && relativeYaw < 22.5) horizontal = "↑";
            else if (relativeYaw >= 22.5 && relativeYaw < 67.5) horizontal = "↗";
            else if (relativeYaw >= 67.5 && relativeYaw < 112.5) horizontal = "→";
            else if (relativeYaw >= 112.5 && relativeYaw < 157.5) horizontal = "↘";
            else horizontal = "↓";

            double dy = targetPos.getY() + 0.5 - player.getEyeY();
            if (dy > 3) return "上方" + horizontal;
            if (dy < -3) return "下方" + horizontal;
            return horizontal;
        }
    }
}