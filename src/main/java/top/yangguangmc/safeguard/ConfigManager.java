package top.yangguangmc.safeguard;

import com.google.gson.*;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.detection.Detection;
import top.yangguangmc.safeguard.protection.option.ConfigOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        // 先加载动作树（全局配置项），再加载检测项树（检测项自身配置项 + 成对覆盖配置项），
        // 使得成对配置项在与全局配置项同名时能够生效（覆盖语义）。
        JsonObject aj = json.getAsJsonObject("action");
        if (aj != null) loadActionTree(aj, "", "");

        JsonObject dj = json.getAsJsonObject("detection");
        if (dj != null) loadDetectionTree(dj, "", "");
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

                if (!detection.getOptions().isEmpty()) {
                    JsonObject options = new JsonObject();
                    for (ConfigOption<?> option : detection.getOptions()) {
                        options.add(option.key(), toJsonTyped(option));
                    }
                    childObj.add("options", options);
                }

                JsonObject bindings = new JsonObject();
                for (Action action : detection.getBoundActions()) {
                    bindings.addProperty(action.getId().toString(), detection.isBindingEnabled(action.getId()));
                }
                childObj.add("actionBindings", bindings);

                JsonObject actionOptions = new JsonObject();
                for (Action action : detection.getBoundActions()) {
                    Collection<ConfigOption<?>> pairOptions = action.getOptions().stream()
                            .filter(ConfigOption::isPairScoped)
                            .toList();
                    if (pairOptions.isEmpty()) continue;
                    JsonObject actionOptionsObj = new JsonObject();
                    for (ConfigOption<?> option : pairOptions) {
                        actionOptionsObj.add(option.key(), toJsonTyped(option));
                    }
                    actionOptions.add(action.getId().toString(), actionOptionsObj);
                }
                if (!actionOptions.entrySet().isEmpty()) childObj.add("actionOptions", actionOptions);
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

            if (child.isLeaf()) {
                List<Action> instances = ctx.protectionManager().getActionInstances(child.getId());
                if (!instances.isEmpty()) {
                    Collection<ConfigOption<?>> globalOptions = instances.getFirst().getOptions().stream()
                            .filter(option -> !option.isPairScoped())
                            .toList();
                    if (!globalOptions.isEmpty()) {
                        JsonObject options = new JsonObject();
                        for (ConfigOption<?> option : globalOptions) {
                            options.add(option.key(), toJsonTyped(option));
                        }
                        childObj.add("options", options);
                    }
                }
            } else {
                JsonObject childrenObj = new JsonObject();
                buildActionTree(childrenObj, child, false);
                childObj.add("children", childrenObj);
            }

            parent.add(key, childObj);
        }
    }

    /**
     * 对通配符类型的 {@link ConfigOption} 做类型捕获转换后序列化，避免调用方处理泛型。
     */
    private static <T> JsonElement toJsonTyped(ConfigOption<T> option) {
        return option.toJson(option.get());
    }

    /**
     * 对通配符类型的 {@link ConfigOption} 做类型捕获转换后从 JSON 解析并设值；
     * 解析失败时记录警告并保留原值（含默认值）。
     */
    private static <T> void applyJsonToOption(ConfigOption<T> option, JsonElement element) {
        try {
            option.set(option.fromJson(element));
        } catch (Exception e) {
            LOGGER.warn("Failed to parse config option '{}' from value '{}', keeping current value.", option.key(), element, e);
        }
    }

    private void loadDetectionTree(JsonObject json, String namespace, String parentPath) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            if (!(entry.getValue() instanceof JsonObject nodeObj)) {
                LOGGER.warn("Value of level 0 detection root {} is not a JsonObject, skipping.", key);
                continue;
            }

            ResourceLocation id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = ResourceLocation.tryParse(key);
                if (id == null) {
                    LOGGER.warn("Level 0 detection root {} is not a valid Identifier, skipping.", key);
                    continue;
                }
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = ResourceLocation.tryBuild(namespace, newPath);
            }

            SwitchTreeNode node = ctx.protectionManager().getDetectionStatesRoot().getNode(id);
            if (node != null) {
                if (nodeObj.has("enabled")) {
                    node.setEnabled(nodeObj.get("enabled").getAsBoolean());
                }

                if (nodeObj.has("options") && !nodeObj.has("children")) {
                    try {
                        Detection detection = ctx.protectionManager().getDetection(id);
                        JsonObject options = nodeObj.getAsJsonObject("options");
                        for (Map.Entry<String, JsonElement> oe : options.entrySet()) {
                            Optional<ConfigOption<?>> option = detection.getOptions().stream()
                                    .filter(o -> o.key().equals(oe.getKey()))
                                    .findFirst();
                            if (option.isPresent()) {
                                applyJsonToOption(option.get(), oe.getValue());
                            } else {
                                LOGGER.warn("Unknown option '{}' for detection {}, skipping.", oe.getKey(), id);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to restore options for detection {}, skipping.", id, e);
                    }
                }

                if (nodeObj.has("actionBindings") && !nodeObj.has("children")) {
                    try {
                        Detection detection = ctx.protectionManager().getDetection(id);
                        JsonObject bindings = nodeObj.getAsJsonObject("actionBindings");
                        for (Map.Entry<String, JsonElement> be : bindings.entrySet()) {
                            ResourceLocation actionId = ResourceLocation.tryParse(be.getKey());
                            detection.setBindingEnabled(actionId, be.getValue().getAsBoolean());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to restore bindings for detection {}, skipping.", id, e);
                    }
                }

                if (nodeObj.has("actionOptions") && !nodeObj.has("children")) {
                    try {
                        Detection detection = ctx.protectionManager().getDetection(id);
                        JsonObject actionOptions = nodeObj.getAsJsonObject("actionOptions");
                        for (Map.Entry<String, JsonElement> ae : actionOptions.entrySet()) {
                            ResourceLocation actionId = ResourceLocation.tryParse(ae.getKey());
                            if (actionId == null || !(ae.getValue() instanceof JsonObject optsObj)) {
                                LOGGER.warn("Invalid actionOptions entry '{}' for detection {}, skipping.", ae.getKey(), id);
                                continue;
                            }
                            Optional<Action> action = detection.getBoundActions().stream()
                                    .filter(a -> a.getId().equals(actionId))
                                    .findFirst();
                            if (action.isEmpty()) {
                                LOGGER.warn("Detection {} has no bound action {}, skipping actionOptions.", id, actionId);
                                continue;
                            }
                            for (Map.Entry<String, JsonElement> oe : optsObj.entrySet()) {
                                Optional<ConfigOption<?>> option = action.get().getOptions().stream()
                                        .filter(ConfigOption::isPairScoped)
                                        .filter(o -> o.key().equals(oe.getKey()))
                                        .findFirst();
                                if (option.isPresent()) {
                                    applyJsonToOption(option.get(), oe.getValue());
                                } else {
                                    LOGGER.warn("Unknown pair-scoped option '{}' for action {} (detection {}), skipping.", oe.getKey(), actionId, id);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to restore actionOptions for detection {}, skipping.", id, e);
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

            ResourceLocation id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = ResourceLocation.tryParse(key);
                if (id == null) {
                    LOGGER.warn("Level 0 action root {} is not a valid Identifier, skipping.", key);
                    continue;
                }
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = ResourceLocation.tryBuild(namespace, newPath);
            }

            SwitchTreeNode node = ctx.protectionManager().getActionStatesRoot().getNode(id);
            if (node != null) {
                if (nodeObj.has("enabled")) {
                    node.setEnabled(nodeObj.get("enabled").getAsBoolean());
                }

                if (nodeObj.has("options") && !nodeObj.has("children")) {
                    List<Action> instances = ctx.protectionManager().getActionInstances(id);
                    if (instances.isEmpty()) {
                        LOGGER.warn("Action {} in config has options but no registered instances, skipping.", id);
                    } else {
                        JsonObject options = nodeObj.getAsJsonObject("options");
                        for (Map.Entry<String, JsonElement> oe : options.entrySet()) {
                            // 全局配置项按 ID 扇出到所有同 ID 的动作实例，保持它们的值同步
                            boolean found = false;
                            for (Action instance : instances) {
                                Optional<ConfigOption<?>> option = instance.getOptions().stream()
                                        .filter(o -> !o.isPairScoped())
                                        .filter(o -> o.key().equals(oe.getKey()))
                                        .findFirst();
                                if (option.isPresent()) {
                                    found = true;
                                    applyJsonToOption(option.get(), oe.getValue());
                                }
                            }
                            if (!found) LOGGER.warn("Unknown global option '{}' for action {}, skipping.", oe.getKey(), id);
                        }
                    }
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
            // 叶节点，至少有 `enabled`。
            "detection1": {
              "enabled": true,
              // `options`：该检测项自身的配置项（ConfigOption），键为选项键名，缺省时使用默认值。
              "options": {
                "someThreshold": 12
              },
              // `actionBindings`：与该检测项绑定的保护动作实例的开关状态。
              // 这种绑定是单向的，检测项是 **事实单例** 而保护动作不是，检测项到保护动作是一对多的关系。
              "actionBindings": {
                "namespace1:category3/action1": true,
                "namespace1:category3/category4/action2": true
              },
              // `actionOptions`：检测项-动作对专属的配置项（如描边/高亮颜色）。
              // 键为完整动作 ID，值为该动作实例上标记为 pairScoped() 的配置项集合。
              // 未在此声明的同 ID 动作配置项，走 action 树下对应叶节点的全局 `options`。
              "actionOptions": {
                "namespace1:category3/action1": {
                  "color": "#66FF4500"
                }
              }
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
          // `options`：该动作 ID 的全局配置项（非 pairScoped）。
          // 由于同一动作类可能被多个检测项各自 new 一份实例，全局配置项按 ID 在这些实例间保持一致，
          // 加载时会扇出到全部实例。
          "options": {
            "minFallDistance": 3.0
          }
        },
        "category4": {
          "enabled": true,
          "children": {
            "action2": {
              "enabled": true
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
