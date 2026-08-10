package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.yangguangmc.safeguard.ModContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class QuitAction extends Action {
    public QuitAction() {
        super("active/afk/quit");
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public void quit(Minecraft client) {
        Component parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.gui.getChat().addMessage(Component.translatable("messages.safeguard.prefix").append(
                Component.translatable("action.safeguard.active.afk.quit.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.schedule(() -> {
            client.getReportingContext().draftReportHandled(client, null, () -> client.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
            client.getToastManager().addToast(new SystemToast(ModContext.SAFEGUARD_QUIT, Component.translatable("messages.safeguard.name"),
                    parentName.copy().append(Component.translatable("action.safeguard.active.afk.quit.title"))));
        });
        client.getSoundManager().play(createSoundInstance(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        modContext.protectionManager().getActionStatesRoot().getNode(Identifier.fromNamespaceAndPath(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}