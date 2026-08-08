package top.yangguangmc.safeguard.protection.action;

import net.minecraft.util.math.BlockPos;
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

    public void outline(Collection<BlockPos> positions, int colorArgb) {
        String tag = getParent().getId().toString();
        modContext.filledThroughWallsRenderer().clearByTag(tag);
        for (BlockPos pos : positions) {
            modContext.filledThroughWallsRenderer().addBox(tag, pos.getX(), pos.getY(), pos.getZ(), colorArgb);
        }
    }
}
