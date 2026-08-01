package top.yangguangmc.safeguard.protection;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 树状开关容器。
 * 每个节点有一个唯一的 {@link Identifier}、一个启用状态、一个默认启用状态，
 * 并可检查"有效启用"（自身及所有祖先均启用）。
 * <p>
 * 使用静态方法 {@link #buildTree(Collection)} 从一组标识符批量构建树；
 * 使用实例方法 {@link #addOrGetNode(Identifier)} 在已有树上动态添加节点。
 * </p>
 * <p>
 * 这个类不是线程安全的。
 * </p>
 */
public class SwitchTreeNode {
    private final Identifier id;
    private final boolean defaultEnabled;
    private boolean enabled;
    private SwitchTreeNode parent;
    private final Map<Identifier, SwitchTreeNode> children = new LinkedHashMap<>();
    // 所有节点持有引用，用于全局 O(1) 查找
    private final Map<Identifier, SwitchTreeNode> nodeMap;
    private final List<Consumer<Boolean>> effectiveStateListeners = new ArrayList<>();
    private Unmodifiable unmodifiableView;

    private SwitchTreeNode(Identifier id, Map<Identifier, SwitchTreeNode> nodeMap) {
        this(id, nodeMap, true);
    }

    private SwitchTreeNode(Identifier id, Map<Identifier, SwitchTreeNode> nodeMap, boolean defaultEnabled) {
        this.id = id;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
        this.nodeMap = nodeMap;
        if (id != null && nodeMap != null) nodeMap.put(id, this);
    }

    /**
     * 获取当前节点的ID。
     * 如果当前节点是根节点，将返回{@code null}。
     */
    public Identifier getId() {
        return id;
    }

    /**
     * 获取当前节点在树结构中的末尾名称。
     * <p>
     * 末尾名称即 ID 最后一个“/”之后的部分。
     * 如果当前节点是根节点，将返回空字符串。
     * </p>
     */
    public String getIdName() {
        if (id == null) return "";
        String[] split = id.getPath().split("/");
        return split[split.length - 1];
    }

    /**
     * 获取当前节点在树结构中的层级序数。
     * <p>
     * 层级序数等于 ID 中“/”的个数。
     * 如果当前节点是根节点，将抛出异常。
     * 例子：
     * <ul>
     *     <li>“foo:bar”将返回 0</li>
     *     <li>“foo:bar1/bar2”将返回 1</li>
     *     <li>“foo:bar1/bar2/bar3”将返回 2</li>
     *     <li>“foo:bar1_bar2-bar3”将返回 0（“_”“-”均不用于计算层级）</li>
     * </ul>
     * </p>
     */
    public int getLevel() {
        if (id == null) throw new IllegalStateException("Cannot get level of root node!");
        return Math.toIntExact(id.getPath().chars().filter(c -> c == '/').count());
    }

    /**
     * 获取当前节点的父节点。
     * 如果当前节点是根节点，将返回{@code null}。
     */
    public SwitchTreeNode getParent() {
        return parent;
    }

    /**
     * 获取具有指定ID的当前节点的直接子节点。
     * <p>
     * 注意：无递归，不包括子节点的子节点。
     * 若不存在，返回{@code null}。
     * </p>
     */
    public SwitchTreeNode getChild(Identifier id) {
        return children.get(id);
    }

    /**
     * 获取当前节点的所有直接子节点。
     * <p>
     * 注意：无递归，不包括子节点的子节点。
     * </p>
     */
    public Collection<SwitchTreeNode> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    /**
     * 在整个树中按 ID 查找节点。
     * 可从任意节点调用，非根节点将自动先找到根节点。
     * 若不存在，返回{@code null}。
     * <p>
     * 由于缓存了整树的状态，此方法的时间复杂度为 O(1)。
     * </p>
     */
    public SwitchTreeNode getNode(Identifier id) {
        if (nodeMap != null) return nodeMap.get(id);
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        return root.nodeMap != null ? root.nodeMap.get(id) : null;
    }

    /**
     * 获取整个树中所有节点的ID集合。
     * 这是有深度的，集合将包括子节点的子节点的ID。
     * 可从任意节点调用，非根节点将自动先找到根节点。
     */
    public Collection<Identifier> getNodeIds() {
        if (nodeMap != null) return Collections.unmodifiableCollection(nodeMap.keySet());
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        if (root.nodeMap == null) throw new IllegalStateException("Root node missing internal node map");
        return Collections.unmodifiableSet(root.nodeMap.keySet());
    }

    /**
     * 判断当前节点是否为叶节点。
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * 判断当前节点是否为根节点。
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 返回当前节点的默认启用状态。
     */
    public boolean getDefaultEnabled() {
        return defaultEnabled;
    }

    /**
     * 返回当前节点的启用状态。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置当前节点的启用状态。
     * 设置后会触发有效状态变更通知。
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        notifyLeafDescendants();
    }

    /**
     * 返回当前节点的有效启用状态。
     * 只有自身及所有祖先均启用才返回{@code true}。
     */
    public boolean isEffectivelyEnabled() {
        if (!enabled) return false;
        SwitchTreeNode p = parent;
        while (p != null) {
            if (!p.enabled) return false;
            p = p.parent;
        }
        return true;
    }

    /**
     * 在树中创建或获取节点，然后返回。
     * 新节点默认启用。
     */
    public SwitchTreeNode addOrGetNode(@NotNull Identifier id) {
        return addOrGetNode(id, true);
    }

    /**
     * 在树中创建或获取节点，然后返回。
     *
     * @param defaultEnabled 指定该节点的默认启用状态。
     */
    public SwitchTreeNode addOrGetNode(@NotNull Identifier id, boolean defaultEnabled) {
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        if (root.nodeMap == null) throw new IllegalStateException("Root node missing internal node map");
        return createNodeAndAncestors(id, root, root.nodeMap, defaultEnabled);
    }

    /**
     * 预定义一个分类节点及其默认启用状态。
     */
    @SuppressWarnings("UnusedReturnValue")
    public SwitchTreeNode predefineCategory(@NotNull Identifier id, boolean defaultEnabled) {
        return addOrGetNode(id, defaultEnabled);
    }

    /**
     * 注册有效状态变更监听器。
     */
    public void addEffectiveStateListener(Consumer<Boolean> listener) {
        effectiveStateListeners.add(listener);
    }

    /**
     * 获得该节点的不可修改视图。
     * <p>
     * 返回的视图可以看作与该节点一样，但不同的是，
     * 任何涉及修改树结构（如父子节点的对应关系、添加子节点，但不包括设置启用状态）的方法都将抛出{@link UnsupportedOperationException}。
     * </p>
     */
    public SwitchTreeNode unmodifiableView() {
        if (unmodifiableView == null) unmodifiableView = new Unmodifiable();
        return unmodifiableView;
    }

    private void addChild(SwitchTreeNode child) {
        children.put(child.getId(), child);
        child.parent = this;
    }

    private void notifyLeafDescendants() {
        if (isLeaf()) {
            boolean effective = isEffectivelyEnabled();
            for (Consumer<Boolean> listener : effectiveStateListeners) {
                listener.accept(effective);
            }
        } else {
            for (SwitchTreeNode child : children.values()) {
                child.notifyLeafDescendants();
            }
        }
    }

    /**
     * 根据一组标识符构建树，然后返回其根节点。
     */
    public static SwitchTreeNode buildTree(Collection<Identifier> identifiers) {
        Map<Identifier, SwitchTreeNode> nodeMap = new HashMap<>();
        SwitchTreeNode root = new SwitchTreeNode(null, nodeMap);
        for (Identifier id : identifiers) createNodeAndAncestors(id, root, nodeMap);
        return root;
    }

    public static SwitchTreeNode buildTree(Identifier... identifiers) {
        return buildTree(Arrays.asList(identifiers));
    }

    /**
     * 根据路径中最后一个 / 推导父 ID，若无则返回 null。
     */
    private static @Nullable Identifier getParentIdentifier(Identifier id) {
        String path = id.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) return null;
        String parentPath = path.substring(0, lastSlash);
        return Identifier.of(id.getNamespace(), parentPath);
    }

    /**
     * 递归创建节点及所有必需的祖先，并挂载到正确父节点下。
     */
    @SuppressWarnings("UnusedReturnValue")
    private static SwitchTreeNode createNodeAndAncestors(Identifier id,
                                                         SwitchTreeNode root,
                                                         Map<Identifier, SwitchTreeNode> nodeMap) {
        return createNodeAndAncestors(id, root, nodeMap, true);
    }

    private static SwitchTreeNode createNodeAndAncestors(Identifier id,
                                                         SwitchTreeNode root,
                                                         Map<Identifier, SwitchTreeNode> nodeMap,
                                                         boolean defaultEnabled) {
        if (nodeMap.containsKey(id)) return nodeMap.get(id);
        // 确保祖先存在（祖先始终默认为 true，避免副作用）
        Identifier parentId = getParentIdentifier(id);
        SwitchTreeNode parentNode = (parentId == null) ? root
                : createNodeAndAncestors(parentId, root, nodeMap, true);
        // 创建目标节点，使用指定的 defaultEnabled
        SwitchTreeNode node = new SwitchTreeNode(id, nodeMap, defaultEnabled);
        parentNode.addChild(node);
        return node;
    }

    private class Unmodifiable extends SwitchTreeNode {
        private Unmodifiable() {
            super(SwitchTreeNode.this.id, SwitchTreeNode.this.nodeMap);
        }

        @Override
        public SwitchTreeNode getParent() {
            SwitchTreeNode parent = SwitchTreeNode.this.getParent();
            return parent == null ? null : parent.unmodifiableView();
        }

        @Override
        public SwitchTreeNode getChild(Identifier id) {
            SwitchTreeNode child = SwitchTreeNode.this.getChild(id);
            return child == null ? null : child.unmodifiableView();
        }

        @Override
        public Collection<SwitchTreeNode> getChildren() {
            return SwitchTreeNode.this.getChildren().stream().map(SwitchTreeNode::unmodifiableView).collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public SwitchTreeNode getNode(Identifier id) {
            SwitchTreeNode node = SwitchTreeNode.this.getNode(id);
            return node == null ? null : node.unmodifiableView();
        }

        @Override
        public Collection<Identifier> getNodeIds() {
            return SwitchTreeNode.this.getNodeIds();
        }

        @Override
        public boolean isLeaf() {
            return SwitchTreeNode.this.isLeaf();
        }

        @Override
        public boolean isRoot() {
            return SwitchTreeNode.this.isRoot();
        }

        @Override
        public boolean getDefaultEnabled() {
            return SwitchTreeNode.this.getDefaultEnabled();
        }

        @Override
        public boolean isEnabled() {
            return SwitchTreeNode.this.isEnabled();
        }

        @Override
        public void setEnabled(boolean enabled) {
            SwitchTreeNode.this.setEnabled(enabled);
        }

        @Override
        public boolean isEffectivelyEnabled() {
            return SwitchTreeNode.this.isEffectivelyEnabled();
        }

        @Override
        public SwitchTreeNode addOrGetNode(@NotNull Identifier id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SwitchTreeNode addOrGetNode(@NotNull Identifier id, boolean defaultEnabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SwitchTreeNode predefineCategory(@NotNull Identifier id, boolean defaultEnabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SwitchTreeNode unmodifiableView() {
            return this;
        }
    }
}