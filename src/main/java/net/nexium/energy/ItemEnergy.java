package net.nexium.energy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.nexium.registry.ModComponents;

/**
 * Nexus Energy in items. A battery cell holds its charge itself; a Jetpack or a Crafting Storage Tablet holds a cell in its own
 * screen and draws from that. Every method takes the stack that holds the energy (a cell, or the Jetpack or Tablet with a cell in it)
 * and works on the cell inside, so a charger and the machines never need to know which it is.
 */
public final class ItemEnergy {
    /** The cell this stack is, or has inside it; empty if there is none. A copy: write it back with {@link #setCell}. */
    public static ItemStack cell(ItemStack holder) {
        if (holder.getItem() instanceof BatteryCellItem) return holder.copy();
        ItemStackTemplate inside = holder.get(ModComponents.INSTALLED_CELL);
        return inside == null ? ItemStack.EMPTY : inside.create();
    }

    /** Puts a cell into a holder (a Jetpack or Tablet), or takes it out when the cell is empty. */
    public static void setCell(ItemStack holder, ItemStack cell) {
        if (cell.isEmpty()) holder.remove(ModComponents.INSTALLED_CELL);
        else holder.set(ModComponents.INSTALLED_CELL, ItemStackTemplate.fromNonEmptyStack(cell));
    }

    /** Whether this can be charged: a cell, or a holder with a cell in it. */
    public static boolean chargeable(ItemStack holder) {
        return !cell(holder).isEmpty();
    }

    public static long stored(ItemStack holder) {
        ItemStack cell = cell(holder);
        Long charge = cell.get(ModComponents.BATTERY_CHARGE);
        return cell.isEmpty() || charge == null ? 0 : Math.min(charge, capacity(cell));
    }

    public static long capacity(ItemStack holder) {
        ItemStack cell = cell(holder);
        return cell.getItem() instanceof BatteryCellItem c ? c.capacity() : 0;
    }

    /** Puts up to {@code max} NE into the cell; returns how much went in. With {@code simulate}, nothing changes. */
    public static long insert(ItemStack holder, long max, boolean simulate) {
        ItemStack cell = cell(holder);
        if (cell.isEmpty() || max <= 0) return 0;
        long n = Math.min(max, capacity(cell) - stored(cell));
        if (n > 0 && !simulate) write(holder, cell, stored(cell) + n);
        return Math.max(0, n);
    }

    /** Takes up to {@code max} NE out of the cell; returns how much came out. With {@code simulate}, nothing changes. */
    public static long extract(ItemStack holder, long max, boolean simulate) {
        ItemStack cell = cell(holder);
        if (cell.isEmpty() || max <= 0) return 0;
        long n = Math.min(max, stored(cell));
        if (n > 0 && !simulate) write(holder, cell, stored(cell) - n);
        return Math.max(0, n);
    }

    private static void write(ItemStack holder, ItemStack cell, long charge) {
        if (charge > 0) cell.set(ModComponents.BATTERY_CHARGE, charge);
        else cell.remove(ModComponents.BATTERY_CHARGE);
        if (holder.getItem() instanceof BatteryCellItem) {
            if (charge > 0) holder.set(ModComponents.BATTERY_CHARGE, charge);
            else holder.remove(ModComponents.BATTERY_CHARGE);
        } else {
            setCell(holder, cell);
        }
    }

    private ItemEnergy() {}
}
