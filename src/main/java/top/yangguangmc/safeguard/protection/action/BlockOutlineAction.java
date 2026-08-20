package top.yangguangmc.safeguard.protection.action;

import net.minecraft.core.BlockPos;

import java.util.Collection;

/**
 * 方块高亮（穿墙描边）保护动作。
 * <p>
 * 抽象基类只负责"如何高亮"，至于"用什么颜色"这种表现层配置，交给各检测项内部的静态子类决定——
 * 子类持有 {@link top.yangguangmc.safeguard.protection.option.ColorOption} 并调用受保护的
 * {@link #outline(Collection, int)}，从而符合"检测项只管如何检测，保护动作只管如何处理"的原则。
 * </p>
 */
public abstract class BlockOutlineAction extends Action {
    protected BlockOutlineAction() {
        super("passive/other/block_outline");
    }

    /**
     * 高亮显示指定的方块位置。
     *
     * @param positions 需要高亮的方块位置
     * @param colorArgb 颜色（ARGB 格式，如 {@code 0xFFFF4500}）
     */
    protected void outline(Collection<BlockPos> positions, int colorArgb) {
        String tag = getParent().getId().toString();
        modContext.filledThroughWallsRenderer().clearByTag(tag);
        for (BlockPos pos : positions) {
            modContext.filledThroughWallsRenderer().addBox(tag, pos.getX(), pos.getY(), pos.getZ(), colorArgb);
        }
    }
}