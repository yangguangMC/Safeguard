package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.action.RedVignetteAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.protection.option.DoubleOption;

public class LowHealthDetection extends Detection {
    private final DoubleOption maxRateThreshold = registerOption(DoubleOption.of("maxRateThreshold", 0.3).range(0, 1).percent());
    private final DoubleOption minRateThreshold = registerOption(DoubleOption.of("minRateThreshold", 0.1).range(0, 1).percent());

    public LowHealthDetection() {
        super("status/low_health", new RedVignetteAction(), new PauseAction(), new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        float killLine = 4;
        float maxHealthThreshold = Mth.clamp((float) (player.getMaxHealth() * maxRateThreshold.get()), 10, 20);
        float minHealthThreshold = Mth.clamp((float) (player.getMaxHealth() * minRateThreshold.get()), killLine, 10);
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
