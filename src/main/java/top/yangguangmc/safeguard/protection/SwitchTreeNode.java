package top.yangguangmc.safeguard.protection;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 树状开关容器。
 * 每个节点有一个唯一的 {@link Identifier}、一个启用状态，
 * 并可检查“有效启用”（自身及所有祖先均启用）。
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
    private boolean enabled = true;
    private SwitchTreeNode parent;
    private final Map<Identifier, SwitchTreeNode> children = new LinkedHashMap<>();

    // 有且仅有根节点持有，用于全局 O(1) 查找
    private final Map<Identifier, SwitchTreeNode> nodeMap;

    private Unmodifiable unmodifiableView;

    private SwitchTreeNode(Identifier id, Map<Identifier, SwitchTreeNode> nodeMap) {
        this.id = id;
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
     * 即其 ID 最后一个“/”之后的部分。
     * 如果当前节点是根节点，将返回空字符串。
     */
    public String getIdName() {
        if (id == null) return "";
        String[] split = id.getPath().split("/");
        return split[split.length - 1];
    }

    /**
     * 获取当前节点在树结构中的层级序数。
     * 即其 ID 中“/”的个数。
     * 如果当前节点是根节点，将抛出异常。
     * 例子：
     * <ul>
     *     <li>“foo:bar”将返回 0</li>
     *     <li>“foo:bar1/bar2”将返回 1</li>
     *     <li>“foo:bar1/bar2/bar3”将返回 2</li>
     *     <li>“foo:bar1_bar2-bar3”将返回 0（“_”“-”均不用于计算层级）</li>
     * </ul>
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
     * 注意：无递归，不包括子节点的子节点。
     * 若不存在，返回{@code null}。
     */
    public SwitchTreeNode getChild(Identifier id) {
        return children.get(id);
    }

    /**
     * 获取具当前节点的所以直接子节点。
     * 注意：无递归，不包括子节点的子节点。
     */
    public Collection<SwitchTreeNode> getChildren() {
        return Collections.unmodifiableCollection(children.values());
    }

    /**
     * 在整个树中按 ID 查找节点。
     * 可从任意节点调用，非根节点将自动先找到根节点。
     * 若不存在，返回{@code null}。
     */
    public SwitchTreeNode getNode(Identifier id) {
        if (nodeMap != null) return nodeMap.get(id);
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        return root.nodeMap != null ? root.nodeMap.get(id) : null;
    }

    /**
     * 获取在整个树中所有节点的ID集合。
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
     * 判断当前节点是否为叶节点，即是否无子节点。
     *
     * @return 若为叶节点则返回{@code true}，否则返回{@code false}。
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * 判断当前节点是否为根节点，即是否无父节点。
     *
     * @return 若为根节点则返回{@code true}，否则返回{@code false}。
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 返回当前节点的启用状态。
     * 这是每个节点上可设置的独立的启用状态，与任何父节点无关，这与{@link #isEffectivelyEnabled()}相区别。
     * 若无单独设置，则默认启用状态为{@code true}。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置当前节点的启用状态。
     *
     * @see #isEnabled()
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回当前节点的有效启用状态。
     * 只有自身及所有祖先均启用才返回{@code true}，这与{@link #isEnabled()}相区别。
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
     * 在树中按给定标识符创建（或获取已存在的）节点，并将其及其所有缺失的祖先节点挂载到正确位置。
     * 可从任意节点调用，内部自动定位到根节点执行，保证全局一致。
     *
     * @param id 要添加或获取的节点 ID
     * @return 对应于该 ID 的节点（若已存在则返回现有节点）
     */
    public SwitchTreeNode addOrGetNode(@NotNull Identifier id) {
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        if (root.nodeMap == null) throw new IllegalStateException("Root node missing internal node map");
        return createNodeAndAncestors(id, root, root.nodeMap);
    }

    /**
     * 获得该节点的不可修改视图。
     * 返回的视图可以看作与该节点一样，但不同的是，
     * 任何涉及修改树结构（如父子节点的对应关系、添加子节点，但不包括设置启用状态）的方法都将抛出{@link UnsupportedOperationException}。
     */
    public SwitchTreeNode unmodifiableView() {
        if (unmodifiableView == null) unmodifiableView = new Unmodifiable();
        return unmodifiableView;
    }

    private void addChild(SwitchTreeNode child) {
        children.put(child.getId(), child);
        child.parent = this;
    }

    /**
     * 根据一组 {@link Identifier} 构建树，自动通过路径中的 {@code /} 确定父子关系。
     *
     * @return 根节点（其 id 为 null）
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
    private static SwitchTreeNode createNodeAndAncestors(Identifier id,
                                                         SwitchTreeNode root,
                                                         Map<Identifier, SwitchTreeNode> nodeMap) {
        if (nodeMap.containsKey(id)) return nodeMap.get(id);
        SwitchTreeNode node = new SwitchTreeNode(id, nodeMap);
        Identifier parentId = getParentIdentifier(id);
        SwitchTreeNode parentNode = (parentId == null) ? root
                : createNodeAndAncestors(parentId, root, nodeMap);
        parentNode.addChild(node);
        return node;
    }

    private class Unmodifiable extends SwitchTreeNode {
        private Unmodifiable() {
            super(null, null);
        }

        @Override
        public Identifier getId() {
            return SwitchTreeNode.this.getId();
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
        public SwitchTreeNode unmodifiableView() {
            return this;
        }
    }
}