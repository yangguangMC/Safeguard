package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class QuitAction extends Action {
    public QuitAction() {
        super("active/afk/quit");
    }

    public void quit(MinecraftClient client) {
        Text parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.inGameHud.getChatHud().addMessage(Text.translatable("messages.safeguard.prefix").append(
                Text.translatable("action.safeguard.active.afk.quit.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.send(() -> {
            client.getAbuseReportContext().tryShowDraftScreen(client, null, () -> client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT), true);
            client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_QUIT, Text.translatable("messages.safeguard.name"),
                    parentName.copy().append(Text.translatable("action.safeguard.active.afk.quit.title"))));
        });
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        modContext.protectionManager().getActionStatesRoot().getNode(Identifier.of(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}