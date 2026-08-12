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

    public SwitchTreeNode getActionStatesRoot() {
        return actionRoot.unmodifiableView();
    }

    public Component getActonName(ResourceLocation id) {
        return Component.translatable("action.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }
}
