package net.storagenexus.builder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.registry.ModComponents;
import org.jspecify.annotations.Nullable;

/**
 * What a Construction Builder is set to, kept on the item: its mode, plane, batch size and its one module slot (a Nexus
 * Link, so blocks can come from a storage network). All of it is set in its tool menu.
 */
public final class BuilderSettings {
    /** Extend grows the clicked face; Line places one row; Exchange swaps the clicked surface for the offhand block. */
    public enum Mode { EXTEND, LINE, EXCHANGE }

    /** Which way it may spread: any way across the face, or only horizontally, or only vertically. */
    public enum Plane { ANY, HORIZONTAL, VERTICAL }

    /** Batch sizes the menu steps through, largest first (only those up to the tier's limit). */
    private static final int[] SIZES = {128, 64, 32, 27, 16, 9, 4, 1};

    public static Mode mode(ItemStack builder) {
        return Mode.values()[Math.floorMod(builder.getOrDefault(ModComponents.BUILDER_MODE, 0), Mode.values().length)];
    }

    public static Mode cycleMode(ItemStack builder) {
        Mode next = Mode.values()[(mode(builder).ordinal() + 1) % Mode.values().length];
        builder.set(ModComponents.BUILDER_MODE, next.ordinal());
        return next;
    }

    public static Plane plane(ItemStack builder) {
        return Plane.values()[Math.floorMod(builder.getOrDefault(ModComponents.BUILDER_PLANE, 0), Plane.values().length)];
    }

    public static Plane cyclePlane(ItemStack builder) {
        Plane next = Plane.values()[(plane(builder).ordinal() + 1) % Plane.values().length];
        builder.set(ModComponents.BUILDER_PLANE, next.ordinal());
        return next;
    }

    public static int maxBlocks(ItemStack builder) {
        return builder.getItem() instanceof BuilderItem item ? item.tier().maxBlocks() : 0;
    }

    /** Blocks one click places: the size set in the menu, never above the tier's limit (the limit if unset). */
    public static int size(ItemStack builder) {
        int max = maxBlocks(builder);
        return Math.max(1, Math.min(max, builder.getOrDefault(ModComponents.BUILDER_SIZE, max)));
    }

    /** Steps the batch size down to the next smaller one, back to the tier's limit after the smallest. */
    public static int cycleSize(ItemStack builder) {
        int max = maxBlocks(builder), current = size(builder), next = max;
        for (int s : SIZES) {
            if (s < current && s <= max) { next = s; break; }
        }
        builder.set(ModComponents.BUILDER_SIZE, next);
        return next;
    }

    /** The module in the builder's one slot, or an empty stack. */
    public static ItemStack module(ItemStack builder) {
        var stored = builder.getOrDefault(ModComponents.MODULES, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
        return stored.isEmpty() ? ItemStack.EMPTY : stored.getFirst();
    }

    public static void setModule(ItemStack builder, ItemStack module) {
        if (module.isEmpty()) builder.remove(ModComponents.MODULES);
        else builder.set(ModComponents.MODULES, ItemContainerContents.fromItems(NonNullList.of(ItemStack.EMPTY, module)));
    }

    /** The terminal its Nexus Link takes blocks from, if it has one switched on and linked. */
    public static @Nullable GlobalPos activeLink(ItemStack builder) {
        ItemStack module = module(builder);
        if (!(module.getItem() instanceof ModuleItem item) || item.type() != ModuleType.NEXUS_LINK) return null;
        if (module.has(ModComponents.MODULE_OFF)) return null;
        return module.get(ModComponents.TERMINAL_LINK);
    }

    private BuilderSettings() {}
}
