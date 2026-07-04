package top.yangguangmc.safeguard;

import net.minecraft.client.toast.SystemToast;
import top.yangguangmc.safeguard.protection.ProtectionManager;

public record ModContext(SafeGuard instance, ProtectionManager protectionManager) {
    public static final String MOD_NAME = "SafeGuard";
    public static final String MOD_ID = "safeguard";
    public static final SystemToast.Type SAFEGUARD_PAUSE = new SystemToast.Type(20000);
    public static final SystemToast.Type SAFEGUARD_PAUSE_UNAVAILABLE = new SystemToast.Type(10000);
    public static final SystemToast.Type SAFEGUARD_QUIT = new SystemToast.Type(20000);
}
