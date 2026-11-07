package net.storagenexus.client.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.storagenexus.terminal.FillGridPayload;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared by the JEI and REI "+" buttons: can the network and inventory make this, and asking the server to fill the grid. */
public final class RecipeTransfer {
    /** Exact check when the viewer gives us the recipe itself (JEI). */
    public static boolean canCraft(TerminalMenu menu, Player player, Recipe<?> recipe) {
        StackedItemContents contents = new StackedItemContents();
        player.getInventory().fillStackedContents(contents);
        menu.fillCraftSlotsStackedContents(contents);
        return contents.canCraft(recipe, null);
    }

    /**
     * Close-enough check from the ingredient lists (REI): every slot needs one of its accepted
     * items, counting what each slot uses up.
     */
    public static boolean canCraft(TerminalMenu menu, Player player, List<List<Item>> slots) {
        return missing(menu, player, slots).isEmpty();
    }

    /**
     * Which recipe slots (indices into {@code slots}, each the items it accepts) nothing in the network, the inventory or
     * the grid can fill, counting what each filled slot uses up. Empty slots in the list are skipped.
     */
    public static List<Integer> missing(TerminalMenu menu, Player player, List<List<Item>> slots) {
        Map<Item, Long> available = available(menu, player);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            List<Item> options = slots.get(i);
            if (options.isEmpty()) continue;
            boolean found = false;
            for (Item item : options) {
                long n = available.getOrDefault(item, 0L);
                if (n > 0) {
                    available.put(item, n - 1);
                    found = true;
                    break;
                }
            }
            if (!found) missing.add(i);
        }
        return missing;
    }

    /** What the network (as this client last saw it), the player's inventory and the crafting grid hold, by item. */
    private static Map<Item, Long> available(TerminalMenu menu, Player player) {
        Map<Item, Long> available = new HashMap<>();
        for (TerminalSyncPayload.ItemEntry e : menu.clientItems()) available.merge(e.stack().getItem(), e.count(), Long::sum);
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (!s.isEmpty()) available.merge(s.getItem(), (long) s.getCount(), Long::sum);
        for (net.minecraft.world.inventory.Slot s : menu.getInputGridSlots()) {
            ItemStack st = s.getItem();
            if (!st.isEmpty()) available.merge(st.getItem(), (long) st.getCount(), Long::sum);
        }
        return available;
    }

    public static void fill(ResourceKey<Recipe<?>> recipe, boolean maxItems) {
        ClientPlayNetworking.send(new FillGridPayload(recipe, maxItems));
    }

    private RecipeTransfer() {}
}
