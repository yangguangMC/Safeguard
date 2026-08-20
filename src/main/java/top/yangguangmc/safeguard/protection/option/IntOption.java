package top.yangguangmc.safeguard.protection.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 有界整数配置项，GUI 中用滑条呈现。
 */
public final class IntOption extends ConfigOption<Integer> {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    private IntOption(String key, int defaultValue) {
        super(key, defaultValue);
    }

    public static IntOption of(String key, int defaultValue) {
        return new IntOption(key, defaultValue);
    }

    public IntOption range(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    protected Integer validate(Integer value) {
        return Mth.clamp(value, min, max);
    }

    @Override
    public JsonElement toJson(Integer value) {
        return new JsonPrimitive(value);
    }

    @Override
    public Integer fromJson(JsonElement element) {
        return element.getAsInt();
    }

    @Override
    public Component formatValue(Integer value) {
        return Component.literal(String.valueOf(value));
    }

    @Override
    public Option<Integer> buildYaclOption(Component name, OptionDescription description,
                                            Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(name)
                .description(description)
                .binding(defaultValue(), getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(min, max)
                        .step(1)
                        .formatValue(this::formatValue))
                .build();
    }
}
