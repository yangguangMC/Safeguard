package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.ModContext;

public class QuitAction extends Action {
    public QuitAction() {
        super("active/afk/quit");
    }

    public void quit(MinecraftClient client, ClientWorld world, MutableText moduleName) {
        boolean single = client.isInSingleplayer();
        ServerInfo serverInfo = client.getCurrentServerEntry();
        world.disconnect();
        if (single) client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        else client.disconnect();
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_QUIT, Text.translatable("messages.safeguard.name"), moduleName.append(Text.literal(" 检测到危险，已自动退出游戏！"))));
        TitleScreen titleScreen = new TitleScreen();
        if (single) client.setScreen(titleScreen);
        else if (serverInfo != null && serverInfo.isRealm()) client.setScreen(new RealmsMainScreen(titleScreen));
        else client.setScreen(new MultiplayerScreen(titleScreen));
        getStateNode().setEnabled(false);
    }
}
