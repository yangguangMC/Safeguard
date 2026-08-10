package top.yangguangmc.safeguard.protection.action;

import net.minecraft.core.BlockPos;
import top.yangguangmc.safeguard.ModContext;

import java.util.Collection;

public class BlockOutlineAction extends Action {
    public BlockOutlineAction() {
        super("passive/other/block_outline");
    }

    @Override
    public void init(ModContext ctx) {
        super.init(ctx);
    }

    /**
     * 高亮显示指定的方块位置。
     *
     * @param positions 需要高亮的方块位置
     * @param colorArgb 颜色（ARGB 格式，如 {@code 0xFFFF4500}）
     */
    public void outline(Collection<BlockPos> positions, int colorArgb) {
        String tag = getParent().getId().toString();
        modContext.filledThroughWallsRenderer().clearByTag(tag);
        for (BlockPos pos : positions) {
            modContext.filledThroughWallsRenderer().addBox(tag, pos.getX(), pos.getY(), pos.getZ(), colorArgb);
        }
    }
}
