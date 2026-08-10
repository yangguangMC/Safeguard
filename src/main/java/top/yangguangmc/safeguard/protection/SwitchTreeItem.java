package top.yangguangmc.safeguard.protection;

import net.minecraft.resources.Identifier;

public interface SwitchTreeItem {
    Identifier getId();

    default boolean defaultEnabled() {
        return true;
    }
}
