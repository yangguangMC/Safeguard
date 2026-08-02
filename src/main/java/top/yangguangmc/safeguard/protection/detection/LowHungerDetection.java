package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;

import java.util.Objects;
import java.util.function.Predicate;

public class LowHungerDetection extends Detection {
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int findFoodThreshold = 12;
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private int lowHungerThreshold = 6;

    public LowHungerDetection() {
        super("status/low_hunger", new ActionBarTitleAction(), new PauseAction(), new QuitAction());
        listen(ClientPlayerTickEvents.GATED_START_TICK, this::onStartTick);
    }

    private void onStartTick(MinecraftClient client, ClientWorld world, ClientPlayerEntity player) {
        int foodLevel = player.getHungerManager().getFoodLevel();
        if (foodLevel > findFoodThreshold) return;
        FoodScorer.FoodResult result = FoodScorer.findBestFood(player, lowHungerThreshold);
        boolean isLow = foodLevel < lowHungerThreshold;
        boolean healingPriority = player.getHealth() <= player.getMaxHealth() * FoodScorer.HALF_HEALTH_RATIO;
        // 饥饿极低或血量告急时始终显示；正常饥饿值下仅当背包有多种食物选择时才推荐
        boolean shouldShow = isLow || healingPriority || result.foodCount() > 1;
        if (shouldShow) {
            MutableText message = isLow
                    ? Text.translatable("detection.safeguard.status.low_hunger.low")
                    : Text.translatable("detection.safeguard.status.low_hunger.replenish");
            if (result.isFound()) {
                message.append(Text.translatable("detection.safeguard.status.low_hunger.suggestion")).append(result.name());
                if (result.slot() < PlayerInventory.HOTBAR_SIZE)
                    message.append(Text.translatable("gui.safeguard.slot.hotbar", result.slot() + 1));
                else if (result.slot() < PlayerInventory.MAIN_SIZE)
                    message.append(Text.translatable("gui.safeguard.slot.inventory"));
                else if (result.slot() == PlayerInventory.OFF_HAND_SLOT)
                    message.append(Text.translatable("gui.safeguard.slot.offhand"));
            }
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
            return stack.get(DataComponentTypes.FOOD) != null && !blacklist.test(stack);
        }

        /**
         * 在背包（含副手）中搜索最佳食物。
         *
         * @return 评分最高的食物结果，若无可食用品返回 {@link FoodResult#NOT_FOUND}
         */
        static FoodResult findBestFood(ClientPlayerEntity player, int lowHungerThreshold) {
            HungerManager hm = player.getHungerManager();
            int currentFood = hm.getFoodLevel();
            float currentSat = hm.getSaturationLevel();
            boolean healingPriority = player.getHealth() <= player.getMaxHealth() * HALF_HEALTH_RATIO;
            boolean bothLow = healingPriority && currentFood <= lowHungerThreshold;
            Predicate<ItemStack> blacklist = bothLow ? LOW_BLACKLIST : NORMAL_BLACKLIST;

            PlayerInventory inv = player.getInventory();
            FoodResult best = FoodResult.NOT_FOUND;
            double bestScore = Double.NEGATIVE_INFINITY;
            int foodCount = 0;

            for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
                ItemStack stack = inv.getStack(i);
                if (!isValidFood(stack, blacklist)) continue;
                foodCount++;
                best = evaluate(stack, i, currentFood, currentSat, healingPriority, best, bestScore);
                if (best.score() > bestScore) bestScore = best.score();
            }
            ItemStack offHand = inv.getStack(PlayerInventory.OFF_HAND_SLOT);
            if (isValidFood(offHand, blacklist)) {
                foodCount++;
                best = evaluate(offHand, PlayerInventory.OFF_HAND_SLOT, currentFood, currentSat, healingPriority, best, bestScore);
            }

            return best.isFound() ? best.withCountAndHealing(foodCount, healingPriority) : best;
        }

        private static FoodResult evaluate(ItemStack stack, int slot, int currentFood, float currentSat,
                                           boolean healingPriority,
                                           FoodResult currentBest, double currentBestScore) {
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            assert food != null;

            double totalNutrition = food.nutrition();
            double totalSat = food.saturation();

            // 迷之炖菜的额外饱和效果
            if (stack.isOf(Items.SUSPICIOUS_STEW)) {
                SuspiciousStewEffectsComponent stewEffects = stack.get(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
                if (stewEffects != null) {
                    for (SuspiciousStewEffectsComponent.StewEffect effect : stewEffects.effects()) {
                        if (Objects.equals(effect.effect().value(), StatusEffects.SATURATION.value())) {
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
                Text name = stack.getName().copy();
                return new FoodResult(name, slot, score, 0, false);
            }
            return currentBest;
        }

        /**
         * 食物评分结果，同时携带评分用于内部比较和推荐门控所需的元数据。
         */
        private record FoodResult(Text name, int slot, double score, int foodCount, boolean healingPriority) {
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