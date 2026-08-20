package top.yangguangmc.safeguard.protection.option;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ConfigOption} 的有序容器，供 {@code Detection}/{@code Action} 各自持有一份。
 * 保序（注册顺序 = GUI 展示顺序），键唯一性由 {@link ConfigOption#key()} 保证。
 */
public final class OptionSet {
    private final Map<String, ConfigOption<?>> options = new LinkedHashMap<>();

    public <O extends ConfigOption<?>> O register(O option) {
        if (options.putIfAbsent(option.key(), option) != null) {
            throw new IllegalArgumentException("Duplicate config option key: " + option.key());
        }
        return option;
    }

    public Collection<ConfigOption<?>> options() {
        return Collections.unmodifiableCollection(options.values());
    }
}
