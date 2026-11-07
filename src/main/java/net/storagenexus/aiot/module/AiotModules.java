package net.storagenexus.aiot.module;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Modules installed in an AIOT. They live in the AIOT's {@code storagenexus:modules} component (one slot per module
 * type, so {@link #SLOTS} slots), and each module item carries its own on/off switch ({@code storagenexus:module_off})
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
     * Takes any retired Vein Mining Module out of this AIOT and returns it (the AIOT keeps the modules it still uses, as
     * {@link #slots} reads them). Empty when there is none.
     */
    public static List<ItemStack> takeRetired(ItemStack tool) {
        if (!(tool.getItem() instanceof AiotItem)) return List.of();
        List<ItemStack> retired = stored(tool).stream().filter(stack -> stack.getItem() instanceof RetiredModuleItem).toList();
        if (!retired.isEmpty()) setSlots(tool, slots(tool));
        return retired;
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

    /** Tunnel lengths the tool menu steps through after "off", up to the config's limit. */
    private static final int[] TUNNEL_STEPS = {2, 4, 8, 16, 24, 32};

    /** How many 3x3 layers deep one 3x3 Mining swing digs with this AIOT: its module's setting, 1 (a plain 3x3) if unset, never above the config's limit. */
    public static int tunnelLength(ItemStack tool) {
        int max = NexusConfig.get().aiotTunnelMaxLength;
        return Math.max(1, Math.min(max, find(tool, ModuleType.AREA_MINING).getOrDefault(ModComponents.TUNNEL_LENGTH, 1)));
    }

    /** Steps the installed 3x3 Mining module to its next tunnel length: off, 2, 4, 8, 16, 24, 32 (those within the limit), then off again. Returns it (1 = off), or 0 if there is no such module. */
    public static int cycleTunnelLength(ItemStack tool) {
        NonNullList<ItemStack> slots = slots(tool);
        for (int i = 0; i < SLOTS; i++) {
            if (!(slots.get(i).getItem() instanceof ModuleItem item) || item.type() != ModuleType.AREA_MINING) continue;
            int max = NexusConfig.get().aiotTunnelMaxLength;
            int current = tunnelLength(tool);
            int next = 1;
            for (int step : TUNNEL_STEPS) {
                if (step > current && step <= max) { next = step; break; }
            }
            ItemStack module = slots.get(i).copy();
            if (next == 1) module.remove(ModComponents.TUNNEL_LENGTH);
            else module.set(ModComponents.TUNNEL_LENGTH, next);
            slots.set(i, module);
            setSlots(tool, slots);
            return next;
        }
        return 0;
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
