package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
        Text parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.inGameHud.getChatHud().addMessage(Text.translatable("messages.safeguard.prefix").append(
                Text.translatable("action.safeguard.active.afk.pause.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.setScreen(new GameMenuScreen(true));
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_PAUSE, Text.translatable("messages.safeguard.name"),
                parentName.copy().append(Text.translatable("action.safeguard.active.afk.pause.title"))));
        modContext.protectionManager().getActionStatesRoot().getNode(Identifier.of(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}
