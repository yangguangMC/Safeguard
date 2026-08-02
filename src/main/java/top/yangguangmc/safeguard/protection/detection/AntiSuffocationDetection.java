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
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.List;
import java.util.Objects;

public class AntiSuffocationDetection extends Detection {
    /**
     * 窒息检测箱体宽度因子，源自 {@link net.minecraft.entity.Entity#isInsideWall()}
     */
    private static final float SUFFOCATION_BOX_WIDTH_FACTOR = 0.8F;
    /**
     * 窒息检测箱体高度，源自 {@link net.minecraft.entity.Entity#isInsideWall()}
     */
    private static final double SUFFOCATION_BOX_HEIGHT = 1.0E-6;
    /**
     * 向上扫描受重力影响对象的最大数量
     */
    @SuppressWarnings("FieldMayBeFinal")
    private int maxFallingScanCount = 36;

    public AntiSuffocationDetection() {
        super("environment/anti_suffocation", new ActionBarTitleAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        // 情况1：正在窒息
        Text suffocatingBlock = getSuffocatingBlockName(world, player);
        if (suffocatingBlock != null) {
            FallingScanResult aboveResult = scanFallingAbove(world, player, BlockPos.ofFloored(player.getEyePos()));
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.CRITICAL,
                            buildMessage(true, suffocatingBlock, aboveResult.count()),
                            client));
            return;
        }
        // 情况2：上方有坠落对象（Falling 方块 或 FallingBlockEntity）
        FallingScanResult aboveResult = scanFallingBlockEntities(world, player);
        if (aboveResult.count() > 0) {
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(DangerLevel.LOW,
                            buildMessage(false, aboveResult.nearestName(), aboveResult.count()),
                            client));
            return;
        }
        // 情况3：挖掘头顶方块意图
        if (Utils.hasDestroyIntention(client, world, player, pos -> isBlockAbovePlayerHead(pos, player))) {
            BlockPos targetPos = ((BlockHitResult) Objects.requireNonNull(client.crosshairTarget)).getBlockPos();
            if (hasFallingBlockNearTarget(world, targetPos)) {
                FallingScanResult aboveTarget = scanFallingBlocks(world, targetPos);
                if (aboveTarget.count() > 0 && !aboveTarget.nearestName().getString().isBlank())
                    tryExecuteAction(ActionBarTitleAction.class, action ->
                            action.updateTitle(DangerLevel.LOW,
                                    buildMessage(false, aboveTarget.nearestName(), aboveTarget.count()),
                                    client));
            }
        }
    }

    /**
     * 构建 ActionBar 消息文本（纯文本，不含样式）。
     */
    private static MutableText buildMessage(boolean suffocating, Text block, int count) {
        MutableText text = Text.translatable("detection.safeguard.environment.anti_suffocation.warning_prefix")
                .append(suffocating
                        ? Text.translatable("detection.safeguard.environment.anti_suffocation.suffocating")
                        : Text.translatable("detection.safeguard.environment.anti_suffocation.above"))
                .append(block);
        if (count > 0)
            text.append(Text.translatable("detection.safeguard.environment.anti_suffocation.count", String.valueOf(count)));
        return text;
    }

    /**
     * 向上扫描受重力影响的对象（Falling 方块 + FallingBlockEntity），返回合并结果。
     *
     * @param world    当前客户端世界
     * @param player   玩家
     * @param startPos 扫描起始位置
     * @return 扫描结果（count + nearestName）
     */
    private FallingScanResult scanFallingAbove(ClientWorld world, ClientPlayerEntity player, BlockPos startPos) {
        FallingScanResult blocks = scanFallingBlocks(world, startPos);
        FallingScanResult entities = scanFallingBlockEntities(world, player);
        Text name = blocks.nearestName().getString().isBlank()
                ? entities.nearestName()
                : blocks.nearestName();
        return new FallingScanResult(Math.min(blocks.count() + entities.count(), maxFallingScanCount + 1), name);
    }

    /**
     * 向上扫描实现 {@link Falling} 接口的方块。
     * <p>
     * 使用手动 for 循环而非 {@link BlockPos#stream(BlockPos, BlockPos)}，
     * 因为后者内部复用同一个 {@link BlockPos.Mutable} 对象，无法收集为列表（.toList() 中所有元素引用相同坐标）。
     *
     * @param world    当前客户端世界
     * @param startPos 扫描起始位置
     * @return 扫描结果
     */
    private FallingScanResult scanFallingBlocks(ClientWorld world, BlockPos startPos) {
        int count = 0;
        Text name = Text.empty();
        for (BlockPos pos = startPos; count < maxFallingScanCount + 1 && !world.isOutOfHeightLimit(pos); pos = pos.up()) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof Falling) {
                count++;
                if (name.getString().isBlank()) name = block.getName();
            }
        }
        return new FallingScanResult(count, name);
    }

    /**
     * 扫描头顶的 {@link FallingBlockEntity} 实体。
     *
     * @param world  当前客户端世界
     * @param player 玩家
     * @return 扫描结果
     */
    private FallingScanResult scanFallingBlockEntities(ClientWorld world, ClientPlayerEntity player) {
        List<Entity> entities = world.getOtherEntities(player,
                player.getBoundingBox().expand(0.5).withMinY(player.getEyeY()).withMaxY(player.getEyeY() + maxFallingScanCount),
                entity -> entity instanceof FallingBlockEntity);
        int count = Math.min(entities.size(), maxFallingScanCount + 1);
        Text name = entities.isEmpty() ? Text.empty() : entities.getFirst().getDisplayName();
        return new FallingScanResult(count, name);
    }

    /**
     * 获取玩家当前正在窒息碰撞的方块名称。
     * 算法源自 {@link net.minecraft.entity.Entity#isInsideWall()}。
     *
     * @param world  当前客户端世界
     * @param player 玩家
     * @return 窒息方块的名称，未窒息时返回 {@code null}
     */
    @Nullable
    private Text getSuffocatingBlockName(ClientWorld world, ClientPlayerEntity player) {
        if (!player.isInsideWall()) return null;
        float f = player.getWidth() * SUFFOCATION_BOX_WIDTH_FACTOR;
        Box box = Box.of(player.getEyePos(), f, SUFFOCATION_BOX_HEIGHT, f);
        return BlockPos.stream(box).filter(pos -> {
            BlockState state = world.getBlockState(pos);
            return !state.isAir()
                    && state.shouldSuffocate(world, pos)
                    && VoxelShapes.matchesAnywhere(state.getCollisionShape(world, pos).offset(pos), VoxelShapes.cuboid(box), BooleanBiFunction.AND);
        }).findAny().map(pos -> world.getBlockState(pos).getBlock().getName()).orElse(null);
    }

    /**
     * 判断目标方块上方 2 格内是否存在 {@link Falling} 类型的方块。
     * <p>
     * 同样不依赖 {@link BlockPos#stream(BlockPos, BlockPos)} 以避开 Mutable 复用陷阱。
     */
    private boolean hasFallingBlockNearTarget(ClientWorld world, BlockPos targetPos) {
        for (BlockPos pos = targetPos; pos.getY() <= targetPos.getY() + 2; pos = pos.up()) {
            if (world.getBlockState(pos).getBlock() instanceof Falling) return true;
        }
        return false;
    }

    /**
     * 判断方块位置是否在玩家头顶（水平方向上与玩家重心距离 ≤1，Y 轴高于眼部高度）。
     */
    private static boolean isBlockAbovePlayerHead(BlockPos pos, ClientPlayerEntity player) {
        return Math.abs(Vec3d.ofCenter(pos).getX() - player.getX()) <= 1
                && pos.getY() > player.getEyeY()
                && Math.abs(Vec3d.ofCenter(pos).getZ() - player.getZ()) < 1;
    }

    private record FallingScanResult(int count, Text nearestName) {
    }
}