package top.yangguangmc.safeguard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.platform.YACLPlatform;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigManager {
    private ModContext ctx;

    public void init(ModContext ctx) {
        this.ctx = ctx;
    }

    public void save() throws IOException {
        // FIXME 这个实现不能正常工作，同时在设计上和Bug上。
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = new JsonObject();
        JsonObject detectionJson = new JsonObject();
        buildTree(detectionJson, ctx.protectionManager().getDetectionStatesRoot());
        json.add("detection", detectionJson);
        JsonObject actionJson = new JsonObject();
        buildTree(actionJson, ctx.protectionManager().getActionStatesRoot());
        json.add("action", actionJson);
        JsonObject linkJson = new JsonObject();
        ctx.protectionManager().getDetectionStatesRoot().getNodeIds()
                .stream()
                .filter(id -> ctx.protectionManager().getDetectionStatesRoot().getNode(id).isLeaf())
                .map(id -> ctx.protectionManager().getDetection(id))
                .flatMap(detection -> detection.getBoundActions().stream())
                .forEach(action -> linkJson.addProperty(action.getParent().getId() + "->" + action.getId(), action.getParent().isBindingEnabled(action.getId())));
        json.add("link", linkJson);
        Files.writeString(getConfig(), gson.toJson(json), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void load() {
    }

    private void buildTree(JsonObject obj, SwitchTreeNode nodeNotLeaf) {
        for (SwitchTreeNode child : nodeNotLeaf.getChildren()) {
            if (child.isLeaf()) {
                obj.addProperty(child.getIdName(), child.isEnabled());
            } else {
                obj.addProperty(child.getIdName() + "/", child.isEnabled());
                JsonObject childObj = new JsonObject();
                obj.add(child.getIdName(), childObj);
                buildTree(childObj, child);
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
