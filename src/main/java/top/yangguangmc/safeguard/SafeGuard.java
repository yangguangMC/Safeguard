package top.yangguangmc.safeguard;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.protection.ProtectionManager;


public final class SafeGuard implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);

    @Override
    public void onInitializeClient() {
        ProtectionManager protectionManager = new ProtectionManager();
        ModContext ctx = new ModContext(this, protectionManager);
        protectionManager.init(ctx);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SafeGuardCommand.register(dispatcher, ctx));
        LOGGER.debug("Debug logging has been enabled.");
        LOGGER.info("Initialized successfully.");
    }
}
