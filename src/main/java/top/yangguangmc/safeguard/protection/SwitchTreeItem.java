package top.yangguangmc.safeguard.protection;

import net.minecraft.resources.ResourceLocation;

public interface SwitchTreeItem {
    ResourceLocation getId();

    default boolean defaultEnabled() {
        return true;
    }
}
