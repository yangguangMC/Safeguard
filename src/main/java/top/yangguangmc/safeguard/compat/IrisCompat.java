package top.yangguangmc.safeguard.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.util.FilledThroughWallsRenderer;

/**
 * Iris 光影兼容层。
 * <p>
 * 通过反射调用 Iris API 将自定义 {@link com.mojang.blaze3d.pipeline.RenderPipeline} 映射到 Iris 内置程序类型，
 * 使光影能正确理解自定义管线的渲染语义。
 * <p>
 * 若 Iris 不在运行时环境中，所有方法静默跳过。
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
            LOGGER.info("Iris detected — registering custom pipelines for shader compatibility.");
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
     * 将自定义 pipeline 注册到 Iris。
     * <p>
     * 应在模组初始化末尾调用。若 Iris 未安装，静默跳过。
     */
    public static void registerPipelines() {
        if (!IRIS_LOADED) {
            return;
        }

        try {
            // 通过反射调用 Iris API，避免编译期硬依赖
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);

            Class<?> irisProgramClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");

            // IrisProgram.BASIC = "basic"
            Object basicProgram = irisProgramClass.getField("BASIC").get(null);

            // 注册 FILLED_THROUGH_WALLS pipeline
            irisApiClass.getMethod("assignPipeline",
                            com.mojang.blaze3d.pipeline.RenderPipeline.class,
                            irisProgramClass)
                    .invoke(irisApiInstance,
                            FilledThroughWallsRenderer.FILLED_THROUGH_WALLS,
                            basicProgram);

            LOGGER.info("Registered '{}' pipeline to Iris BASIC program.",
                    FilledThroughWallsRenderer.FILLED_THROUGH_WALLS.getLocation());
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to register pipelines with Iris: {}", e.getMessage());
        }
    }
}