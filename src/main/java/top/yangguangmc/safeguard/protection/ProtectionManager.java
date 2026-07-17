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

    public void init(ModContext ctx) {
        this.ctx = ctx;
        register(new AntiCreeperDetection());
        register(new AntiFallDetection());
        register(new ProjectileTrackerDetection());
        register(new AntiAmbushDetection());
        LOGGER.info("Protections initialized with {} detections and {} actions.", protections.size(), protections.values().stream().mapToLong(Collection::size).sum());
    }

    public void register(Detection detection) {
        protections.put(detection, detection.getBoundActions());
        detectionRoot.addOrGetNode(detection.getId()).setEnabled(detection.isEnabledByDefault());
        detection.getBoundActions().forEach(action -> actionRoot.addOrGetNode(action.getId()).setEnabled(action.isEnabledByDefault()));
        detection.init(ctx);
    }

    public Detection getDetection(Identifier id) {
        return protections.keySet().stream().filter(detection -> detection.getId().equals(id)).findAny().orElseThrow();
    }

    public boolean getDetectionDefaultState(Identifier id) {
        return getDetection(id).isEnabledByDefault();
    }

    public SwitchTreeNode getDetectionStatesRoot() {
        return detectionRoot.unmodifiableView();
    }

    public Text getDetectionName(Identifier id) {
        return Text.translatable("detection.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }

    public boolean getActionDefaultState(Identifier id) {
        // 一般地，要求同一个ID对应的所有Action的isEnabledByDefault是一模一样的，所以使用findAny()
        return protections.values().stream().flatMap(Collection::stream).filter(action -> action.getId().equals(id)).findAny().orElseThrow().isEnabledByDefault();
    }

    public SwitchTreeNode getActionStatesRoot() {
        return actionRoot.unmodifiableView();
    }

    public Text getActonName(Identifier id) {
        return Text.translatable("action.%s.%s".formatted(id.getNamespace(), id.getPath().replace("/", ".")));
    }
}
