package top.yangguangmc.safeguard;

import net.minecraft.client.gui.components.toasts.SystemToast;
import top.yangguangmc.safeguard.protection.ProtectionManager;
import top.yangguangmc.safeguard.util.FilledThroughWallsRenderer;

public record ModContext(Safeguard instance,
                         ProtectionManager protectionManager,
                         ConfigManager configManager,
                         FilledThroughWallsRenderer filledThroughWallsRenderer) {
    public static final String MOD_NAME = "Safeguard";
    public static final String MOD_ID = "safeguard";
    public static final SystemToast.SystemToastId SAFEGUARD_PAUSE = new SystemToast.SystemToastId(20000);
    public static final SystemToast.SystemToastId SAFEGUARD_PAUSE_UNAVAILABLE = new SystemToast.SystemToastId(10000);
    public static final SystemToast.SystemToastId SAFEGUARD_QUIT = new SystemToast.SystemToastId(20000);
}
