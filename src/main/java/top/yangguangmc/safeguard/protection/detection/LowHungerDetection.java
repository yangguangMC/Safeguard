package top.yangguangmc.safeguard.protection.detection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import top.yangguangmc.safeguard.protection.action.ActionBarTitleAction;
import top.yangguangmc.safeguard.protection.action.DangerLevel;
import top.yangguangmc.safeguard.protection.action.PauseAction;
import top.yangguangmc.safeguard.protection.action.QuitAction;
import top.yangguangmc.safeguard.protection.event.ClientPlayerTickEvents;
import top.yangguangmc.safeguard.util.Utils;

import java.util.List;
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
        boolean shouldShow = isLow || healingPriority || result.foodCount() > 1;
        if (shouldShow) {
            MutableText message = isLow
                    ? Text.translatable("detection.safeguard.status.low_hunger.low")
                    : Text.translatable("detection.safeguard.status.low_hunger.replenish");
            if (result.isFound())
                message.append(Text.translatable("detection.safeguard.status.low_hunger.suggestion"))
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

    private static final class FoodScorer {
        private static final float HALF_HEALTH_RATIO = 0.5F;

        // 1.20.6: Use Item.isFood() instead of DataComponentTypes
        private static boolean isFood(ItemStack stack) {
            return stack.getItem().isFood();
        }

        private static FoodComponent getFoodComponent(ItemStack stack) {
            return stack.getItem().getFoodComponent();
        }

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
            return isFood(stack) && !blacklist.test(stack);
        }

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
            FoodComponent food = getFoodComponent(stack);
            if (food == null) return currentBest;

            double totalNutrition = food.getHunger();
            double totalSat = food.getSaturationModifier() * 2.0F * food.getHunger();

            // 1.20.6: Suspicious Stew effects via PotionUtil.getPotionEffects()
            if (stack.isOf(Items.SUSPICIOUS_STEW)) {
                List<StatusEffectInstance> stewEffects = PotionUtil.getPotionEffects(stack);
                for (StatusEffectInstance effect : stewEffects) {
                    if (effect.getEffectType() == StatusEffects.SATURATION) {
                        int amplifier = effect.getAmplifier();
                        int ticks = effect.getDuration();
                        totalNutrition += (amplifier + 1.0) * ticks;
                        totalSat += 2.0 * ticks;
                    }
                }
            }

            int room = 20 - currentFood;
            double effectiveNutrition = Math.min(totalNutrition, room);
            double newFoodLevel = currentFood + effectiveNutrition;
            double effectiveSat = Math.min(currentSat + totalSat, newFoodLevel) - currentSat;

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

        private record FoodResult(Text name, int slot, double score, int foodCount, boolean healingPriority) {
            static final FoodResult NOT_FOUND = new FoodResult(null, -1, Double.NEGATIVE_INFINITY, 0, false);

            boolean isFound() { return name != null; }

            FoodResult withCountAndHealing(int foodCount, boolean healingPriority) {
                return new FoodResult(this.name, this.slot, this.score, foodCount, healingPriority);
            }
        }
    }
}
