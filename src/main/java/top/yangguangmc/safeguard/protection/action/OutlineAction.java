package top.yangguangmc.safeguard.protection.action;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OutlineAction extends Action {
    private static final Map<Integer, Integer> OUTLINE_TICKS = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> OUTLINE_COLOR = new ConcurrentHashMap<>();

    static {
        ClientTickEvents.END_WORLD_TICK.register(world -> {
            for (int id : OUTLINE_TICKS.keySet()) {
                if (world.getEntityById(id) == null) {
                    OUTLINE_TICKS.remove(id);
                    OUTLINE_COLOR.remove(id);
                    continue;
                }
                int ticks = OUTLINE_TICKS.get(id);
                if (ticks > 0) {
                    OUTLINE_TICKS.put(id, ticks - 1);
                } else {
                    OUTLINE_TICKS.remove(id);
                    OUTLINE_COLOR.remove(id);
                }
            }
        });
    }

    public OutlineAction() {
        super("passive/other/outline");
    }

    public void outline(Entity entity, int ticks, int color) {
        OUTLINE_TICKS.put(entity.getId(), ticks);
        OUTLINE_COLOR.put(entity.getId(), color);
    }

    public static int getOutline(Entity entity) {
        return OUTLINE_TICKS.containsKey(entity.getId()) ? OUTLINE_COLOR.get(entity.getId()) : 0;
    }
}
