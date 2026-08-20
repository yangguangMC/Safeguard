package top.yangguangmc.safeguard.protection.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 颜色配置项，值以 ARGB int 存储/暴露（{@link #get()}），JSON 中以十六进制字符串
 * （{@code "#AARRGGBB"} 或 {@code "#RRGGBB"}）持久化以便人类阅读。
 */
public final class ColorOption extends ConfigOption<Integer> {
    private final boolean allowAlpha;

    private ColorOption(String key, int defaultValueArgb, boolean allowAlpha) {
        super(key, allowAlpha ? defaultValueArgb : (defaultValueArgb | 0xFF000000));
        this.allowAlpha = allowAlpha;
    }

    /**
     * @param key              选项键
     * @param defaultValueArgb 默认颜色（ARGB 格式；若 {@code allowAlpha} 为 {@code false}，
     *                         alpha 位将被忽略并强制为不透明）
     * @param allowAlpha       是否允许调整透明度
     */
    public static ColorOption of(String key, int defaultValueArgb, boolean allowAlpha) {
        return new ColorOption(key, defaultValueArgb, allowAlpha);
    }

    @Override
    protected Integer validate(Integer value) {
        return allowAlpha ? value : (value | 0xFF000000);
    }

    @Override
    public JsonElement toJson(Integer value) {
        return new JsonPrimitive(formatHex(value));
    }

    @Override
    public Integer fromJson(JsonElement element) {
        String hex = element.getAsString().trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        long parsed = Long.parseLong(hex, 16);
        if (hex.length() <= 6) parsed |= 0xFF000000L;
        return (int) parsed;
    }

    @Override
    public Component formatValue(Integer value) {
        return Component.literal(formatHex(value));
    }

    private String formatHex(int value) {
        return allowAlpha ? String.format("#%08X", value) : String.format("#%06X", value & 0xFFFFFF);
    }

    @Override
    public Option<?> buildYaclOption(Component name, OptionDescription description,
                                      Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Color>createBuilder()
                .name(name)
                .description(description)
                .binding(new Color(defaultValue(), allowAlpha),
                        () -> new Color(getter.get(), allowAlpha),
                        color -> setter.accept(color.getRGB()))
                .controller(option -> ColorControllerBuilder.create(option).allowAlpha(allowAlpha))
                .build();
    }
}
