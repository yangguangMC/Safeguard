package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Falling;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.List;
import java.util.Objects;

public class AntiSuffocationDetection extends Detection {
    @SuppressWarnings("FieldMayBeFinal")
    private int checkHeight = 99;

    public AntiSuffocationDetection() {
        super("environment/anti_suffocation", new ActionBarTitleAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        AboveContext above = getFallingAbove(world, player, BlockPos.ofFloored(player.getEyePos()));
        // 情况 1：正在窒息
        if (player.isInsideWall()) {
            // 算法来自官方 net.minecraft.entity.Entity.isInsideWall
            Box box = Box.of(player.getEyePos(), player.getWidth() * 0.8F, 1.0E-6, player.getWidth() * 0.8F);
            Text name = world.getBlockState(BlockPos.stream(box).filter(pos -> {
                BlockState blockState = world.getBlockState(pos);
                return !blockState.isAir()
                        && blockState.shouldSuffocate(world, pos)
                        && VoxelShapes.matchesAnywhere(blockState.getCollisionShape(world, pos).offset(pos), VoxelShapes.cuboid(box), BooleanBiFunction.AND);
            }).findAny().orElseThrow()).getBlock().getName();
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(client, true, name, above.count(), checkHeight));
            return;
        }
        // 情况 2：上方有下落中的方块
        if (!above.entities().isEmpty()) {
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(client, false, above.nearestName(), above.count(), checkHeight));
            return;
        }
        // 情况 3：尝试挖掘头顶方块
        if (Utils.hasDestroyIntention(client, world, player,
                pos -> Math.abs(Vec3d.ofCenter(pos).getX() - player.getX()) <= 1
                        && pos.getY() > player.getEyeY()
                        && Math.abs(Vec3d.ofCenter(pos).getZ() - player.getZ()) < 1)) {
            AboveContext above2 = getFallingAbove(world, player, ((BlockHitResult) Objects.requireNonNull(client.crosshairTarget)).getBlockPos());
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(client, false, above2.nearestName(), above2.count(), checkHeight));
        }
    }

    private AboveContext getFallingAbove(ClientWorld world, ClientPlayerEntity player, BlockPos startPos) {
        int count = 0;
        List<Entity> entities = null;
        Text name = null;
        for (BlockPos pos = startPos; count < checkHeight && !world.isOutOfHeightLimit(pos); pos = pos.up()) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof Falling) {
                count++;
                if (name == null) name = block.getName();
            }
        }
        if (count < checkHeight) {
            entities = world.getOtherEntities(player,
                    player.getBoundingBox().expand(0.5).withMinY(player.getEyeY()).withMaxY(player.getEyeY() + checkHeight),
                    entity -> entity instanceof FallingBlockEntity);
            count += entities.size();
            if (name == null && !entities.isEmpty()) name = entities.getFirst().getDisplayName();
        }
        return new AboveContext(count, entities == null ? List.of() : entities, name == null ? Text.empty() : name);
    }

    private record AboveContext(int count, List<Entity> entities, Text nearestName) {
    }

    private static class ActionBarTitleAction extends Action {
        public ActionBarTitleAction() {
            super("passive/hud/action_bar_title");
        }

        public void updateTitle(MinecraftClient client, boolean suffocating, Text block, int count, int maxCount) {
            MutableText text = Text.literal("警告：").append(suffocating ? "当前窒息于 " : (block == null ? "" : "当前上方为 ")).append(block == null ? Text.empty() : block);
            if (count > 0)
                text.append("，共有 ").append(count > maxCount ? maxCount + "+" : String.valueOf(count)).append(" 个");
            client.inGameHud.setOverlayMessage(text.styled(style -> style.withColor(Formatting.RED)), false);
        }
    }
}
