package top.yangguangmc.safeguard.protection;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
        // 预定义默认关闭的分类
        // 递归创建路径上的所有 category 都会使用传入的 defaultEnabled？这不是我们通常想要的。只能预定义其前一节点。
        predefineActionCategory(new CategoryDefinition("active", true));
        predefineActionCategory(new CategoryDefinition("active/afk", false));
    }

    public void init(ModContext ctx) {
        this.ctx = ctx;
        register(new AntiCreeperDetection());
        register(new AntiFallDetection());
        register(new ProjectileTrackerDetection());
        register(new AntiAmbushDetection());
        register(new AntiSuffocationDetection());
        LOGGER.info("Protections initialized with {} detections and {} actions.", protections.size(), protections.values().stream().mapToLong(Collection::size).sum());
    }

    public void register(Detection detection) {
        protections.put(detection, detection.getBoundActions());
        SwitchTreeNode detectionNode = detectionRoot.addOrGetNode(detection.getId());
        detectionNode.addEffectiveStateListener(detection::applyActiveState);
        for (Action action : detection.getBoundActions()) actionRoot.addOrGetNode(action.getId());
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

    public Detection getDetection(Identifier id) {
        return protections.keySet().stream().filter(detection -> detection.getId().equals(id)).findAny().orElseThrow();
    }

    public SwitchTreeNode getDetectionStatesRoot() {
        return detectionRoot.unmodifiableView();
    }

    public Text getDetectionName(Identifier id) {
        return Text.translatable("detection.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }

    public SwitchTreeNode getActionStatesRoot() {
        return actionRoot.unmodifiableView();
    }

    public Text getActonName(Identifier id) {
        return Text.translatable("action.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }
}