package top.yangguangmc.safeguard.protection;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.detection.AntiCreeperDetection;
import top.yangguangmc.safeguard.protection.detection.Detection;

public class ProtectionManager {
    private final SwitchTreeNode detectionRoot = SwitchTreeNode.buildTree();
    private final SwitchTreeNode actionRoot = SwitchTreeNode.buildTree();
    private ModContext ctx;

    public void init(ModContext ctx) {
        this.ctx = ctx;
        register(new AntiCreeperDetection());
    }

    public void register(Detection detection) {

        detectionRoot.addOrGetNode(detection.getId()).setEnabled(detection.isEnabledByDefault());
        detection.getBoundActions().forEach(action -> actionRoot.addOrGetNode(action.getId()).setEnabled(action.isEnabledByDefault()));
        detection.init(ctx);
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
