package top.yangguangmc.safeguard.protection.action;

import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;

import java.util.Objects;

public abstract class Action implements SwitchTreeItem {
    private final Identifier id;
    protected ModContext modContext;

    public Action(String path) {
        id = new Identifier(ModContext.MOD_ID, path);
    }

    @Override
    public Identifier getId() {
        return id;
    }

    public void init(ModContext ctx) {
        modContext = ctx;
    }

    protected SwitchTreeNode getStateNode() {
        return Objects.requireNonNull(modContext.protectionManager().getActionStatesRoot().getNode(getId()));
    }
}
