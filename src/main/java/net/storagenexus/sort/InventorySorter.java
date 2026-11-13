package net.storagenexus.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.storagenexus.barrel.BarrelMenu;

/**
 * Sorts one region of an open menu: either the container's own slots or the player's main inventory (never the hotbar,
 * armor or offhand). Stacks are merged first, then put A to Z; shulker boxes and bundles go to the bottom-right slots
 * when there is room, so the rest of the items stay at the top.
 */
public final class InventorySorter {
    /** First and last inventory index of the main inventory: 0 to 8 is the hotbar, 9 to 35 the storage rows. */
    public static final int MAIN_FIRST = 9;
    public static final int MAIN_LAST = 35;

    public enum Region { CONTAINER, PLAYER }

    private InventorySorter() {}

    /** Menus whose container slots may be sorted: chests, barrels, ender chests, shulker boxes, hoppers, dispensers. */
    public static boolean supportsContainer(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu
                || menu instanceof DispenserMenu || menu instanceof BarrelMenu;
    }

    public static boolean isMainInventorySlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() >= MAIN_FIRST
                && slot.getContainerSlot() <= MAIN_LAST;
    }

    /** The slots of a region in menu order (top-left to bottom-right); empty when it cannot be sorted. */
    public static List<Slot> slotsOf(AbstractContainerMenu menu, Region region) {
        List<Slot> result = new ArrayList<>();
        if (region == Region.CONTAINER && !supportsContainer(menu)) return result;
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            boolean main = isMainInventorySlot(slot);
            if (region == Region.PLAYER ? main : !(slot.container instanceof Inventory)) result.add(slot);
        }
        return result;
    }

    /**
     * Which region a point falls in; x and y are relative to the screen's top-left corner. A region is the box around
     * its slots, so gaps between slots count, but the hotbar, armor and everything else do not.
     */
    public static Region regionAt(AbstractContainerMenu menu, double x, double y) {
        for (Region region : Region.values()) {
            List<Slot> slots = slotsOf(menu, region);
            if (slots.isEmpty()) continue;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (Slot s : slots) {
                minX = Math.min(minX, s.x);
                minY = Math.min(minY, s.y);
                maxX = Math.max(maxX, s.x + 16);
                maxY = Math.max(maxY, s.y + 16);
            }
            if (x >= minX && x < maxX && y >= minY && y < maxY) return region;
        }
        return null;
    }

    /** Server side: sort what the player's open menu shows. Returns whether anything was sorted. */
    public static boolean sort(ServerPlayer player, int containerId, boolean playerRegion) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId) return false;
        List<Slot> slots = slotsOf(menu, playerRegion ? Region.PLAYER : Region.CONTAINER);
        if (slots.isEmpty()) return false;
        boolean changed = sort(slots);
        if (changed) menu.broadcastChanges();
        return changed;
    }

    /** Sorts the given slots in place, A to Z. */
    public static boolean sort(List<Slot> slots) {
        int n = slots.size();
        List<ItemStack> merged = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            int limit = Math.max(1, Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack)));
            ItemStack rest = stack.copy();
            for (ItemStack into : merged) {
                if (rest.isEmpty()) break;
                if (into.getCount() < limit && ItemStack.isSameItemSameComponents(into, rest)) {
                    int move = Math.min(rest.getCount(), limit - into.getCount());
                    into.grow(move);
                    rest.shrink(move);
                }
            }
            if (!rest.isEmpty()) merged.add(rest);
        }
        Comparator<ItemStack> comparator = BY_NAME;
        List<ItemStack> normal = new ArrayList<>();
        List<ItemStack> special = new ArrayList<>();
        for (ItemStack s : merged) (isBottomRight(s) ? special : normal).add(s);
        normal.sort(comparator);
        special.sort(comparator);

        ItemStack[] target = new ItemStack[n];
        java.util.Arrays.fill(target, ItemStack.EMPTY);
        for (int i = 0; i < normal.size(); i++) target[i] = normal.get(i);
        for (int i = 0; i < special.size(); i++) target[n - special.size() + i] = special.get(i);

        for (int i = 0; i < n; i++) {
            if (!target[i].isEmpty() && !slots.get(i).mayPlace(target[i])) return false;
        }
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            Slot slot = slots.get(i);
            ItemStack now = slot.getItem();
            if (ItemStack.matches(now, target[i])) continue;
            slot.set(target[i].copy());
            changed = true;
        }
        return changed;
    }

    /** Shulker boxes and bundles sit at the bottom right of a sorted inventory. */
    public static boolean isBottomRight(ItemStack stack) {
        return stack.has(DataComponents.BUNDLE_CONTENTS)
                || Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    /** A to Z by name, then by id, biggest stack first. */
    private static final Comparator<ItemStack> BY_NAME = Comparator
            .comparing((ItemStack s) -> s.getHoverName().getString().toLowerCase(Locale.ROOT))
            .thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
            .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
}
