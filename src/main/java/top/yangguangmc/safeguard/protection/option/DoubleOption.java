package top.yangguangmc.safeguard.protection.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 有界浮点数配置项，GUI 中用滑条呈现，可选以百分比显示。
 */
public final class DoubleOption extends ConfigOption<Double> {
    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;
    private double step = 0.1;
    private boolean percent;

    private DoubleOption(String key, double defaultValue) {
        super(key, defaultValue);
    }

    public static DoubleOption of(String key, double defaultValue) {
        return new DoubleOption(key, defaultValue);
    }

    public DoubleOption range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public DoubleOption step(double step) {
        this.step = step;
        return this;
    }

    /**
     * 标记该选项以百分比形式显示（滑条显示如 "30%"），存储/使用的值仍是原始比率（如 0.3）。
     */
    public DoubleOption percent() {
        this.percent = true;
        this.step = 0.01;
        return this;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    @Override
    protected Double validate(Double value) {
        return Mth.clamp(value, min, max);
    }

    @Override
    public JsonElement toJson(Double value) {
        return new JsonPrimitive(value);
    }

    @Override
    public Double fromJson(JsonElement element) {
        return element.getAsDouble();
    }

    @Override
    public Component formatValue(Double value) {
        return Component.literal(percent ? String.format("%.0f%%", value * 100) : String.format("%.2f", value));
    }

    @Override
    public Option<Double> buildYaclOption(Component name, OptionDescription description,
                                           Supplier<Double> getter, Consumer<Double> setter) {
        return Option.<Double>createBuilder()
                .name(name)
                .description(description)
                .binding(defaultValue(), getter, setter)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                        .range(min, max)
                        .step(step)
                        .formatValue(this::formatValue))
                .build();
    }
}
