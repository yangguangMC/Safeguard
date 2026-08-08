package top.yangguangmc.safeguard.protection.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ActionBarTitleAction extends Action {

    private static DangerLevel currentLevel = DangerLevel.LOW;
    @SuppressWarnings("unused")
    private static Text currentRawMessage;

    static {
        currentRawMessage = Text.empty();
    }

    public ActionBarTitleAction() {
        super("passive/hud/action_bar_title");
    }

    public void updateTitle(@NotNull DangerLevel level, @NotNull Text rawText, @NotNull MinecraftClient client) {
        if (level.compareTo(currentLevel) > 0) return;
        currentLevel = level;
        currentRawMessage = rawText;
        Text displayText = wrapWithPrefix(level, rawText);
        client.inGameHud.setOverlayMessage(displayText.copy().styled(style -> applyStyle(style, level.style())), false);
    }

    private static Text wrapWithPrefix(DangerLevel level, Text rawText) {
        String prefixKey = level.getPrefixKey();
        if (prefixKey == null) return rawText;
        return Text.translatable(prefixKey, rawText);
    }

    protected Style applyStyle(Style base, Style fromLevel) {
        return base.withColor(fromLevel.getColor()).withBold(fromLevel.isBold());
    }

    public static void resetForTick() {
        currentLevel = DangerLevel.LOW;
        currentRawMessage = Text.empty();
    }
}
