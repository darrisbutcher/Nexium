package net.storagenexus.essentials.backpack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.drawer.CompactingRecipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The Compacting module, on the server. While a Backpack with the module in and on is worn, it packs what the Backpack
 * holds into storage blocks once a second: 9 iron ingots become an iron block, 9 iron nuggets an ingot (and then a block),
 * 9 wheat a hay bale. What an item packs into comes from the game's own crafting recipes, the same way the Compacting
 * Drawer finds its tiers ({@link CompactingRecipes}): only a pack that crafts straight back into the same items counts, so
 * nothing is lost or changed (sand never becomes sandstone), and modded blocks work too. The blocks go into the Backpack's
 * own free slots, and a pack that would not fit is left alone. Renamed, enchanted and other customised items are never
 * packed. It works while the Backpack's screen is open too, packing the slots on the screen as they are.
 */
public final class BackpackCompacting {
    private static final int INTERVAL = 20;
    /** Nuggets to ingots to blocks is two steps in a row. */
    private static final int MAX_PASSES = 4;
    /** The smallest pack there is is 2 x 2. */
    private static final int SMALLEST_PACK = 4;

    /** What each item packs into, found from the recipes the first time it is seen and kept until they are reloaded. */
    private static final Map<Item, Optional<CompactingRecipes.Pack>> RULES = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if ((tick + player.getId()) % INTERVAL == 0) compactNow(player);
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> RULES.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RULES.clear());
    }

    /** Packs what the worn Backpack holds, once, if its Compacting module is on. Returns whether anything was packed. */
    public static boolean compactNow(ServerPlayer player) {
        ItemStack backpack = Backpacks.activeFor(player, BackpackModuleType.COMPACTING);
        if (backpack.isEmpty()) return false;
        ServerLevel level = player.level();
        NonNullList<ItemStack> slots = Backpacks.read(player, backpack);
        if (!compact(slots, item -> RULES.computeIfAbsent(item, i -> CompactingRecipes.packInto(level, i)))) return false;
        Backpacks.write(player, backpack, slots);
        return true;
    }

    /** Plain stacks only: nothing renamed, enchanted, damaged or otherwise different from a fresh one. */
    private static boolean isPlain(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
    }

    /**
     * Packs these slots as far as {@code rules} (what an item packs into, if anything) allow, in as many rounds as it
     * takes for a pack of a pack. Returns whether anything was packed.
     */
    static boolean compact(List<ItemStack> slots, Function<Item, Optional<CompactingRecipes.Pack>> rules) {
        boolean changed = false;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Map<Item, Integer> totals = new LinkedHashMap<>();
            for (ItemStack stack : slots) {
                if (isPlain(stack)) totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
            boolean packed = false;
            for (Map.Entry<Item, Integer> total : totals.entrySet()) {
                if (total.getValue() < SMALLEST_PACK) continue;
                Optional<CompactingRecipes.Pack> rule = rules.apply(total.getKey());
                if (rule.isPresent() && pack(slots, total.getKey(), total.getValue(), rule.get())) packed = true;
            }
            if (!packed) break;
            changed = true;
        }
        return changed;
    }

    /** Packs as many whole packs of this item as the slots have room for the blocks of. */
    private static boolean pack(List<ItemStack> slots, Item item, int total, CompactingRecipes.Pack pack) {
        for (int blocks = total / pack.count(); blocks >= 1; blocks--) {
            List<ItemStack> trial = new ArrayList<>(slots.size());
            for (ItemStack stack : slots) trial.add(stack.copy());
            take(trial, item, blocks * pack.count());
            if (!BackpackContents.insert(trial, new ItemStack(pack.into(), blocks)).isEmpty()) continue;
            for (int i = 0; i < slots.size(); i++) slots.set(i, trial.get(i));
            return true;
        }
        return false;
    }

    /** Takes this many of the item out of the plain stacks of it, from the last slot first. */
    private static void take(List<ItemStack> slots, Item item, int amount) {
        for (int i = slots.size() - 1; i >= 0 && amount > 0; i--) {
            ItemStack stack = slots.get(i);
            if (!stack.is(item) || !isPlain(stack)) continue;
            int took = Math.min(amount, stack.getCount());
            stack.shrink(took);
            amount -= took;
            if (stack.isEmpty()) slots.set(i, ItemStack.EMPTY);
        }
    }

    private BackpackCompacting() {}
}
