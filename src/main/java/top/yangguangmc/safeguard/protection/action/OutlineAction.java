package top.yangguangmc.safeguard.protection.action;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体描边高亮保护动作。
 * <p>
 * 抽象基类只负责"如何描边"（描边时长、颜色的底层实现），至于"用什么颜色"这种表现层配置，
 * 交给各检测项内部的静态子类决定——子类持有 {@link top.yangguangmc.safeguard.protection.option.ColorOption}
 * 并调用受保护的 {@link #outline(Entity, int, int)}，从而符合"检测项只管如何检测，保护动作只管如何处理"的原则。
 * </p>
 */
public abstract class OutlineAction extends Action {
    private static final Map<UUID, Integer> OUTLINE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> OUTLINE_COLOR = new ConcurrentHashMap<>();

    static {
        ClientTickEvents.END_LEVEL_TICK.register(world -> {
            for (UUID uuid : OUTLINE_TICKS.keySet()) {
                if (world.getEntity(uuid) == null) {
                    OUTLINE_TICKS.remove(uuid);
                    OUTLINE_COLOR.remove(uuid);
                    continue;
                }
                int ticks = OUTLINE_TICKS.get(uuid);
                if (ticks > 0) {
                    OUTLINE_TICKS.put(uuid, ticks - 1);
                } else {
                    OUTLINE_TICKS.remove(uuid);
                    OUTLINE_COLOR.remove(uuid);
                }
            }
        });
    }

    protected OutlineAction() {
        super("passive/other/outline");
    }

    protected void outline(Entity entity, int ticks, int color) {
        OUTLINE_TICKS.put(entity.getUUID(), ticks);
        OUTLINE_COLOR.put(entity.getUUID(), color);
    }

    public static int getOutline(UUID uuid) {
        return OUTLINE_TICKS.containsKey(uuid) ? OUTLINE_COLOR.get(uuid) : 0;
    }
}
