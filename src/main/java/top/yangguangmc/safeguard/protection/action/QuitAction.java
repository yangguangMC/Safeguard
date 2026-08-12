package top.yangguangmc.safeguard.protection.action;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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

    public void quit(Minecraft client) {
        Component parentName = modContext.protectionManager().getDetectionName(getParent().getId());
        client.gui.getChat().addMessage(Component.translatable("messages.safeguard.prefix").append(
                Component.translatable("action.safeguard.active.afk.quit.chat_message", parentName,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                )
        ));
        client.tell(() -> {
            client.getReportingContext().draftReportHandled(client, client.screen, () -> {
                boolean singleplayer = client.isLocalServer();
                ServerData serverInfo = client.getCurrentServer();
                Objects.requireNonNull(client.level).disconnect();
                if (singleplayer) client.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
                else client.disconnect();
                TitleScreen titleScreen = new TitleScreen();
                if (singleplayer) client.setScreen(titleScreen);
                else if (serverInfo != null && serverInfo.isRealm())
                    client.setScreen(new RealmsMainScreen(titleScreen));
                else client.setScreen(new JoinMultiplayerScreen(titleScreen));
            }, true);
            client.getToasts().addToast(new SystemToast(ModContext.SAFEGUARD_QUIT, Component.translatable("messages.safeguard.name"),
                    parentName.copy().append(Component.translatable("action.safeguard.active.afk.quit.title"))));
        });
        client.getSoundManager().play(createSoundInstance(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        modContext.protectionManager().getActionStatesRoot().getNode(ResourceLocation.tryBuild(ModContext.MOD_ID, "active/afk")).setEnabled(false);
    }
}
