package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.attribute.EnvironmentAttributes;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

import java.util.List;
import java.util.function.Predicate;

public class OnFireDetection extends Detection {
    private static final List<Strategy> STRATEGIES = List.of(
            // 1. 喷溅型抗火药水
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.splash_fire_resistance"), Items.SPLASH_POTION,
                    contents -> contents.matches(Potions.FIRE_RESISTANCE)
                            || contents.matches(Potions.LONG_FIRE_RESISTANCE),
                    (world, player) -> true),
            // 2. 滞留型抗火药水
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.lingering_fire_resistance"), Items.LINGERING_POTION,
                    contents -> contents.matches(Potions.FIRE_RESISTANCE)
                            || contents.matches(Potions.LONG_FIRE_RESISTANCE),
                    (world, player) -> true),
            // 3. 普通抗火药水
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.fire_resistance"), Items.POTION,
                    contents -> contents.matches(Potions.FIRE_RESISTANCE)
                            || contents.matches(Potions.LONG_FIRE_RESISTANCE),
                    (world, player) -> true),
            // 4. 喷溅型水瓶
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.splash_water"), Items.SPLASH_POTION,
                    contents -> contents.matches(Potions.WATER),
                    (world, player) -> true),
            // 5. 滞留型水瓶
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.lingering_water"), Items.LINGERING_POTION,
                    contents -> contents.matches(Potions.WATER),
                    (world, player) -> true),
            // 6. 水桶
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.water_bucket"), Items.WATER_BUCKET,
                    (world, player) ->
                            !world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY)),
            // 7. 细雪桶
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.powder_snow_bucket"), Items.POWDER_SNOW_BUCKET,
                    (world, player) -> true),
            // 8. 炼药锅
            Strategy.cauldron(Text.translatable("item.safeguard.fire_strategy.cauldron")),
            // 9. 附魔金苹果
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.enchanted_golden_apple"), Items.ENCHANTED_GOLDEN_APPLE,
                    (world, player) -> true),
            // 10. 不死图腾
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.totem_of_undying"), Items.TOTEM_OF_UNDYING,
                    (world, player) -> true)
    );

    public OnFireDetection() {
        super("environment/on_fire", new ActionBarTitleAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        if (!player.isOnFire() || player.isFireImmune() || player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE))
            return;
        StrategyResult result = findBestStrategy(world, player);

        MutableText message = Text.translatable("detection.safeguard.environment.on_fire.warning");
        if (result.isFound()) {
            message.append(Text.translatable("detection.safeguard.environment.on_fire.suggestion")).append(result.displayName());
            if (result.slot() < PlayerInventory.HOTBAR_SIZE)
                message.append(Text.translatable("gui.safeguard.slot.hotbar", result.slot() + 1));
            else if (result.slot() < PlayerInventory.MAIN_SIZE)
                message.append(Text.translatable("gui.safeguard.slot.inventory"));
            else if (result.slot() == PlayerInventory.OFF_HAND_SLOT)
                message.append(Text.translatable("gui.safeguard.slot.offhand"));
        }

        DangerLevel level = result.isFound() ? DangerLevel.MEDIUM : DangerLevel.HIGH;
        tryExecuteAction(ActionBarTitleAction.class,
                action -> action.updateTitle(level, message, client));
    }

    private StrategyResult findBestStrategy(ClientWorld world, ClientPlayerEntity player) {
        for (Strategy strategy : STRATEGIES) {
            if (!strategy.dimensionCheck().test(world, player)) continue;

            int slot = strategy.searchInventory(player);
            if (slot >= 0) return new StrategyResult(strategy.displayName(), slot);
        }
        return StrategyResult.NOT_FOUND;
    }

    /**
     * 灭火/防火策略定义。
     *
     * @param displayName     策略展示名（现已使用 Text.translatable() 国际化）
     * @param inventorySearch 背包搜索函数，返回找到的物品所在槽位索引，-1 表示未找到
     * @param dimensionCheck  维度有效性检查
     */
    private record Strategy(
            Text displayName,
            InventorySearch inventorySearch,
            DimensionCheck dimensionCheck
    ) {
        static Strategy direct(Text name, Item item, DimensionCheck dimCheck) {
            return new Strategy(name,
                    player -> searchItem(player.getInventory(), item),
                    dimCheck);
        }

        static Strategy potion(Text name, Item container, Predicate<PotionContentsComponent> potionMatcher,
                               DimensionCheck dimCheck) {
            return new Strategy(name,
                    player -> searchPotion(player.getInventory(), container, potionMatcher),
                    dimCheck);
        }

        static Strategy cauldron(Text name) {
            return new Strategy(name,
                    player -> searchCauldron(player.getInventory()),
                    (world, player) -> true);
        }

        int searchInventory(ClientPlayerEntity player) {
            return inventorySearch.find(player);
        }
    }

    @FunctionalInterface
    private interface InventorySearch {
        int find(ClientPlayerEntity player);
    }

    @FunctionalInterface
    private interface DimensionCheck {
        boolean test(ClientWorld world, ClientPlayerEntity player);
    }

    /**
     * 策略搜索结果。NOT_FOUND 表示无可用策略。
     */
    private record StrategyResult(Text displayName, int slot) {
        static final StrategyResult NOT_FOUND = new StrategyResult(null, -1);

        boolean isFound() {
            return displayName != null;
        }
    }

    // ======================== 背包搜索工具方法 ========================

    /**
     * 在玩家背包（含副手）中搜索指定物品。
     *
     * @return 物品所在槽位索引（0-35 主背包，40 副手），未找到返回 -1
     */
    private static int searchItem(PlayerInventory inventory, Item target) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            if (inventory.getStack(i).isOf(target)) return i;
        }
        if (inventory.getStack(PlayerInventory.OFF_HAND_SLOT).isOf(target)) return PlayerInventory.OFF_HAND_SLOT;
        return -1;
    }

    /**
     * 在玩家背包（含副手）中搜索指定容器类型且药水内容匹配的物品。
     *
     * @return 物品所在槽位索引，未找到返回 -1
     */
    private static int searchPotion(PlayerInventory inventory, Item container,
                                    Predicate<PotionContentsComponent> potionMatcher) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(container) && matchesPotion(stack, potionMatcher)) return i;
        }
        ItemStack offHandStack = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);
        if (offHandStack.isOf(container) && matchesPotion(offHandStack, potionMatcher))
            return PlayerInventory.OFF_HAND_SLOT;
        return -1;
    }

    private static boolean matchesPotion(ItemStack stack, Predicate<PotionContentsComponent> matcher) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return contents != null && matcher.test(contents);
    }

    /**
     * 搜索炼药锅策略：背包中同时存在炼药锅 + 任意水容器（水桶/水瓶类）。
     * 水瓶类包括普通水瓶、喷溅水瓶、滞留水瓶（药水内容为水）。
     *
     * @return 炼药锅所在槽位（因为展示名是"炼药锅"），未找到返回 -1
     */
    private static int searchCauldron(PlayerInventory inventory) {
        int cauldronSlot = searchItem(inventory, Items.CAULDRON);
        if (cauldronSlot < 0) return -1;

        boolean hasWaterSource = searchItem(inventory, Items.WATER_BUCKET) >= 0
                || searchPotion(inventory, Items.POTION, c -> c.matches(Potions.WATER)) >= 0
                || searchPotion(inventory, Items.SPLASH_POTION, c -> c.matches(Potions.WATER)) >= 0
                || searchPotion(inventory, Items.LINGERING_POTION, c -> c.matches(Potions.WATER)) >= 0;

        return hasWaterSource ? cauldronSlot : -1;
    }
}