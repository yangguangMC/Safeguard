package top.yangguangmc.safeguard.protection;

import net.minecraft.util.Identifier;

public interface SwitchTreeItem {
    Identifier getId();

    default boolean isEnabledByDefault() {
        return true;
    }
}
