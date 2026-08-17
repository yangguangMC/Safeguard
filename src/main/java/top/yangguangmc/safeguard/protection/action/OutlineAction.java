package top.yangguangmc.safeguard.protection.action;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OutlineAction extends Action {
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

    public OutlineAction() {
        super("passive/other/outline");
    }

    public void outline(Entity entity, int ticks, int color) {
        OUTLINE_TICKS.put(entity.getUUID(), ticks);
        OUTLINE_COLOR.put(entity.getUUID(), color);
    }

    public static int getOutline(UUID uuid) {
        return OUTLINE_TICKS.containsKey(uuid) ? OUTLINE_COLOR.get(uuid) : 0;
    }
}
