package top.yangguangmc.safeguard.gui.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.SwitchTreeNode;
import top.yangguangmc.safeguard.protection.action.Action;
import top.yangguangmc.safeguard.protection.detection.Detection;

public class ConfigScreen {
    private static ModContext ctx;

    public static void init(ModContext ctx) {
        ConfigScreen.ctx = ctx;
    }

    @SuppressWarnings("SimplifiableConditionalExpression")
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("screen.safeguard.config").styled(style -> style.withColor(Formatting.GREEN)));

        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Default"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        SubCategoryBuilder subCategoryBuilder1 = entryBuilder.startSubCategory(Text.literal("检测项"));
        SwitchTreeNode root1 = ctx.protectionManager().getDetectionStatesRoot();
        for (Identifier id : root1.getNodeIds()) {
            SwitchTreeNode node1 = root1.getNode(id);
            subCategoryBuilder1.add(entryBuilder.startBooleanToggle(ctx.protectionManager().getDetectionName(id), node1.isEnabled())
                    .setTooltip(Text.literal("默认值：" + (node1.isLeaf() ? ctx.protectionManager().getDetectionDefaultState(id) : true)), Text.literal(id.toString()))
                    .setSaveConsumer(node1::setEnabled)
                    .build());
        }
        category.addEntry(subCategoryBuilder1.build());

        SubCategoryBuilder subCategoryBuilder2 = entryBuilder.startSubCategory(Text.literal("保护动作"));
        SwitchTreeNode root2 = ctx.protectionManager().getActionStatesRoot();
        for (Identifier id : root2.getNodeIds()) {
            SwitchTreeNode node = root2.getNode(id);
            subCategoryBuilder2.add(entryBuilder.startBooleanToggle(ctx.protectionManager().getActonName(id), node.isEnabled())
                    .setTooltip(Text.literal("默认值：" + (node.isLeaf() ? ctx.protectionManager().getActionDefaultState(id) : true)), Text.literal(id.toString()))
                    .setSaveConsumer(node::setEnabled)
                    .build());
        }
        category.addEntry(subCategoryBuilder2.build());

        SubCategoryBuilder subCategoryBuilder3 = entryBuilder.startSubCategory(Text.literal("链接"));
        for (Identifier id : ctx.protectionManager().getDetectionStatesRoot().getNodeIds()) {
            if (!ctx.protectionManager().getDetectionStatesRoot().getNode(id).isLeaf()) continue;
            Detection detection = ctx.protectionManager().getDetection(id);
            for (Action action : detection.getBoundActions()) {
                subCategoryBuilder3.add(entryBuilder.startBooleanToggle(ctx.protectionManager().getDetectionName(id).copy().append(" --> ").append(ctx.protectionManager().getActonName(action.getId())), detection.isBindingEnabled(action.getId()))
                        .setTooltip(Text.literal("默认值：true"), Text.literal(detection.getId().toString()).append(" --> ").append(action.getId().toString()))
                        .setSaveConsumer(enabled -> detection.setBindingEnabled(action.getId(), enabled))
                        .build());
            }
        }
        category.addEntry(subCategoryBuilder3.build());

        return builder.build();
    }
}
