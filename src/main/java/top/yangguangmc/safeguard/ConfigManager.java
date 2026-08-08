package top.yangguangmc.safeguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.util.Identifier;
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
    private ModContext ctx;

    public void init(ModContext ctx) {
        this.ctx = ctx;
    }

    public void trySave() {
        try {
            save();
        } catch (Exception e) {
            LOGGER.error("Could not save config file!", e);
            LOGGER.info("Trying to backup original file and retry saving...");
            try {
                backupAndRename();
                save();
            } catch (Exception ex) {
                LOGGER.error("Could not save config file during backup or after retrying!", ex);
            }
        }
    }

    public void save() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = new JsonObject();

        JsonObject detectionJson = new JsonObject();
        buildDetectionTree(detectionJson, ctx.protectionManager().getDetectionStatesRoot(), true);
        json.add("detection", detectionJson);

        JsonObject actionJson = new JsonObject();
        buildActionTree(actionJson, ctx.protectionManager().getActionStatesRoot(), true);
        json.add("action", actionJson);

        Files.writeString(getConfig(), gson.toJson(json), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
            LOGGER.info("Trying to backup original file...");
            try {
                backupAndRename();
            } catch (Exception ex) {
                LOGGER.error("Failed to backup original config file!", ex);
            }
        }
    }

    public void load() throws IOException {
        String content = Files.readString(getConfig());
        JsonObject json = new Gson().fromJson(content, JsonObject.class);

        JsonObject dj = json.getAsJsonObject("detection");
        if (dj != null) loadDetectionTree(dj, "", "");

        JsonObject aj = json.getAsJsonObject("action");
        if (aj != null) loadActionTree(aj, "", "");
        LOGGER.debug("Successfully loaded config from '{}'.", getConfig());
    }

    private void backupAndRename() throws IOException {
        Path config = getConfig();
        if (Files.exists(config)) {
            Path target = YACLPlatform.getConfigDir().resolve(ModContext.MOD_ID + ".json.backup");
            Files.copy(config, target, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(config);
            LOGGER.info("Successfully backed config up at '{}'.", target);
        }
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
            if (!(entry.getValue() instanceof JsonObject nodeObj)) continue;

            Identifier id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = Identifier.tryParse(key);
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = Identifier.of(namespace, newPath);
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
                            Identifier actionId = Identifier.tryParse(be.getKey());
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
            if (!(entry.getValue() instanceof JsonObject nodeObj)) continue;

            Identifier id;
            String ns = namespace;
            String newPath;

            if (parentPath.isEmpty()) {
                id = Identifier.tryParse(key);
                ns = id.getNamespace();
                newPath = id.getPath();
            } else {
                newPath = parentPath + "/" + key;
                id = Identifier.of(namespace, newPath);
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
