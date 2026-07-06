package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.ModContext;

public class PauseAction extends Action {

    public PauseAction() {
        super("active/afk/pause");
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    public void pause(MinecraftClient client, MutableText moduleName) {
        if (!client.isInSingleplayer()) {
            client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_PAUSE_UNAVAILABLE, Text.translatable("messages.safeguard.name"), Text.literal("检测到非单人游戏，暂停不可用！")));
            getStateNode().setEnabled(false);
            return;
        }
        client.setScreen(new GameMenuScreen(true));
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_PAUSE, Text.translatable("messages.safeguard.name"), moduleName.append(Text.literal(" 检测到危险，已自动暂停游戏！"))));
        getStateNode().setEnabled(false);
    }
}
