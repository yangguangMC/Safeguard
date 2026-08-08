package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.List;
import java.util.function.Predicate;

public class OnFireDetection extends Detection {
    private static final List<Strategy> STRATEGIES = List.of(
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.splash_fire_resistance"), Items.SPLASH_POTION,
                    potion -> potion == Potions.FIRE_RESISTANCE || potion == Potions.LONG_FIRE_RESISTANCE,
                    (world, player) -> true),
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.lingering_fire_resistance"), Items.LINGERING_POTION,
                    potion -> potion == Potions.FIRE_RESISTANCE || potion == Potions.LONG_FIRE_RESISTANCE,
                    (world, player) -> true),
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.fire_resistance"), Items.POTION,
                    potion -> potion == Potions.FIRE_RESISTANCE || potion == Potions.LONG_FIRE_RESISTANCE,
                    (world, player) -> true),
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.splash_water"), Items.SPLASH_POTION,
                    potion -> potion == Potions.WATER,
                    (world, player) -> true),
            Strategy.potion(Text.translatable("item.safeguard.fire_strategy.lingering_water"), Items.LINGERING_POTION,
                    potion -> potion == Potions.WATER,
                    (world, player) -> true),
            // 1.20.6: ultrawarm() instead of EnvironmentAttributes
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.water_bucket"), Items.WATER_BUCKET,
                    (world, player) -> !world.getDimension().ultrawarm()),
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.powder_snow_bucket"), Items.POWDER_SNOW_BUCKET,
                    (world, player) -> true),
            Strategy.cauldron(Text.translatable("item.safeguard.fire_strategy.cauldron")),
            Strategy.direct(Text.translatable("item.safeguard.fire_strategy.enchanted_golden_apple"), Items.ENCHANTED_GOLDEN_APPLE,
                    (world, player) -> true),
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
        if (result.isFound())
            message.append(Text.translatable("detection.safeguard.environment.on_fire.suggestion"))
                    .append(result.displayName()).append(Utils.getInventoryPosIndicator(result.slot()));

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

        // 1.20.6: Use Potion instead of PotionContentsComponent
        static Strategy potion(Text name, Item container, Predicate<Potion> potionMatcher,
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

    private record StrategyResult(Text displayName, int slot) {
        static final StrategyResult NOT_FOUND = new StrategyResult(null, -1);
        boolean isFound() { return displayName != null; }
    }

    private static int searchItem(PlayerInventory inventory, Item target) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            if (inventory.getStack(i).isOf(target)) return i;
        }
        if (inventory.getStack(PlayerInventory.OFF_HAND_SLOT).isOf(target)) return PlayerInventory.OFF_HAND_SLOT;
        return -1;
    }

    // 1.20.6: PotionUtil.getPotion() instead of DataComponentTypes.POTION_CONTENTS
    private static int searchPotion(PlayerInventory inventory, Item container,
                                    Predicate<Potion> potionMatcher) {
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(container) && matchesPotion(stack, potionMatcher)) return i;
        }
        ItemStack offHandStack = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);
        if (offHandStack.isOf(container) && matchesPotion(offHandStack, potionMatcher))
            return PlayerInventory.OFF_HAND_SLOT;
        return -1;
    }

    private static boolean matchesPotion(ItemStack stack, Predicate<Potion> matcher) {
        Potion potion = PotionUtil.getPotion(stack);
        return potion != null && matcher.test(potion);
    }

    private static int searchCauldron(PlayerInventory inventory) {
        int cauldronSlot = searchItem(inventory, Items.CAULDRON);
        if (cauldronSlot < 0) return -1;
        boolean hasWaterSource = searchItem(inventory, Items.WATER_BUCKET) >= 0
                || searchPotion(inventory, Items.POTION, c -> c == Potions.WATER) >= 0
                || searchPotion(inventory, Items.SPLASH_POTION, c -> c == Potions.WATER) >= 0
                || searchPotion(inventory, Items.LINGERING_POTION, c -> c == Potions.WATER) >= 0;
        return hasWaterSource ? cauldronSlot : -1;
    }
}
