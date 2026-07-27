package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.ModContext;

public class QuitAction extends Action {
    public QuitAction() {
        super("active/afk/quit");
    }

    public void quit(MinecraftClient client) {
        client.send(() -> client.getAbuseReportContext().tryShowDraftScreen(client, null, () -> client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT), true));
        client.getSoundManager().play(createSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
        client.getToastManager().add(new SystemToast(ModContext.SAFEGUARD_QUIT, Text.translatable("messages.safeguard.name"),
                modContext.protectionManager().getDetectionName(getParent().getId()).copy().append(Text.literal(" 检测到危险，已自动退出游戏！"))));
        getStateNode().setEnabled(false);
    }
}