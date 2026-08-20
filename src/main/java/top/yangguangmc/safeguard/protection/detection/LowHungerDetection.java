package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.protection.option.IntOption;
import top.yangguangmc.safeguard.util.Utils;

import java.util.Objects;
import java.util.function.Predicate;

public class LowHungerDetection extends Detection {
    private final IntOption findFoodThreshold = registerOption(IntOption.of("findFoodThreshold", 12).range(1, 20));
    private final IntOption lowHungerThreshold = registerOption(IntOption.of("lowHungerThreshold", 6).range(1, 20));

    public LowHungerDetection() {
        super("status/low_hunger", new ActionBarTitleAction(), new PauseAction(), new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(Minecraft client, ClientLevel world, LocalPlayer player) {
        int foodLevel = player.getFoodData().getFoodLevel();
        if (foodLevel > findFoodThreshold.get()) return;
        int lowThreshold = lowHungerThreshold.get();
        FoodScorer.FoodResult result = FoodScorer.findBestFood(player, lowThreshold);
        boolean isLow = foodLevel < lowThreshold;
        boolean healingPriority = player.getHealth() <= player.getMaxHealth() * FoodScorer.HALF_HEALTH_RATIO;
        // 饥饿极低或血量告急时始终显示；正常饥饿值下仅当背包有多种食物选择时才推荐
        boolean shouldShow = isLow || healingPriority || result.foodCount() > 1;
        if (shouldShow) {
            MutableComponent message = isLow
                    ? Component.translatable("detection.safeguard.status.low_hunger.low")
                    : Component.translatable("detection.safeguard.status.low_hunger.replenish");
            if (result.isFound())
                message.append(Component.translatable("detection.safeguard.status.low_hunger.suggestion"))
                        .append(result.name()).append(Utils.getInventoryPosIndicator(result.slot()));
            DangerLevel level = isLow ? DangerLevel.LOW : DangerLevel.INFO;
            tryExecuteAction(ActionBarTitleAction.class, action ->
                    action.updateTitle(level, message, client));
        }
        if (isLow) {
            tryExecuteAction(PauseAction.class, action -> action.pause(client));
            tryExecuteAction(QuitAction.class, action -> action.quit(client));
        }
    }

    /**
     * 食物评分器。根据玩家当前饥饿值、饱食度和血量，为背包中每种可食物品评分，选出最优食物。
     * <p>
     * 评分分为两种模式：
     * <ul>
     *   <li><b>效率优先</b>（血量 > 50%）: score = effectiveNutrition + effectiveSaturation × 1.5，尽量避免浪费</li>
     *   <li><b>回血优先</b>（血量 ≤ 50%）: score = effectiveSaturation × 3.0 + effectiveNutrition，优先把饱食度拉满触发快速回血</li>
     * </ul>
     * <p>
     * effectiveNutrition 和 effectiveSaturation 分别表示该食物实际能提供的饥饿值/饱食度增量（扣除因上限夹逼造成的浪费部分）。
     */
    private static final class FoodScorer {
        private static final float HALF_HEALTH_RATIO = 0.5F;

        private static final Predicate<ItemStack> NORMAL_BLACKLIST = stack -> {
            Item item = stack.getItem();
            return item == Items.SPIDER_EYE
                    || item == Items.POISONOUS_POTATO
                    || item == Items.PUFFERFISH
                    || item == Items.ROTTEN_FLESH
                    || item == Items.CHICKEN;
        };

        private static final Predicate<ItemStack> LOW_BLACKLIST = stack -> {
            Item item = stack.getItem();
            return item == Items.SPIDER_EYE
                    || item == Items.POISONOUS_POTATO
                    || item == Items.PUFFERFISH;
        };

        private static boolean isValidFood(ItemStack stack, Predicate<ItemStack> blacklist) {
            return stack.get(DataComponents.FOOD) != null && !blacklist.test(stack);
        }

        /**
         * 在背包（含副手）中搜索最佳食物。
         *
         * @return 评分最高的食物结果，若无可食用品返回 {@link FoodResult#NOT_FOUND}
         */
        static FoodResult findBestFood(LocalPlayer player, int lowHungerThreshold) {
            FoodData hm = player.getFoodData();
            int currentFood = hm.getFoodLevel();
            float currentSat = hm.getSaturationLevel();
            boolean healingPriority = player.getHealth() <= player.getMaxHealth() * HALF_HEALTH_RATIO;
            boolean bothLow = healingPriority && currentFood <= lowHungerThreshold;
            Predicate<ItemStack> blacklist = bothLow ? LOW_BLACKLIST : NORMAL_BLACKLIST;

            Inventory inv = player.getInventory();
            FoodResult best = FoodResult.NOT_FOUND;
            double bestScore = Double.NEGATIVE_INFINITY;
            int foodCount = 0;

            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                ItemStack stack = inv.getItem(i);
                if (!isValidFood(stack, blacklist)) continue;
                foodCount++;
                best = evaluate(stack, i, currentFood, currentSat, healingPriority, best, bestScore);
                if (best.score() > bestScore) bestScore = best.score();
            }
            ItemStack offHand = inv.getItem(Inventory.SLOT_OFFHAND);
            if (isValidFood(offHand, blacklist)) {
                foodCount++;
                best = evaluate(offHand, Inventory.SLOT_OFFHAND, currentFood, currentSat, healingPriority, best, bestScore);
            }

            return best.isFound() ? best.withCountAndHealing(foodCount, healingPriority) : best;
        }

        private static FoodResult evaluate(ItemStack stack, int slot, int currentFood, float currentSat,
                                           boolean healingPriority,
                                           FoodResult currentBest, double currentBestScore) {
            FoodProperties food = stack.get(DataComponents.FOOD);
            assert food != null;

            double totalNutrition = food.nutrition();
            double totalSat = food.saturation();

            // 迷之炖菜的额外饱和效果
            if (stack.is(Items.SUSPICIOUS_STEW)) {
                SuspiciousStewEffects stewEffects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
                if (stewEffects != null) {
                    for (SuspiciousStewEffects.Entry effect : stewEffects.effects()) {
                        if (Objects.equals(effect.effect().value(), MobEffects.SATURATION.value())) {
                            // saturation 效果每 tick 调用 HungerManager.add(amplifier+1, 1.0F)
                            int amplifier = 0;
                            int ticks = effect.duration();
                            totalNutrition += (amplifier + 1.0) * ticks;
                            totalSat += 2.0 * ticks;
                        }
                    }
                }
            }

            // 计算实际增量（扣除因上限夹逼造成的浪费）
            int room = 20 - currentFood;
            double effectiveNutrition = Math.min(totalNutrition, room);
            double newFoodLevel = currentFood + effectiveNutrition;
            double effectiveSat = Math.min(currentSat + totalSat, newFoodLevel) - currentSat;

            // 评分
            double score;
            if (healingPriority) {
                score = effectiveSat * 3.0 + effectiveNutrition;
            } else {
                score = effectiveNutrition + effectiveSat * 1.5;
            }

            if (score > currentBestScore) {
                Component name = stack.getHoverName().copy();
                return new FoodResult(name, slot, score, 0, false);
            }
            return currentBest;
        }

        /**
         * 食物评分结果，同时携带评分用于内部比较和推荐门控所需的元数据。
         */
        private record FoodResult(Component name, int slot, double score, int foodCount, boolean healingPriority) {
            static final FoodResult NOT_FOUND = new FoodResult(null, -1, Double.NEGATIVE_INFINITY, 0, false);

            boolean isFound() {
                return name != null;
            }

            /**
             * 由 {@link FoodScorer#findBestFood} 在遍历完毕后调用，注入计数与血量状态。
             */
            FoodResult withCountAndHealing(int foodCount, boolean healingPriority) {
                return new FoodResult(this.name, this.slot, this.score, foodCount, healingPriority);
            }
        }
    }
}