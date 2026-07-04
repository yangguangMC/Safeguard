package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.function.UnaryOperator;

public class ActionBarTitleAction extends Action {
    public ActionBarTitleAction() {
        super("passive/hud/action_bar_title");
    }

    public void updateTitle(MinecraftClient client, double distance, UnaryOperator<Style> styleProvider) {
        client.inGameHud.setOverlayMessage(Text.literal("苦力怕距离你%.1f方块".formatted(distance)).styled(styleProvider), false);
    }
}
