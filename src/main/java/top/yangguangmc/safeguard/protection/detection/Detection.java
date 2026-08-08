package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.event.GatedEvent;

import java.util.*;
import java.util.function.Consumer;

public abstract class Detection implements SwitchTreeItem {
    private final Identifier id;
    private final Map<Action, Boolean> boundActions = new HashMap<>();
    private final List<GatedEvent<?>> gatedEvents = new ArrayList<>();
    protected ModContext modContext;

    public Detection(Identifier id, Action... actions) {
        this.id = id;
        Set<Identifier> set = new HashSet<>();
        if (!Arrays.stream(actions).map(Action::getId).allMatch(set::add))
            throw new IllegalArgumentException("Action IDs duplicated");
        for (Action action : actions) {
            action.initParent(this);
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
    public Identifier getId() { return id; }

    public Collection<Action> getBoundActions() {
        return Collections.unmodifiableSet(boundActions.keySet());
    }

    public boolean isBindingEnabled(Identifier actionId) {
        return boundActions.get(getBoundAction(actionId));
    }

    public void setBindingEnabled(Identifier actionId, boolean enabled) {
        boundActions.put(getBoundAction(actionId), enabled);
    }

    protected SwitchTreeNode getStateNode() {
        return Objects.requireNonNull(modContext.protectionManager().getDetectionStatesRoot().getNode(getId()));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Action> T getBoundAction(Identifier id) {
        return (T) boundActions.keySet().stream().filter(action -> action.getId().equals(id)).findAny().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    protected <T extends Action> T getBoundAction(Class<T> actionClass) {
        return (T) boundActions.keySet().stream().filter(actionClass::isInstance).findAny().orElseThrow();
    }

    protected boolean isActionEffectivelyEnabled(Action action) {
        return Objects.requireNonNull(modContext.protectionManager().getActionStatesRoot().getNode(action.getId())).isEffectivelyEnabled() && boundActions.get(action);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> void listen(GatedEvent<T> event, T listener) {
        gatedEvents.add(event);
        event.listen(this, listener);
        event.suspend(this);
    }

    protected <T extends Action> void tryExecuteAction(Class<T> actionClass, Consumer<T> executor) {
        T action = getBoundAction(actionClass);
        if (isActionEffectivelyEnabled(action)) executor.accept(action);
    }

    public void applyActiveState(boolean active) {
        for (GatedEvent<?> event : gatedEvents) {
            if (active) event.resume(this);
            else event.suspend(this);
        }
    }
}
