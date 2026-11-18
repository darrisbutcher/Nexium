package net.nexium.essentials.backpack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Feeding module, on the server. While a Backpack with the module in and on is worn, it eats for the player: once a
 * second it looks at how much hunger is missing and eats the food in the Backpack that restores the most without any of it
 * being wasted. Foods with side effects (rotten flesh, pufferfish, golden apples, chorus fruit and the like) are never eaten.
 * It does nothing in creative mode, and works while the Backpack's screen is open too (the food on the screen is eaten).
 */
public final class BackpackFeeding {
    /** The food bar's size: what is missing is this less the food level. */
    private static final int FULL = 20;
    private static final int INTERVAL = 20;

    /** One food in the Backpack, as far as picking goes. */
    public record Food(int slot, int nutrition, float saturation) {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if ((tick + player.getId()) % INTERVAL == 0) feed(player);
            }
        });
    }

    /**
     * Which food to eat when this much hunger is missing: the one that restores the most without restoring more than is
     * missing, the one with more saturation if two restore the same, and the earlier slot after that. Returns its slot, or
     * -1 if nothing fits (so nothing is eaten).
     */
    public static int pick(int missing, List<Food> foods) {
        Food best = null;
        for (Food food : foods) {
            if (food.nutrition() <= 0 || food.nutrition() > missing) continue;
            if (best == null || food.nutrition() > best.nutrition()
                    || (food.nutrition() == best.nutrition() && food.saturation() > best.saturation())) best = food;
        }
        return best == null ? -1 : best.slot();
    }

    /**
     * The food in this stack if it is safe to eat for you: edible, and with no side effect when eaten. Null otherwise.
     * Eating applies the food itself, the consume effects of the item (rotten flesh's hunger, a golden apple's regeneration,
     * a chorus fruit's teleport) and every other component that acts when eaten (a suspicious stew's effects): only a food
     * with none of those is safe.
     */
    public static @Nullable FoodProperties safeFood(ItemStack stack) {
        if (stack.isEmpty()) return null;
        FoodProperties food = stack.get(DataComponents.FOOD);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (food == null || consumable == null || food.nutrition() <= 0 || !consumable.onConsumeEffects().isEmpty()) return null;
        for (TypedDataComponent<?> component : stack.getComponents()) {
            if (component.value() instanceof ConsumableListener && !(component.value() instanceof FoodProperties)) return null;
        }
        return food;
    }

    /** Eats once if the worn Backpack's Feeding module is on and the player is hungry enough for something in it. Returns whether something was eaten. */
    public static boolean feed(ServerPlayer player) {
        // The game mode, not isCreative(): they agree for players, and the game mode is what a hunger bar follows
        if (player.gameMode.getGameModeForPlayer().isCreative()) return false;
        ItemStack backpack = Backpacks.activeFor(player, BackpackModuleType.FEEDING);
        if (backpack.isEmpty()) return false;
        int missing = FULL - player.getFoodData().getFoodLevel();
        if (missing <= 0) return false;

        NonNullList<ItemStack> slots = Backpacks.read(player, backpack);
        List<Food> foods = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            FoodProperties food = safeFood(slots.get(i));
            if (food != null) foods.add(new Food(i, food.nutrition(), food.saturation()));
        }
        int slot = pick(missing, foods);
        if (slot < 0) return false;

        // Eat one: the game applies the food, plays the sound and fires the eat event; a bowl or the like comes back
        ItemStack eaten = slots.get(slot).split(1);
        ItemStack remainder = eaten.finishUsingItem(player.level(), player);
        if (!remainder.isEmpty()) {
            remainder = BackpackContents.insert(slots, remainder);
            if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder, Prediction.PREDICTED);
        }
        Backpacks.write(player, backpack, slots);
        return true;
    }

    private BackpackFeeding() {}
}
