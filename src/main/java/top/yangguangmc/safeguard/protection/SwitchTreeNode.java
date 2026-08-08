package top.yangguangmc.safeguard.protection;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 树状开关容器。
 */
public class SwitchTreeNode {
    private final Identifier id;
    private final boolean defaultEnabled;
    private boolean enabled;
    private SwitchTreeNode parent;
    private final Map<Identifier, SwitchTreeNode> children = new LinkedHashMap<>();
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

    public Identifier getId() { return id; }

    public String getIdName() {
        if (id == null) return "";
        String[] split = id.getPath().split("/");
        return split[split.length - 1];
    }

    public int getLevel() {
        if (id == null) throw new IllegalStateException("Cannot get level of root node!");
        return Math.toIntExact(id.getPath().chars().filter(c -> c == '/').count());
    }

    public SwitchTreeNode getParent() { return parent; }
    public SwitchTreeNode getChild(Identifier id) { return children.get(id); }
    public Collection<SwitchTreeNode> getChildren() { return Collections.unmodifiableCollection(children.values()); }

    public SwitchTreeNode getNode(Identifier id) {
        if (nodeMap != null) return nodeMap.get(id);
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        return root.nodeMap != null ? root.nodeMap.get(id) : null;
    }

    public Collection<Identifier> getNodeIds() {
        if (nodeMap != null) return Collections.unmodifiableCollection(nodeMap.keySet());
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        if (root.nodeMap == null) throw new IllegalStateException("Root node missing internal node map");
        return Collections.unmodifiableSet(root.nodeMap.keySet());
    }

    public boolean isLeaf() { return children.isEmpty(); }
    public boolean isRoot() { return parent == null; }
    public boolean getDefaultEnabled() { return defaultEnabled; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        notifyLeafDescendants();
    }

    public boolean isEffectivelyEnabled() {
        if (!enabled) return false;
        SwitchTreeNode p = parent;
        while (p != null) {
            if (!p.enabled) return false;
            p = p.parent;
        }
        return true;
    }

    public SwitchTreeNode addOrGetNode(@NotNull Identifier id) {
        return addOrGetNode(id, true);
    }

    public SwitchTreeNode addOrGetNode(@NotNull Identifier id, boolean defaultEnabled) {
        SwitchTreeNode root = this;
        while (root.parent != null) root = root.parent;
        if (root.nodeMap == null) throw new IllegalStateException("Root node missing internal node map");
        return createNodeAndAncestors(id, root, root.nodeMap, defaultEnabled);
    }

    @SuppressWarnings("UnusedReturnValue")
    public SwitchTreeNode predefineCategory(@NotNull Identifier id, boolean defaultEnabled) {
        return addOrGetNode(id, defaultEnabled);
    }

    public void addEffectiveStateListener(Consumer<Boolean> listener) {
        effectiveStateListeners.add(listener);
    }

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

    public static SwitchTreeNode buildTree(Collection<Identifier> identifiers) {
        Map<Identifier, SwitchTreeNode> nodeMap = new HashMap<>();
        SwitchTreeNode root = new SwitchTreeNode(null, nodeMap);
        for (Identifier id : identifiers) createNodeAndAncestors(id, root, nodeMap);
        return root;
    }

    public static SwitchTreeNode buildTree(Identifier... identifiers) {
        return buildTree(Arrays.asList(identifiers));
    }

    private static @Nullable Identifier getParentIdentifier(Identifier id) {
        String path = id.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) return null;
        String parentPath = path.substring(0, lastSlash);
        return Identifier.of(id.getNamespace(), parentPath);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static SwitchTreeNode createNodeAndAncestors(Identifier id, SwitchTreeNode root, Map<Identifier, SwitchTreeNode> nodeMap) {
        return createNodeAndAncestors(id, root, nodeMap, true);
    }

    private static SwitchTreeNode createNodeAndAncestors(Identifier id, SwitchTreeNode root, Map<Identifier, SwitchTreeNode> nodeMap, boolean defaultEnabled) {
        if (nodeMap.containsKey(id)) return nodeMap.get(id);
        Identifier parentId = getParentIdentifier(id);
        SwitchTreeNode parentNode = (parentId == null) ? root : createNodeAndAncestors(parentId, root, nodeMap, true);
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
        public Collection<Identifier> getNodeIds() { return SwitchTreeNode.this.getNodeIds(); }
        @Override
        public boolean isLeaf() { return SwitchTreeNode.this.isLeaf(); }
        @Override
        public boolean isRoot() { return SwitchTreeNode.this.isRoot(); }
        @Override
        public boolean getDefaultEnabled() { return SwitchTreeNode.this.getDefaultEnabled(); }
        @Override
        public boolean isEnabled() { return SwitchTreeNode.this.isEnabled(); }

        @Override
        public void setEnabled(boolean enabled) { SwitchTreeNode.this.setEnabled(enabled); }

        @Override
        public boolean isEffectivelyEnabled() { return SwitchTreeNode.this.isEffectivelyEnabled(); }

        @Override
        public SwitchTreeNode addOrGetNode(@NotNull Identifier id) { throw new UnsupportedOperationException(); }

        @Override
        public SwitchTreeNode addOrGetNode(@NotNull Identifier id, boolean defaultEnabled) { throw new UnsupportedOperationException(); }

        @Override
        public SwitchTreeNode predefineCategory(@NotNull Identifier id, boolean defaultEnabled) { throw new UnsupportedOperationException(); }

        @Override
        public SwitchTreeNode unmodifiableView() { return this; }
    }
}
