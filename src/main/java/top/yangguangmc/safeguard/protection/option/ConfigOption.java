package top.yangguangmc.safeguard.protection.option;

import com.google.gson.JsonElement;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 检测项/保护动作的一项可配置参数。
 * <p>
 * 每个 {@link ConfigOption} 自身即为其值的唯一真相源（无需反射），通过 {@link #get()}/{@link #set}
 * 读写；子类负责声明具体类型、取值范围与对应的 YACL 控制器。
 * </p>
 * <p>
 * 默认情况下，一个选项属于其宿主（{@code Detection} 或 {@code Action}）本身，按 ID 全局生效，
 * 持久化在该 ID 对应叶节点的 {@code options} 字段中。调用 {@link #pairScoped()} 后，该选项被标记为
 * "检测项-动作对专属"——仅当它由某个 {@code Action} 内部的静态子类持有时才有意义，此时持久化到绑定
 * 它的检测项叶节点的 {@code actionOptions} 字段中，而非该动作 ID 对应的全局 {@code options} 字段。
 * </p>
 *
 * @param <T> 选项值的类型
 */
public abstract class ConfigOption<T> {
    private final String key;
    private final T defaultValue;
    private T value;
    private boolean pairScoped;

    protected ConfigOption(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /**
     * 该选项在宿主内的唯一键名（如 {@code "distance"}），用于 JSON 持久化与 i18n 翻译键拼接。
     */
    public final String key() {
        return key;
    }

    public final T defaultValue() {
        return defaultValue;
    }

    public final T get() {
        return value;
    }

    /**
     * 设置该选项的值。子类应通过覆写 {@link #validate(Object)} 实现钳制/校验。
     */
    public final void set(T value) {
        this.value = validate(value);
    }

    /**
     * 校验/钳制传入的值，默认原样返回。
     */
    protected T validate(T value) {
        return value;
    }

    /**
     * 标记该选项为"检测项-动作对专属"。见类文档。
     *
     * @return 便于链式调用，返回值即为 {@code this}
     */
    public final ConfigOption<T> pairScoped() {
        this.pairScoped = true;
        return this;
    }

    public final boolean isPairScoped() {
        return pairScoped;
    }

    /**
     * 将给定值序列化为 JSON。
     */
    public abstract JsonElement toJson(T value);

    /**
     * 从 JSON 解析出该选项的值（不做钳制，钳制交给 {@link #set}）。
     * 解析失败应抛出异常，由调用方（{@code ConfigManager}）捕获并回退默认值。
     */
    public abstract T fromJson(JsonElement element);

    /**
     * 将值格式化为可读文本，用于 GUI 描述行与滑条数值显示。
     */
    public abstract Component formatValue(T value);

    /**
     * 构建该选项对应的 YACL {@link Option}。
     *
     * @param name        选项名称（已按 i18n 约定翻译）
     * @param description 选项描述（默认值/范围/ID 等，由调用方统一拼装）
     * @param getter      读取当前值（可能来自单一宿主实例，也可能是多实例中的代表实例）
     * @param setter      写入新值（全局动作选项时需扇出到所有同 ID 实例，由调用方负责）
     */
    public abstract Option<?> buildYaclOption(Component name, OptionDescription description,
                                               Supplier<T> getter, Consumer<T> setter);
}
