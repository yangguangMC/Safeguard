package top.yangguangmc.safeguard;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.gui.screen.ConfigScreen;
import top.yangguangmc.safeguard.protection.ProtectionManager;


public final class SafeGuard implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.debug("Debug logging has been enabled.");
        ProtectionManager protectionManager = new ProtectionManager();
        ConfigManager configManager = new ConfigManager();
        ModContext ctx = new ModContext(this, protectionManager, configManager);
        configManager.init(ctx);
        protectionManager.init(ctx);
        configManager.tryLoad();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.trySave());
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> SafeGuardCommand.register(dispatcher, ctx)
        );
        ConfigScreen.init(ctx);
        LOGGER.info("Initialized successfully.");
    }
}
