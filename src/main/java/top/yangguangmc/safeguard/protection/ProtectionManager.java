package top.yangguangmc.safeguard.protection;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.detection.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);
    private final Map<Detection, Collection<Action>> protections = new HashMap<>();
    private final SwitchTreeNode detectionRoot = SwitchTreeNode.buildTree();
    private final SwitchTreeNode actionRoot = SwitchTreeNode.buildTree();
    private ModContext ctx;

    public ProtectionManager() {
        // active/afk 分类默认关闭，方便用户挂机时一键批量切换
        predefineActionCategory(new CategoryDefinition("active/afk", false));
    }

    public void init(ModContext ctx) {
        this.ctx = ctx;
        register(new AntiCreeperDetection());
        register(new AntiFallDetection());
        register(new ProjectileTrackerDetection());
        register(new AntiAmbushDetection());
        register(new AntiSuffocationDetection());
        register(new LavaDetection());
        register(new DamageDetection());
        register(new LowHealthDetection());
        register(new OnFireDetection());
        register(new LowHungerDetection());
        LOGGER.info("Protections initialized with {} detections and {} actions.", protections.size(), protections.values().stream().mapToLong(Collection::size).sum());
    }

    public void register(Detection detection) {
        protections.put(detection, detection.getBoundActions());
        SwitchTreeNode detectionNode = detectionRoot.addOrGetNode(detection.getId(), detection.defaultEnabled());
        detectionNode.addEffectiveStateListener(detection::applyActiveState);
        for (Action action : detection.getBoundActions())
            actionRoot.addOrGetNode(action.getId(), action.defaultEnabled());
        detection.init(ctx);
        detection.applyActiveState(detectionNode.isEffectivelyEnabled());
    }

    /**
     * 预定义一个检测项分类。
     * 通过预定义分类的方式，可以实现自定义默认启用状态。
     */
    public void predefineDetectionCategory(CategoryDefinition category) {
        detectionRoot.predefineCategory(category.id(), category.defaultEnabled());
    }

    /**
     * 预定义一个保护动作分类。
     * 通过预定义分类的方式，可以实现自定义默认启用状态。
     */
    public void predefineActionCategory(CategoryDefinition category) {
        actionRoot.predefineCategory(category.id(), category.defaultEnabled());
    }

    public Detection getDetection(ResourceLocation id) {
        return protections.keySet().stream().filter(detection -> detection.getId().equals(id)).findAny().orElseThrow();
    }

    public SwitchTreeNode getDetectionStatesRoot() {
        return detectionRoot.unmodifiableView();
    }

    public Component getDetectionName(ResourceLocation id) {
        return Component.translatable("detection.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }

    /**
     * 检测项某个配置项的翻译键，格式为 {@code detection.<namespace>.<path>.option.<key>}。
     * 若语言文件中存在其 {@code .details} 后缀键，内容将作为补充说明（如性能警告）显示在描述面板中。
     */
    public String getDetectionOptionTranslationKey(ResourceLocation id, String optionKey) {
        return "detection.%s.%s.option.%s".formatted(id.getNamespace(), id.getPath().replace("/", "."), optionKey);
    }

    public SwitchTreeNode getActionStatesRoot() {
        return actionRoot.unmodifiableView();
    }

    public Component getActonName(ResourceLocation id) {
        return Component.translatable("action.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }

    /**
     * 动作某个配置项的翻译键，格式为 {@code action.<namespace>.<path>.option.<key>}。
     * 若语言文件中存在其 {@code .details} 后缀键，内容将作为补充说明（如性能警告）显示在描述面板中。
     */
    public String getActionOptionTranslationKey(ResourceLocation id, String optionKey) {
        return "action.%s.%s.option.%s".formatted(id.getNamespace(), id.getPath().replace("/", "."), optionKey);
    }

    /**
     * 按动作 ID 查找所有已注册的动作实例。
     * <p>
     * 由于同一动作类可能被多个检测项各自 {@code new} 一份实例（但共享同一动作 ID 和开关树节点），
     * 动作的"全局配置项"需要在这些实例间保持一致——本方法供 {@code ConfigManager}/{@code ConfigScreen}
     * 用于扇出读写。
     * </p>
     */
    public List<Action> getActionInstances(ResourceLocation id) {
        return protections.values().stream()
                .flatMap(Collection::stream)
                .filter(action -> action.getId().equals(id))
                .toList();
    }
}
