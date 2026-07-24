package top.yangguangmc.safeguard.protection.event;

import net.fabricmc.fabric.api.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 门控事件 — 在 Fabric {@link Event} 之上添加"按所有者挂起/恢复"的能力。
 * <p>
 * 每创建一个 {@code GatedEvent}，就在对应的 Fabric Event 上注册恰好一个总监听器。
 * 总监听器在触发时，仅向当前未被挂起的所有者转发事件。
 * </p>
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 在事件声明处：
 * public static final GatedEvent<StartTick> GATED_START_TICK = new GatedEvent<>(
 *     START_TICK,
 *     active -> (client, world, player) -> {
 *         for (StartTick cb : active.get()) cb.onStartTick(client, world, player);
 *     }
 * );
 *
 * // 在 Detection 子类中：
 * listen(GATED_START_TICK, this::onStartTick);
 * }</pre>
 * </p>
 * <p>
 * 这个类是线程安全的。
 * </p>
 *
 * @param <T> Fabric 事件的监听器类型（函数式接口）
 */
public class GatedEvent<T> {

    private final Map<Object, List<T>> listeners = new ConcurrentHashMap<>();
    private final Set<Object> suspended = ConcurrentHashMap.newKeySet();

    /**
     * 构造一个门控事件，在指定 Fabric Event 上注册总监听器。
     *
     * @param fabricEvent 要包装的 Fabric Event
     * @param gateFactory 门控工厂。
     *                    接受 {@code Supplier<List<T>>}（每次调用返回当前活跃监听器的实时列表），
     *                    返回一个 {@code T} 实例。
     *                    当该实例的方法被调用时，会从 Supplier 获取实时列表并迭代。
     */
    public GatedEvent(Event<T> fabricEvent, Function<Supplier<List<T>>, T> gateFactory) {
        fabricEvent.register(gateFactory.apply(this::getActiveListeners));
    }

    /**
     * 注册监听器，绑定到指定所有者。
     *
     * @param owner    监听器的所有者，通常为 {@code this}
     * @param listener 监听器实例
     */
    public void listen(Object owner, T listener) {
        listeners.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    /**
     * 挂起指定所有者的全部监听器。
     * 挂起后，该所有者的监听器不会收到事件。
     *
     * @param owner 要挂起的所有者
     */
    public void suspend(Object owner) {
        suspended.add(owner);
    }

    /**
     * 恢复指定所有者的全部监听器。
     *
     * @param owner 要恢复的所有者
     */
    public void resume(Object owner) {
        suspended.remove(owner);
    }

    /**
     * 获取当前活跃（未被挂起）的所有者的全部监听器列表。
     */
    private List<T> getActiveListeners() {
        return listeners.entrySet().stream()
                .filter(e -> !suspended.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }
}
