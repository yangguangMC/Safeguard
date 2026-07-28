package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

public class LowHealthDetection extends Detection {
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private float maxRateThreshold = 0.3F;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private float minRateThreshold = 0.1F;

    public LowHealthDetection() {
        super("status/low_health", new RedVignetteAction(), new PauseAction(), new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        float killLine = 4;
        float maxHealthThreshold = MathHelper.clamp(player.getMaxHealth() * maxRateThreshold, 10, 20);
        float minHealthThreshold = MathHelper.clamp(player.getMaxHealth() * minRateThreshold, killLine, 10);
        float delta;
        if (player.getHealth() < minHealthThreshold) delta = 1F;
        else if (player.getHealth() > maxHealthThreshold) delta = 0F;
        else delta = (maxHealthThreshold - player.getHealth()) / (maxHealthThreshold - minHealthThreshold);
        tryExecuteAction(RedVignetteAction.class, action -> action.setProgress(delta));
        if (player.getHealth() < maxHealthThreshold) {
            tryExecuteAction(PauseAction.class, action -> action.pause(client));
            tryExecuteAction(QuitAction.class, action -> action.quit(client));
        }
    }
}
