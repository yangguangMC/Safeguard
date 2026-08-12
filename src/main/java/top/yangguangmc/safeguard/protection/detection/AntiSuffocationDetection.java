package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.List;
import java.util.Objects;

public class AntiSuffocationDetection extends Detection {
    /**
     * 窒息检测箱体宽度因子，源自 {@link net.minecraft.world.entity.Entity#isInWall()}
     */
    private static final float SUFFOCATION_BOX_WIDTH_FACTOR = 0.8F;
    /**
     * 窒息检测箱体高度，源自 {@link net.minecraft.world.entity.Entity#isInWall()}
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

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        // 情况1：正在窒息
        Component suffocatingBlock = getSuffocatingBlockName(world, player);
        if (suffocatingBlock != null) {
            FallingScanResult aboveResult = scanFallingAbove(world, player, BlockPos.containing(player.getEyePosition()));
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
            BlockPos targetPos = ((BlockHitResult) Objects.requireNonNull(client.hitResult)).getBlockPos();
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
    private static MutableComponent buildMessage(boolean suffocating, Component block, int count) {
        MutableComponent text = Component.empty()
                .append(suffocating
                        ? Component.translatable("detection.safeguard.environment.anti_suffocation.suffocating")
                        : Component.translatable("detection.safeguard.environment.anti_suffocation.above"))
                .append(block);
        if (count > 0)
            text.append(Component.translatable("detection.safeguard.environment.anti_suffocation.count", String.valueOf(count)));
        return text;
    }

    /**
     * 向上扫描受重力影响的对象（{@link Fallable} 方块 + {@link FallingBlockEntity}），返回合并结果。
     *
     * @param world    当前客户端世界
     * @param player   玩家
     * @param startPos 扫描起始位置
     * @return 扫描结果（count + nearestName）
     */
    private FallingScanResult scanFallingAbove(ClientLevel world, LocalPlayer player, BlockPos startPos) {
        FallingScanResult blocks = scanFallingBlocks(world, startPos);
        FallingScanResult entities = scanFallingBlockEntities(world, player);
        Component name = blocks.nearestName().getString().isBlank()
                ? entities.nearestName()
                : blocks.nearestName();
        return new FallingScanResult(Math.min(blocks.count() + entities.count(), maxFallingScanCount + 1), name);
    }

    /**
     * 向上扫描实现 {@link Fallable} 接口的方块。
     * <p>
     * 使用手动 for 循环而非 {@link BlockPos#betweenClosedStream(BlockPos, BlockPos)}，
     * 因为后者内部复用同一个 {@link BlockPos.MutableBlockPos} 对象，无法收集为列表（.toList() 中所有元素引用相同坐标）。
     *
     * @param world    当前客户端世界
     * @param startPos 扫描起始位置
     * @return 扫描结果
     */
    private FallingScanResult scanFallingBlocks(ClientLevel world, BlockPos startPos) {
        int count = 0;
        Component name = Component.empty();
        for (BlockPos pos = startPos; count < maxFallingScanCount + 1 && !world.isOutsideBuildHeight(pos); pos = pos.above()) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof Fallable) {
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
    private FallingScanResult scanFallingBlockEntities(ClientLevel world, LocalPlayer player) {
        List<Entity> entities = world.getEntities(player,
                player.getBoundingBox().inflate(0.5).setMinY(player.getEyeY()).setMaxY(player.getEyeY() + maxFallingScanCount),
                entity -> entity instanceof FallingBlockEntity);
        int count = Math.min(entities.size(), maxFallingScanCount + 1);
        Component name = entities.isEmpty() ? Component.empty() : entities.getFirst().getDisplayName();
        return new FallingScanResult(count, name);
    }

    /**
     * 获取玩家当前正在窒息碰撞的方块名称。
     * 算法源自 {@link net.minecraft.world.entity.Entity#isInWall()}。
     *
     * @param world  当前客户端世界
     * @param player 玩家
     * @return 窒息方块的名称，未窒息时返回 {@code null}
     */
    @Nullable
    private Component getSuffocatingBlockName(ClientLevel world, LocalPlayer player) {
        if (!player.isInWall()) return null;
        float f = player.getBbWidth() * SUFFOCATION_BOX_WIDTH_FACTOR;
        AABB box = AABB.ofSize(player.getEyePosition(), f, SUFFOCATION_BOX_HEIGHT, f);
        return BlockPos.betweenClosedStream(box).filter(pos -> {
            BlockState blockState = world.getBlockState(pos);
            return !blockState.isAir()
                    && blockState.isSuffocating(world, pos)
                    && Shapes.joinIsNotEmpty(
                    blockState.getCollisionShape(world, pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(box), BooleanOp.AND
            );
        }).findAny().map(pos -> world.getBlockState(pos).getBlock().getName()).orElse(null);
    }

    /**
     * 判断目标方块上方 2 格内是否存在 {@link Fallable} 类型的方块。
     * <p>
     * 同样不依赖 {@link BlockPos#betweenClosedStream(BlockPos, BlockPos)} 以避开 {@link BlockPos.MutableBlockPos} 复用陷阱。
     */
    private boolean hasFallingBlockNearTarget(ClientLevel world, BlockPos targetPos) {
        for (BlockPos pos = targetPos; pos.getY() <= targetPos.getY() + 2; pos = pos.above()) {
            if (world.getBlockState(pos).getBlock() instanceof Fallable) return true;
        }
        return false;
    }

    /**
     * 判断方块位置是否在玩家头顶（水平方向上与玩家重心距离 ≤1，Y 轴高于眼部高度）。
     */
    private static boolean isBlockAbovePlayerHead(BlockPos pos, LocalPlayer player) {
        return Math.abs(Vec3.atCenterOf(pos).x() - player.getX()) <= 1
                && pos.getY() > player.getEyeY()
                && Math.abs(Vec3.atCenterOf(pos).z() - player.getZ()) < 1;
    }

    private record FallingScanResult(int count, Component nearestName) {
    }
}
