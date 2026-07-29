package top.yangguangmc.safeguard;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.gui.screen.ConfigScreen;
import top.yangguangmc.safeguard.protection.ProtectionManager;
import top.yangguangmc.safeguard.util.FilledThroughWallsRenderer;


public final class Safeguard implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.debug("Debug logging has been enabled.");
        ProtectionManager protectionManager = new ProtectionManager();
        ConfigManager configManager = new ConfigManager();
        FilledThroughWallsRenderer filledThroughWallsRenderer = new FilledThroughWallsRenderer();
        ModContext ctx = new ModContext(this, protectionManager, configManager, filledThroughWallsRenderer);
        configManager.init(ctx);
        protectionManager.init(ctx);
        configManager.tryLoad();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.trySave());
        ConfigScreen.init(ctx);
        SafeguardCommand.init(ctx);
        filledThroughWallsRenderer.init();
        LOGGER.info("Initialized successfully.");
    }
}