package net.nexium.aiot.module;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.nexium.aiot.AiotItem;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Modules installed in an AIOT. They live in the AIOT's {@code nexium:modules} component (one slot per module
 * type, so {@link #SLOTS} slots), and each module item carries its own on/off switch ({@code nexium:module_off})
 * and, for the Nexus Link, its terminal link. Taking a module out keeps both.
 *
 * <p>AIOTs saved when there were three module types (the Vein Mining Module has been retired, see
 * {@link RetiredModuleItem}) are read as they are: a module keeps its slot when that slot still exists, otherwise it moves
 * to a free one, and anything that is no longer a module is left out until the next time the slots are written.
 */
public final class AiotModules {
    public static final int SLOTS = ModuleType.values().length;

    /** The AIOT's module slots, always {@link #SLOTS} long (empty stacks for empty slots). */
    public static NonNullList<ItemStack> slots(ItemStack tool) {
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        List<ItemStack> stored = tool.getOrDefault(ModComponents.MODULES, ItemContainerContents.EMPTY).itemCopies().toList();
        EnumSet<ModuleType> seen = EnumSet.noneOf(ModuleType.class);
        List<ItemStack> displaced = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            ItemStack stack = stored.get(i);
            if (!(stack.getItem() instanceof ModuleItem item) || !seen.add(item.type())) continue;
            if (i < SLOTS) list.set(i, stack);
            else displaced.add(stack);
        }
        for (ItemStack stack : displaced) {
            for (int i = 0; i < SLOTS; i++) {
                if (list.get(i).isEmpty()) {
                    list.set(i, stack);
                    break;
                }
            }
        }
        return list;
    }

    /** Everything stored in the AIOT's module data, retired modules included (what drops when the AIOT breaks). */
    public static List<ItemStack> stored(ItemStack tool) {
        return tool.getOrDefault(ModComponents.MODULES, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().toList();
    }

    /**
     * Takes any retired module (the Vein Mining Module, the 3x3 Mining Module) out of this AIOT and returns it (the AIOT keeps
     * the modules it still uses, as {@link #slots} reads them). An old 3x3 Mining Module hands its on/off state and tunnel length
     * to the AIOT first. Empty when there is none.
     */
    public static List<ItemStack> takeRetired(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return List.of();
        List<ItemStack> retired = stored(tool).stream().filter(stack -> stack.getItem() instanceof RetiredModuleItem).toList();
        if (!retired.isEmpty()) {
            adoptOldArea(tool);
            setSlots(tool, slots(tool));
        }
        return retired;
    }

    /** The old 3x3 Mining Module still installed in this AIOT, or an empty stack. */
    private static ItemStack oldArea(ItemStack tool) {
        for (ItemStack stack : stored(tool)) if (stack.is(net.nexium.registry.ModItems.RETIRED_AREA_MINING_MODULE)) return stack;
        return ItemStack.EMPTY;
    }

    /** Copies an old 3x3 Mining Module's on/off state and tunnel length onto the AIOT itself, once (what the AIOT already has is kept). */
    private static void adoptOldArea(ItemStack tool) {
        ItemStack old = oldArea(tool);
        if (old.isEmpty()) return;
        if (!tool.has(ModComponents.AREA_MINING)) tool.set(ModComponents.AREA_MINING, !old.has(ModComponents.MODULE_OFF));
        if (!tool.has(ModComponents.TUNNEL_LENGTH) && old.has(ModComponents.TUNNEL_LENGTH)) {
            tool.set(ModComponents.TUNNEL_LENGTH, old.get(ModComponents.TUNNEL_LENGTH));
        }
    }

    /** Whether 3x3 mining is switched on for this AIOT: its own setting, or what an old 3x3 Mining Module in it was set to; off if neither. */
    public static boolean areaOn(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return false;
        Boolean on = tool.get(ModComponents.AREA_MINING);
        if (on != null) return on;
        ItemStack old = oldArea(tool);
        return !old.isEmpty() && !old.has(ModComponents.MODULE_OFF);
    }

    /** Switches 3x3 mining on or off. Returns its new state, or null if this is not an AIOT. */
    public static @Nullable Boolean toggleArea(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return null;
        adoptOldArea(tool);
        boolean now = !areaOn(tool);
        tool.set(ModComponents.AREA_MINING, now);
        return now;
    }

    public static void setSlots(ItemStack tool, NonNullList<ItemStack> slots) {
        if (slots.stream().allMatch(ItemStack::isEmpty)) tool.remove(ModComponents.MODULES);
        else tool.set(ModComponents.MODULES, ItemContainerContents.fromItems(slots));
    }

    /** The installed module of this type, or an empty stack. */
    public static ItemStack find(ItemStack tool, ModuleType type) {
        if (!(tool.getItem() instanceof AiotItem)) return ItemStack.EMPTY;
        for (ItemStack module : slots(tool)) {
            if (module.getItem() instanceof ModuleItem item && item.type() == type) return module;
        }
        return ItemStack.EMPTY;
    }

    /** Whether the AIOT has this module installed and switched on. */
    public static boolean active(ItemStack tool, ModuleType type) {
        ItemStack module = find(tool, type);
        return !module.isEmpty() && !module.has(ModComponents.MODULE_OFF);
    }

    /** The terminal the AIOT's Nexus Link sends drops to, if it has an active, linked one. */
    public static @Nullable GlobalPos activeLink(ItemStack tool) {
        return active(tool, ModuleType.NEXUS_LINK) ? find(tool, ModuleType.NEXUS_LINK).get(ModComponents.TERMINAL_LINK) : null;
    }

    /** Switches the module in this slot on or off. Returns its new state, or null if the slot is empty. */
    public static @Nullable Boolean toggle(ItemStack tool, int slot) {
        NonNullList<ItemStack> slots = slots(tool);
        if (slot < 0 || slot >= SLOTS || slots.get(slot).isEmpty()) return null;
        ItemStack module = slots.get(slot).copy();
        boolean nowOn = module.has(ModComponents.MODULE_OFF);
        if (nowOn) module.remove(ModComponents.MODULE_OFF);
        else module.set(ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
        slots.set(slot, module);
        setSlots(tool, slots);
        return nowOn;
    }

    /** Sizes the tool menu steps through, below the config's limit (which is always the last one). */
    private static final int[] VEIN_STEPS = {4, 9, 18, 32, 64, 128, 256};

    /** Most blocks one vein mining swing mines with this AIOT: the size set in its tool menu, never above the config's limit. */
    public static int veinSize(ItemStack tool) {
        int max = NexusConfig.get().aiotVeinMaxBlocks;
        return Math.max(1, Math.min(max, tool.getOrDefault(ModComponents.VEIN_SIZE, max)));
    }

    /** Steps the AIOT's vein size to the next one (back to the smallest after the limit). Returns it, or 0 if this isn't an AIOT. */
    public static int cycleVeinSize(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return 0;
        int max = NexusConfig.get().aiotVeinMaxBlocks;
        int current = veinSize(tool);
        int next = max;
        boolean found = false;
        for (int step : VEIN_STEPS) {
            if (step > current && step < max) { next = step; found = true; break; }
        }
        if (!found && current >= max) next = Math.min(VEIN_STEPS[0], max);
        tool.set(ModComponents.VEIN_SIZE, next);
        return next;
    }

    /** Tunnel lengths after "off" (a plain 3x3) that the tool menu and the tunnel key's scroll step through, up to the config's limit. */
    private static final int[] TUNNEL_STEPS = {2, 3, 8, 16, 24, 32};

    /** How many 3x3 layers deep one 3x3 swing digs with this AIOT: its setting, 1 (a plain 3x3) if unset, never above the config's limit. */
    public static int tunnelLength(ItemStack tool) {
        int max = NexusConfig.get().aiotTunnelMaxLength;
        Integer own = tool.get(ModComponents.TUNNEL_LENGTH);
        if (own == null) own = oldArea(tool).getOrDefault(ModComponents.TUNNEL_LENGTH, 1);
        return Math.max(1, Math.min(max, own));
    }

    private static void setTunnelLength(ItemStack tool, int length) {
        if (length <= 1) tool.remove(ModComponents.TUNNEL_LENGTH);
        else tool.set(ModComponents.TUNNEL_LENGTH, length);
    }

    /** Steps the AIOT's tunnel length to its next one: off, 2, 3, 8, 16, 24, 32 (those within the limit), then off again. Returns it (1 = off), or 0 if this isn't an AIOT. */
    public static int cycleTunnelLength(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return 0;
        adoptOldArea(tool);
        int max = NexusConfig.get().aiotTunnelMaxLength;
        int current = tunnelLength(tool);
        int next = 1;
        for (int step : TUNNEL_STEPS) {
            if (step > current && step <= max) { next = step; break; }
        }
        setTunnelLength(tool, next);
        return next;
    }

    /**
     * Moves the AIOT's tunnel length one step along off, 2, 3, 8, 16, 24, 32 (those within the limit): up to the next longer
     * tunnel, or down to the next shorter one, and no further than either end (off at the bottom, the longest at the top). A length
     * that is not on the list (an older setting) goes to the nearest step in that direction. Returns the length now set (1 = off),
     * or 0 if this isn't an AIOT.
     */
    public static int stepTunnelLength(ItemStack tool, int direction) {
        if (!(tool.getItem() instanceof AiotItem)) return 0;
        adoptOldArea(tool);
        int max = NexusConfig.get().aiotTunnelMaxLength;
        int current = tunnelLength(tool);
        int next = current;
        if (direction > 0) {
            for (int step : TUNNEL_STEPS) {
                if (step > current && step <= max) { next = step; break; }
            }
        } else if (direction < 0) {
            next = 1; // off, unless a shorter step is found
            for (int step : TUNNEL_STEPS) {
                if (step < current && step <= max) next = step;
            }
        }
        if (next != current) setTunnelLength(tool, next);
        return next;
    }

    /** Links the AIOT's installed Nexus Link to a terminal. Returns false if it has none. */
    public static boolean relink(ItemStack tool, GlobalPos terminal) {
        NonNullList<ItemStack> slots = slots(tool);
        for (int i = 0; i < SLOTS; i++) {
            if (slots.get(i).getItem() instanceof ModuleItem item && item.type() == ModuleType.NEXUS_LINK) {
                ItemStack module = slots.get(i).copy();
                module.set(ModComponents.TERMINAL_LINK, terminal);
                slots.set(i, module);
                setSlots(tool, slots);
                return true;
            }
        }
        return false;
    }

    private AiotModules() {}
}
