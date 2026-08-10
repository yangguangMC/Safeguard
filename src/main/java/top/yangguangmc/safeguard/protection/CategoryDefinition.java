package top.yangguangmc.safeguard.protection;

import net.minecraft.resources.Identifier;
import top.yangguangmc.safeguard.ModContext;

/**
 * 分类定义，用于声明树中枝干节点的默认启用状态。
 * <p>
 * 使用示例：
 * <pre>{@code
 * protectionManager.predefineActionCategory(
 *     new CategoryDefinition("active/afk", false));
 * }</pre>
 * </p>
 *
 * @param id             分类的 Identifier
 * @param defaultEnabled 默认是否启用
 */
public record CategoryDefinition(Identifier id, boolean defaultEnabled) {

    /**
     * 使用 safeguard 命名空间的便捷构造。
     *
     * @param path           分类路径（不含命名空间，如 "active/afk"）
     * @param defaultEnabled 默认是否启用
     */
    public CategoryDefinition(String path, boolean defaultEnabled) {
        this(Identifier.fromNamespaceAndPath(ModContext.MOD_ID, path), defaultEnabled);
    }
}
