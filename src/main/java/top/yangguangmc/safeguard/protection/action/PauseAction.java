package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import top.yangguangmc.safeguard.ModContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PauseAction extends Action {
    public PauseAction() {
        super("active/afk/pause");
    }

    public void pause(Minecraft client) {
        if (!client.isLocalServer()) {
            client.getToasts().addToast(new SystemToast(ModContext.SAFEGUARD_PAUSE_UNAVAILABLE, Component.translatable("messages.safeguard.name"), Component.translatable("action.safeguard.active.afk.pause.unavailable")));
            getStateNode().setEnabled(false);
            return;
        }
        Component parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.gui.getChat().addMessage(Component.translatable("messages.safeguard.prefix").append(
                Component.translatable("action.safeguard.active.afk.pause.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.setScreen(new PauseScreen(true));
        client.getSoundManager().play(createSoundInstance(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToasts().addToast(new SystemToast(ModContext.SAFEGUARD_PAUSE, Component.translatable("messages.safeguard.name"),
                parentName.copy().append(Component.translatable("action.safeguard.active.afk.pause.title"))));
        modContext.protectionManager().getActionStatesRoot().getNode(ResourceLocation.tryBuild(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}
