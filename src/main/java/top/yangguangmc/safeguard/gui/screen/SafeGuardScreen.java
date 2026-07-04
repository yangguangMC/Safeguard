package top.yangguangmc.safeguard.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SafeGuardScreen extends Screen {
    @Nullable
    protected final Screen parent;

    public SafeGuardScreen(Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(new TextWidget(title, textRenderer)).setPosition(width / 2 - textRenderer.getWidth(title) / 2, 20);
    }

    @SuppressWarnings("resource")
    @Override
    public void close() {
        client().setScreen(parent);
    }

    protected @NotNull MinecraftClient client() {
        if (client != null) return client;
        else throw new IllegalStateException("client not initialized for the screen");
    }
}
