package top.yangguangmc.safeguard.protection.action;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DangerLevel {
    CRITICAL(Formatting.RED, true, "danger.safeguard.level.critical"),
    HIGH(Formatting.RED, false, "danger.safeguard.level.high"),
    MEDIUM(Formatting.GOLD, false, "danger.safeguard.level.medium"),
    LOW(Formatting.YELLOW, false, "danger.safeguard.level.low"),
    INFO(Formatting.GRAY, false, null);

    private final Formatting color;
    private final boolean bold;
    @Nullable
    private final String prefixKey;

    DangerLevel(Formatting color, boolean bold, @Nullable String prefixKey) {
        this.color = color;
        this.bold = bold;
        this.prefixKey = prefixKey;
    }

    public @NotNull Style style() {
        Style style = Style.EMPTY.withColor(color);
        return bold ? style.withBold(true) : style;
    }

    @Nullable
    public String getPrefixKey() {
        return prefixKey;
    }
}
