package top.yangguangmc.safeguard.protection.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 布尔配置项，GUI 中用 TickBox 呈现——有意与叶节点/分支的开关（BooleanController）区分开，
 * 表明它"不是开关树结构的一部分"。
 */
public final class BoolOption extends ConfigOption<Boolean> {
    private BoolOption(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    public static BoolOption of(String key, boolean defaultValue) {
        return new BoolOption(key, defaultValue);
    }

    @Override
    public JsonElement toJson(Boolean value) {
        return new JsonPrimitive(value);
    }

    @Override
    public Boolean fromJson(JsonElement element) {
        return element.getAsBoolean();
    }

    @Override
    public Component formatValue(Boolean value) {
        return (value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF).copy();
    }

    @Override
    public Option<Boolean> buildYaclOption(Component name, OptionDescription description,
                                            Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(name)
                .description(description)
                .binding(defaultValue(), getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}
