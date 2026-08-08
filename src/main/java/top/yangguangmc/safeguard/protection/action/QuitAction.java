package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class QuitAction extends Action {
    public QuitAction() {
        super("active/afk/quit");
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public void quit(MinecraftClient client) {
        Text parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.inGameHud.getChatHud().addMessage(Text.translatable("messages.safeguard.prefix").append(
                Text.translatable("action.safeguard.active.afk.quit.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.send(() -> {
            client.getAbuseReportContext().tryShowDraftScreen(client, client.currentScreen, () -> {
                boolean singleplayer = client.isInSingleplayer();
                ServerInfo serverInfo = client.getCurrentServerEntry();
                Objects.requireNonNull(client.world).disconnect();
                if (singleplayer) client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
                else client.disconnect();
                TitleScreen titleScreen = new TitleScreen();
                if (singleplayer) client.setScreen(titleScreen);
                else if (serverInfo != null && serverInfo.isRealm())
                    client.setScreen(new RealmsMainScreen(titleScreen));
                else client.setScreen(new MultiplayerScreen(titleScreen));
            }, true);
            client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_QUIT, Text.translatable("messages.safeguard.name"),
                    parentName.copy().append(Text.translatable("action.safeguard.active.afk.quit.title"))));
        });
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        modContext.protectionManager().getActionStatesRoot().getNode(Identifier.of(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}
