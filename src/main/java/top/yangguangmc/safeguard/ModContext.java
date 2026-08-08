package top.yangguangmc.safeguard;

import net.minecraft.client.toast.SystemToast;
import top.yangguangmc.safeguard.protection.ProtectionManager;
import top.yangguangmc.safeguard.util.FilledThroughWallsRenderer;

public record ModContext(Safeguard instance,
                         ProtectionManager protectionManager,
                         ConfigManager configManager,
                         FilledThroughWallsRenderer filledThroughWallsRenderer) {
    public static final String MOD_NAME = "Safeguard";
    public static final String MOD_ID = "safeguard";
    public static final SystemToast.Type SAFEGUARD_PAUSE = new SystemToast.Type(20000);
    public static final SystemToast.Type SAFEGUARD_PAUSE_UNAVAILABLE = new SystemToast.Type(10000);
    public static final SystemToast.Type SAFEGUARD_QUIT = new SystemToast.Type(20000);
}
