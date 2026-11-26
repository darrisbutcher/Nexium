package net.nexium.essentials.backpack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.nexium.drawer.CompactingRecipes;
import net.nexium.essentials.config.NexusConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The Restocking module, on the server. While a Backpack with the module in and on is worn, a hotbar or offhand stack
 * that is being used up (blocks placed, arrows shot...) is topped up to a full stack from the Backpack when it gets down to
 * the top-up setting (8, the same as the Nexus Restock armor bonus), or refilled if the last one is used. Only use counts:
 * a stack moved or thrown away as a whole is left alone.
 *
 * <p>When the Backpack has none of the item left but holds a bigger form of it, it unpacks just enough of it: Triple
 * Compressed stone becomes Double Compressed, then Compressed, then stone, and what is not needed goes back into the
 * Backpack. Which blocks unpack into what comes from the game's own crafting recipes, as for the Compacting module (a pack
 * that crafts straight back into the same items), so the Compressed blocks, an iron block and modded blocks all work.
 * Torches are left out while the Auto Torch module is on: it places them from the Backpack itself.
 * Renamed, enchanted and other customised items are restocked from stacks just like them, and never unpacked. It works
 * while the Backpack's screen is open too, through the screen's slots.
 */
public final class BackpackRestocking {
    /** Slots watched: the nine hotbar slots, then the offhand. */
    private static final int WATCHED = Inventory.getSelectionSize() + 1;
    private static final int OFFHAND = Inventory.getSelectionSize();
    /** Nuggets to ingots to blocks, or stone through three Compressed levels: this many forms in a row at most. */
    private static final int MAX_CHAIN = 6;

    /** Each player's watched slots a tick ago. The arrays and their stacks are updated in place, tick after tick. */
    private static final Map<UUID, ItemStack[]> LAST = new HashMap<>();
    /** What each item packs into, found from the recipes the first time it is seen and kept until they are reloaded. */
    private static final Map<Item, Optional<CompactingRecipes.Pack>> RULES = new HashMap<>();

    /** The game time of each player's last click in a menu: what changed in the hotbar just after was moved by hand, not used up. */
    private static final Map<UUID, Long> LAST_CLICK = new HashMap<>();

    /** A click in a menu (the inventory included) just happened; see {@link #LAST_CLICK}. */
    public static void noteMenuClick(ServerPlayer player) {
        LAST_CLICK.put(player.getUUID(), player.level().getGameTime());
    }

    /** How long after a click in a menu (in ticks, half a second) the hotbar is left alone: a Space + click sends one click for every stack of an item. */
    private static final int CLICK_QUIET_TICKS = 10;

    private static boolean clickedRecently(ServerPlayer player) {
        Long at = LAST_CLICK.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= CLICK_QUIET_TICKS;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST.remove(handler.getPlayer().getUUID());
            LAST_CLICK.remove(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> RULES.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST.clear();
            LAST_CLICK.clear();
            RULES.clear();
        });
    }

    private static ItemStack get(ServerPlayer player, int slot) {
        return slot == OFFHAND ? player.getItemInHand(InteractionHand.OFF_HAND) : player.getInventory().getItem(slot);
    }

    private static void set(ServerPlayer player, int slot, ItemStack stack) {
        if (slot == OFFHAND) player.setItemInHand(InteractionHand.OFF_HAND, stack);
        else player.getInventory().setItem(slot, stack);
    }

    /** One tick of restocking for this player (public for the game tests). */
    public static void tick(ServerPlayer player) {
        // The game mode, not isCreative(): creative stacks are never used up
        ItemStack backpack = player.gameMode.getGameModeForPlayer().isCreative() ? ItemStack.EMPTY : Backpacks.activeFor(player, BackpackModuleType.RESTOCKING);
        if (backpack.isEmpty()) {
            LAST.remove(player.getUUID());
            return;
        }
        ItemStack[] last = LAST.get(player.getUUID());
        if (last == null) {
            last = new ItemStack[WATCHED];
            java.util.Arrays.fill(last, ItemStack.EMPTY);
            LAST.put(player.getUUID(), last);
        } else {
            // A click in a menu this tick moved things by hand (a shift-click of the last one or two of a stack, say): nothing was used up
            if (!clickedRecently(player)) for (int slot = 0; slot < WATCHED; slot++) restockSlot(player, backpack, slot, last[slot]);
        }
        // Remember the slots for next tick, copying a stack only when it is a different one
        for (int slot = 0; slot < WATCHED; slot++) {
            ItemStack now = get(player, slot);
            if (now.isEmpty()) last[slot] = ItemStack.EMPTY;
            else if (!last[slot].isEmpty() && ItemStack.isSameItemSameComponents(now, last[slot])) last[slot].setCount(now.getCount());
            else last[slot] = now.copy();
        }
    }

    private static void restockSlot(ServerPlayer player, ItemStack backpack, int slot, ItemStack before) {
        if (before.isEmpty() || before.getMaxStackSize() <= 1) return;
        // Torches are the Auto Torch module's to use: it places them from the Backpack, so they are not handed back out to the hotbar
        if (before.is(Items.TORCH) && !Backpacks.activeFor(player, BackpackModuleType.AUTO_TORCH).isEmpty()) return;
        ItemStack now = get(player, slot);
        boolean same = ItemStack.isSameItemSameComponents(now, before);
        int used = before.getCount() - (same ? now.getCount() : 0);
        // Used, a little at a time: never a whole stack moved or thrown away
        boolean usedUp = now.isEmpty() && before.getCount() <= 2;
        boolean runningLow = same && used > 0 && used <= 2 && now.getCount() <= NexusConfig.get().restockTopUpAt;
        if (!usedUp && !runningLow) return;
        int want = before.getMaxStackSize() - (same ? now.getCount() : 0);
        ServerLevel level = player.level();
        NonNullList<ItemStack> slots = Backpacks.read(player, backpack);
        int got = fill(slots, before, want, item -> RULES.computeIfAbsent(item, i -> CompactingRecipes.packInto(level, i)));
        if (got <= 0) return;
        Backpacks.write(player, backpack, slots);
        if (same) now.grow(got);
        else set(player, slot, before.copyWithCount(got));
    }

    /**
     * Takes up to {@code want} of this stack's item out of these slots: from stacks just like it first, then, if that is not
     * enough and the item is a plain one, by unpacking the bigger forms of it ({@code rules}: what an item packs into, if
     * anything), only as many as it takes. What is unpacked and not taken stays in the slots. Returns how many came out.
     */
    public static int fill(List<ItemStack> slots, ItemStack template, int want, Function<Item, Optional<CompactingRecipes.Pack>> rules) {
        int got = takeSame(slots, template, want);
        if (got >= want || !isPlain(template)) return got;

        // The forms above this item: chain[0] is the item, chain[k + 1] what chain[k] packs into, ratio[k + 1] of chain[k] to one of it
        List<Item> chain = new ArrayList<>();
        List<Integer> ratio = new ArrayList<>();
        chain.add(template.getItem());
        ratio.add(1);
        while (chain.size() < MAX_CHAIN) {
            Optional<CompactingRecipes.Pack> pack = rules.apply(chain.get(chain.size() - 1));
            if (pack.isEmpty() || chain.contains(pack.get().into())) break;
            chain.add(pack.get().into());
            ratio.add(Math.max(1, pack.get().count()));
        }
        if (chain.size() < 2) return got;

        // How many of the item the bigger forms would make in all, so a partial restock still takes what there is
        long available = 0;
        long perBlock = 1;
        for (int k = 1; k < chain.size(); k++) {
            perBlock *= ratio.get(k);
            available += (long) plainCount(slots, chain.get(k)) * perBlock;
        }
        int need = (int) Math.min(want - got, available);
        if (need <= 0) return got;

        List<ItemStack> trial = copy(slots);
        if (!ensure(trial, chain, ratio, 0, need)) return got;
        int took = takeSame(trial, template, need);
        for (int i = 0; i < slots.size(); i++) slots.set(i, trial.get(i));
        return got + took;
    }

    /** Makes sure the slots hold at least this many plain items of level {@code k} of the chain, unpacking the level above as needed. */
    private static boolean ensure(List<ItemStack> slots, List<Item> chain, List<Integer> ratio, int k, int amount) {
        int have = plainCount(slots, chain.get(k));
        if (have >= amount) return true;
        if (k + 1 >= chain.size()) return false;
        int perBlock = ratio.get(k + 1);
        int blocks = (amount - have + perBlock - 1) / perBlock;
        if (!ensure(slots, chain, ratio, k + 1, blocks)) return false;
        removePlain(slots, chain.get(k + 1), blocks);
        // The pieces go back into the Backpack's own slots: if they do not fit, nothing is unpacked
        return BackpackContents.insert(slots, new ItemStack(chain.get(k), blocks * perBlock)).isEmpty();
    }

    /** Plain stacks only: nothing renamed, enchanted, damaged or otherwise different from a fresh one. */
    private static boolean isPlain(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
    }

    private static int plainCount(List<ItemStack> slots, Item item) {
        long total = 0;
        for (ItemStack stack : slots) if (stack.is(item) && isPlain(stack)) total += stack.getCount();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static void removePlain(List<ItemStack> slots, Item item, int amount) {
        for (int i = slots.size() - 1; i >= 0 && amount > 0; i--) {
            ItemStack stack = slots.get(i);
            if (!stack.is(item) || !isPlain(stack)) continue;
            int took = Math.min(amount, stack.getCount());
            stack.shrink(took);
            amount -= took;
            if (stack.isEmpty()) slots.set(i, ItemStack.EMPTY);
        }
    }

    /** Takes up to this many items out of the stacks that are the same as the template (components too), from the last slot first. */
    private static int takeSame(List<ItemStack> slots, ItemStack template, int amount) {
        int left = amount;
        for (int i = slots.size() - 1; i >= 0 && left > 0; i--) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;
            int took = Math.min(left, stack.getCount());
            stack.shrink(took);
            left -= took;
            if (stack.isEmpty()) slots.set(i, ItemStack.EMPTY);
        }
        return amount - left;
    }

    private static List<ItemStack> copy(List<ItemStack> slots) {
        List<ItemStack> copy = new ArrayList<>(slots.size());
        for (ItemStack stack : slots) copy.add(stack.copy());
        return copy;
    }

    private BackpackRestocking() {}
}
