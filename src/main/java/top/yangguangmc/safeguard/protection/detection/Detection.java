package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.resources.ResourceLocation;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeItem;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.event.GatedEvent;

import java.util.*;
import java.util.function.Consumer;

public abstract class Detection implements SwitchTreeItem {
    private final ResourceLocation id;
    private final Map<Action, Boolean> boundActions = new HashMap<>();  // 一般地，我们要求单个检测项注册的保护动作的ID要是不能重复的
    private final List<GatedEvent<?>> gatedEvents = new ArrayList<>();
    protected ModContext modContext;

    public Detection(ResourceLocation id, Action... actions) {
        this.id = id;
        Set<ResourceLocation> set = new HashSet<>();
        if (!Arrays.stream(actions).map(Action::getId).allMatch(set::add))
            throw new IllegalArgumentException("Action IDs duplicated");
        for (Action action : actions) {
            action.initParent(this);
            boundActions.put(action, true);
        }
    }

    protected Detection(String path, Action... actions) {
        this(ResourceLocation.tryBuild(ModContext.MOD_ID, path), actions);
    }

    public void init(ModContext ctx) {
        modContext = ctx;
        boundActions.keySet().forEach(action -> action.init(ctx));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public Collection<Action> getBoundActions() {
        return Collections.unmodifiableSet(boundActions.keySet());
    }

    public boolean isBindingEnabled(ResourceLocation actionId) {
        return boundActions.get(getBoundAction(actionId));
    }

    public void setBindingEnabled(ResourceLocation actionId, boolean enabled) {
        boundActions.put(getBoundAction(actionId), enabled);
    }

    protected SwitchTreeNode getStateNode() {
        return Objects.requireNonNull(modContext.protectionManager().getDetectionStatesRoot().getNode(getId()));
    }

    @SuppressWarnings("unchecked")
    protected <T extends Action> T getBoundAction(ResourceLocation id) {
        return (T) boundActions.keySet().stream().filter(action -> action.getId().equals(id)).findAny().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    protected <T extends Action> T getBoundAction(Class<T> actionClass) {
        return (T) boundActions.keySet().stream().filter(actionClass::isInstance).findAny().orElseThrow();
    }

    protected boolean isActionEffectivelyEnabled(Action action) {
        return Objects.requireNonNull(modContext.protectionManager().getActionStatesRoot().getNode(action.getId())).isEffectivelyEnabled() && boundActions.get(action);
    }

    /**
     * 声明此检测项监听某个门控事件。
     * 当检测项的有效启用状态变更时，基类自动挂起/恢复事件传递。
     * 通常在构造器或 init() 中调用。
     *
     * @param event    门控事件
     * @param listener 监听器实例（通常为方法引用，如 {@code this::onStartTick}）
     * @param <T>      监听器类型
     */
    @SuppressWarnings("SameParameterValue")
    protected <T> void listen(GatedEvent<T> event, T listener) {
        gatedEvents.add(event);
        event.listen(this, listener);
        // 初始挂起，等待 ProtectionManager 调用 applyActiveState() 激活
        event.suspend(this);
    }

    /**
     * 尝试执行一个保护动作。
     * 自动检查双重开关（树开关 + 绑定开关），仅当两者均通过时才执行。
     *
     * @param actionClass 动作的类型
     * @param executor    要执行的逻辑（接受 Action 实例作为参数）
     * @param <T>         动作的具体类型
     */
    protected <T extends Action> void tryExecuteAction(Class<T> actionClass, Consumer<T> executor) {
        T action = getBoundAction(actionClass);
        if (isActionEffectivelyEnabled(action)) executor.accept(action);
    }

    /**
     * 由 ProtectionManager 调用，更新所有门控事件的挂起/恢复状态。
     */
    public void applyActiveState(boolean active) {
        for (GatedEvent<?> event : gatedEvents) {
            if (active) event.resume(this);
            else event.suspend(this);
        }
    }
}
