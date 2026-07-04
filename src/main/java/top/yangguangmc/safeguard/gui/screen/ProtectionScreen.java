package top.yangguangmc.safeguard.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public class ProtectionScreen extends SafeGuardScreen {

    public ProtectionScreen(@Nullable Screen parent) {
        super(Text.translatable("messages.safeguard.name").styled(style -> style.withColor(Formatting.GREEN))
                .append(" - ").append(Text.translatable("screen.safeguard.protection")), parent);
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).width(150).position(width / 2 - 150 / 2, height - 20 - 20).build());
    }
}
