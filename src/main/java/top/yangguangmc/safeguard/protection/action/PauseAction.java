package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.ModContext;

public class PauseAction extends Action {

    public PauseAction() {
        super("active/afk/pause");
    }

    public void pause(MinecraftClient client) {
        if (!client.isInSingleplayer()) {
            client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_PAUSE_UNAVAILABLE, Text.translatable("messages.safeguard.name"), Text.translatable("action.safeguard.active.afk.pause.unavailable")));
            getStateNode().setEnabled(false);
            return;
        }
        client.setScreen(new GameMenuScreen(true));
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_PAUSE, Text.translatable("messages.safeguard.name"),
                modContext.protectionManager().getDetectionName(getParent().getId()).copy().append(Text.translatable("action.safeguard.active.afk.pause.title"))));
        getStateNode().setEnabled(false);
    }
}