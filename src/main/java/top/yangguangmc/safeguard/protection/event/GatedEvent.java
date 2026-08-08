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
 */
public class GatedEvent<T> {

    private final Map<Object, List<T>> listeners = new ConcurrentHashMap<>();
    private final Set<Object> suspended = ConcurrentHashMap.newKeySet();

    public GatedEvent(Event<T> fabricEvent, Function<Supplier<List<T>>, T> gateFactory) {
        fabricEvent.register(gateFactory.apply(this::getActiveListeners));
    }

    public void listen(Object owner, T listener) {
        listeners.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    public void suspend(Object owner) {
        suspended.add(owner);
    }

    public void resume(Object owner) {
        suspended.remove(owner);
    }

    private List<T> getActiveListeners() {
        return listeners.entrySet().stream()
                .filter(e -> !suspended.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }
}
