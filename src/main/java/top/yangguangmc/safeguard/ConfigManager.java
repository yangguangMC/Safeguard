package top.yangguangmc.safeguard;

import com.google.gson.*;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.detection.Detection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModContext.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CONFIG_VERSION_KEY = "_version";
    private static final int CONFIG_VERSION = 1;
    private ModContext ctx;

    public void init(ModContext ctx) {
        this.ctx = ctx;
    }

    public void trySave() {
        try {
            save();
        } catch (Exception e) {
            LOGGER.error("Could not save config file!", e);
            try {
                LOGGER.info("Trying to backup original file and retry saving...");
                backup();
                save();
            } catch (Exception ex) {
                LOGGER.error("Could not save config file during backup or after retrying!", ex);
            }
        }
    }

    public void save() throws IOException {
        JsonObject json = new JsonObject();

        JsonObject detectionJson = new JsonObject();
        buildDetectionTree(detectionJson, ctx.protectionManager().getDetectionStatesRoot(), true);
        json.add("detection", detectionJson);

        JsonObject actionJson = new JsonObject();
        buildActionTree(actionJson, ctx.protectionManager().getActionStatesRoot(), true);
        json.add("action", actionJson);

        json.addProperty(CONFIG_VERSION_KEY, CONFIG_VERSION);
        Files.writeString(getConfig(), GSON.toJson(json), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.debug("Successfully saved config to '{}'.", getConfig());
    }

    public void tryLoad() {
        if (!Files.exists(getConfig())) {
            LOGGER.info("No config file found, using defaults.");
            return;
        }
        boolean loaded = false;
        try {
            load();
            loaded = true;
        } catch (IOException e) {
            LOGGER.warn("Failed to load config, using defaults.", e);
        } catch (Exception e) {
            LOGGER.warn("Config file corrupted, using defaults.", e);
        }
        if (!loaded) {
            tryBackup();
        }
    }

    public void load() throws IOException {
        String content = Files.readString(getConfig());
        JsonObject json = GSON.fromJson(content, JsonObject.class);
        if (json == null) throw new JsonParseException("JsonObject is null or empty.");
        int version = json.has(CONFIG_VERSION_KEY) ? json.get(CONFIG_VERSION_KEY).getAsInt() : 0;
        if (version > CONFIG_VERSION) {
            LOGGER.warn("The version of the config file is higher that coded version! File: {}, code: {}. Config loading can no longer be guaranteed.", version, CONFIG_VERSION);
            tryBackup();
        } else if (version < CONFIG_VERSION) {
            LOGGER.info("Trying to migrate config from version {} to {}.", version, CONFIG_VERSION);
            tryBackup();
            for (int v = version; v < CONFIG_VERSION; v++) {
                //noinspection DataFlowIssue
                json = migrate(json, v);
            }
        }

        JsonObject dj = json.getAsJsonObject("detection");
        if (dj != null) loadDetectionTree(dj, "", "");

        JsonObject aj = json.getAsJsonObject("action");
        if (aj != null) loadActionTree(aj, "", "");
        LOGGER.debug("Successfully loaded config from '{}'.", getConfig());
    }

    public void tryBackup() {
        LOGGER.info("Trying to backup original file...");
        try {
            backup();
        } catch (Exception ex) {
            LOGGER.error("Failed to backup original config file!", ex);
        }
    }

    public void backup() throws IOException {
        Path config = getConfig();
        if (Files.exists(config)) {
            Path target = YACLPlatform.getConfigDir().resolve(ModContext.MOD_ID + ".json.backup");
            Files.move(config, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Successfully backed config up to '{}'.", target);
        }
    }

    private JsonObject migrate(JsonObject json, @SuppressWarnings("unused") int fromVersion) {
        return json;
    }

    private void buildDetectionTree(JsonObject parent, SwitchTreeNode node, boolean isTopLevel) {
        for (SwitchTreeNode child : node.getChildren()) {
            String key = isTopLevel ? child.getId().toString() : child.getIdName();
            JsonObject childObj = new JsonObject();
            childObj.addProperty("enabled", child.isEnabled());

            if (child.isLeaf()) {
                Detection detection = ctx.protectionManager().getDetection(child.getId());
                JsonObject bindings = new JsonObject();
                for (Action action : detection.getBoundActions()) {
                    bindings.addProperty(action.getId().toString(), detection.isBindingEnabled(action.getId()));
                }
                childObj.add("actionBindings", bindings);
            } else {
                JsonObject childrenObj = new JsonObject();
                buildDetectionTree(childrenObj, child, false);
                childObj.add("children", childrenObj);
            }

            parent.add(key, childObj);
        }
    }

    private void buildActionTree(JsonObject parent, SwitchTreeNode node, boolean isTopLevel) {
        for (SwitchTreeNode child : node.getChildren()) {
            String key = isTopLevel ? child.getId().toString() : child.getIdName();
            JsonObject childObj = new JsonObject();
            childObj.addProperty("enabled", child.isEnabled());

            if (!child.isLeaf()) {
                JsonObject childrenObj = new JsonObject();
                buildActionTree(childrenObj, child, false);
                childObj.add("children", childrenObj);
            }

            parent.add(key, childObj);
        }
    }

    private void loadDetectionTree(JsonObject json, String namespace, String parentPath) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (!(entry.getValue() instanceof JsonObject nodeObj)) {
                LOGGER.warn("Value of level 0 detection root {} is not a JsonObject, skipping.", key);
                continue;
            }

            Identifier id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = Identifier.tryParse(key);
                if (id == null) {
                    LOGGER.warn("Level 0 detection root {} is not a valid Identifier, skipping.", key);
                    continue;
                }
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = Identifier.fromNamespaceAndPath(namespace, newPath);
            }

            SwitchTreeNode node = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
            if (node != null) {
                if (nodeObj.has("enabled")) {
                    node.setEnabled(nodeObj.get("enabled").getAsBoolean());
                }

                if (nodeObj.has("actionBindings") && !nodeObj.has("children")) {
                    try {
                        Detection detection = ctx.protectionManager().getDetection(id);
                        JsonObject bindings = nodeObj.getAsJsonObject("actionBindings");
                        for (Map.Entry<String, JsonElement> be : bindings.entrySet()) {
                            Identifier actionId = Identifier.parse(be.getKey());
                            detection.setBindingEnabled(actionId, be.getValue().getAsBoolean());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to restore bindings for detection {}, skipping.", id, e);
                    }
                }
            } else {
                LOGGER.warn("Detection {} in config not found in tree, skipping.", id);
            }

            if (nodeObj.has("children")) {
                loadDetectionTree(nodeObj.getAsJsonObject("children"), ns, newPath);
            }
        }
    }

    private void loadActionTree(JsonObject json, String namespace, String parentPath) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (!(entry.getValue() instanceof JsonObject nodeObj)) {
                LOGGER.warn("Value of level 0 action root {} is not a JsonObject, skipping.", key);
                continue;
            }

            Identifier id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = Identifier.tryParse(key);
                if (id == null) {
                    LOGGER.warn("Level 0 action root {} is not a valid Identifier, skipping.", key);
                    continue;
                }
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = Identifier.fromNamespaceAndPath(namespace, newPath);
            }

            SwitchTreeNode node = ctx.protectionManager().getActionStatesRoot().getNode(id);
            if (node != null) {
                if (nodeObj.has("enabled")) {
                    node.setEnabled(nodeObj.get("enabled").getAsBoolean());
                }
            } else {
                LOGGER.warn("Action {} in config not found in tree, skipping.", id);
            }

            if (nodeObj.has("children")) {
                loadActionTree(nodeObj.getAsJsonObject("children"), ns, newPath);
            }
        }
    }

    private Path getConfig() {
        return YACLPlatform.getConfigDir().resolve(ModContext.MOD_ID + ".json");
    }

    /*
    预期的配置结构如下。虽然写着“json5”，但实际储存格式可能还是要用不带注释的 JSON。
```json5
{
  // 检测项根
  // 各个节点的开关状态是按照树结构储存的。
  "detection": {
    // 枝干节点是 Category，必须有 `enabled` 和 `children` 两个字段。
    // 只有底层 Category 的键才要声明命名空间，其所有后代视为与它同命名空间。
    "namespace1:category1": {
      "enabled": true,
      "children": {
        "category2": {
          "enabled": true,
          "children": {
            // 叶节点，至少有 `enabled`，其他为该叶节点的特定配置，数量不限。
            "detection1": {
              "enabled": true,
              // 对于检测项，还应该有 `actionBindings` 字段，储存 **与该检测项绑定的保护动作实例** 的开关状态。
              // 这种绑定是单向的，检测项是 **事实单例** 而保护动作不是，检测项到保护动作是一对多的关系。
              "actionBindings": {
                "namespace1:category3/action1": true,
                "namespace1:category3/category4/action2": true
              },
              "otherKey": "otherValue"
            }
          }
        }
      }
    }
  },
  // 保护动作根
  "action": {
    "namespace1:category3": {
      "enabled": true,
      "children": {
        "action1": {
          "enabled": true,
          "otherKey": "otherValue"
        },
        "category4": {
          "enabled": true,
          "children": {
            "action2": {
              "enabled": true,
              "otherKey": "otherValue"
            }
          }
        }
      }
    }
  }
}
```
     */
}
