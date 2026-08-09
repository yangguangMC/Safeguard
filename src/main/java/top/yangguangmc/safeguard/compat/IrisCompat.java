package top.yangguangmc.safeguard.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.ModContext;

/**
 * Iris 光影兼容层。
 * 1.20.6: Iris compatibility simplified — the 1.20.6 version of this renderer
 * uses the standard RenderLayer system which Iris already understands,
 * so explicit pipeline registration is not needed.
 */
public final class IrisCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);

    private static final boolean IRIS_LOADED;

    static {
        boolean loaded = false;
        try {
            loaded = FabricLoader.getInstance().isModLoaded("iris");
        } catch (Exception ignored) {
            // 如果 FabricLoader 不可用（理论上不会），保持 false
        }
        IRIS_LOADED = loaded;
        if (IRIS_LOADED) {
            LOGGER.info("Iris detected — compatibility mode active.");
        }
    }

    private IrisCompat() {
        throw new AssertionError("工具类不应实例化");
    }

    /**
     * 判断当前是否安装了 Iris。
     */
    public static boolean isIrisLoaded() {
        return IRIS_LOADED;
    }

    /**
     * 1.20.6: No custom RenderPipeline to register — the standard RenderLayer
     * system is used instead, which Iris handles natively.
     */
    public static void registerPipelines() {
        if (!IRIS_LOADED) return;
        LOGGER.info("Iris compatibility initialized (standard RenderLayer mode).");
    }
}
