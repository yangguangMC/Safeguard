package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;

import java.util.*;

public abstract class Detection implements SwitchTreeItem {
    private final Identifier id;
    private final Map<Action, Boolean> boundActions = new HashMap<>();    // 一般地，我们要求单个检测项注册的保护动作的ID要是不能重复的
    protected ModContext modContext;

    public Detection(Identifier id, Action... actions) {
        this.id = id;
        Set<Identifier> set = new HashSet<>();
        if (!Arrays.stream(actions).map(Action::getId).allMatch(set::add))
            throw new IllegalArgumentException("Action IDs duplicated");
        for (Action action : actions) {
            boundActions.put(action, true);
        }
    }

    protected Detection(String path, Action... actions) {
        this(Identifier.of(ModContext.MOD_ID, path), actions);
    }

    public void init(ModContext ctx) {
        modContext = ctx;
        boundActions.keySet().forEach(action -> action.init(ctx));
    }

    @Override
    public Identifier getId() {
        return id;
    }

    public Collection<Action> getBoundActions() {
        return Collections.unmodifiableSet(boundActions.keySet());
    }

    public boolean isBindingEnabled(Identifier actionId) {
        return boundActions.get(getBoundAction(actionId));
    }

    public void setBindingEnabled(Identifier actionId, boolean enabled) {
        boundActions.put(getBoundAction(actionId), enabled);
    }

    protected MutableText getName() {
        return modContext.protectionManager().getDetectionName(getId()).copy();
    }

    protected SwitchTreeNode getStateNode() {
        return Objects.requireNonNull(modContext.protectionManager().getDetectionStatesRoot().getNode(getId()));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Action> T getBoundAction(Identifier id) {
        // 因为ID在单个检测项内唯一，所以无需考虑重复情况，findAny足矣
        return (T) boundActions.keySet().stream().filter(action -> action.getId().equals(id)).findAny().orElseThrow();
    }

    protected boolean isActionEffectivelyEnabled(Action action) {
        return Objects.requireNonNull(modContext.protectionManager().getActionStatesRoot().getNode(action.getId())).isEffectivelyEnabled() && boundActions.get(action);
    }
}
