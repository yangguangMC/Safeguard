package top.yangguangmc.safeguard;

import com.google.gson.*;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.resources.ResourceLocation;
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
}
