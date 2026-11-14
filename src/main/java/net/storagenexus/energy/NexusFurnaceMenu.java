package net.storagenexus.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModMenus;
import org.jspecify.annotations.Nullable;

/** The Nexus Furnace's screen: eight inputs over eight outputs, a progress bar for each, the energy bar, the banked experience. */
public class NexusFurnaceMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 190;
    public static final int INPUT_X = 16, INPUT_Y = 18, OUTPUT_Y = 44;
    public static final int INVENTORY_TOP = 108;
    public static final int BTN_COLLECT = 0;
    private static final int SLOTS = NexusFurnaceBlockEntity.SLOTS;
    private static final int FIRST_PLAYER_SLOT = NexusFurnaceBlockEntity.SIZE;
    /** Screen values travel as 16-bit numbers, so the energy and the experience are each sent as two halves of 15 bits. */
    private static final int D_PROGRESS = 0, D_ENERGY = SLOTS, D_XP = SLOTS + 2, D_WORKING = SLOTS + 4, D_WANTING = SLOTS + 5, D_COUNT = SLOTS + 6;
    private static final long MAX_XP_SHOWN = 0x3FFF_FFFFL;

    private final Container container;
    private final ContainerData data;
    private final @Nullable NexusFurnaceBlockEntity furnace;

    /** Client side. */
    public NexusFurnaceMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(NexusFurnaceBlockEntity.SIZE), null);
    }

    /** Server side. */
    public NexusFurnaceMenu(int id, Inventory inventory, NexusFurnaceBlockEntity furnace) {
        this(id, inventory, furnace, furnace);
    }

    private NexusFurnaceMenu(int id, Inventory inventory, Container container, @Nullable NexusFurnaceBlockEntity furnace) {
        super(ModMenus.NEXUS_FURNACE, id);
        this.container = container;
        this.furnace = furnace;
        container.startOpen(inventory.player);
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(container, i, INPUT_X + i * 18, INPUT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); }
            });
        }
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(container, SLOTS + i, INPUT_X + i * 18, OUTPUT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = furnace == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                if (index < SLOTS) return furnace.progress(index);
                return switch (index) {
                    case D_ENERGY -> (int) (furnace.energy().stored() & 0x7FFF);
                    case D_ENERGY + 1 -> (int) (furnace.energy().stored() >> 15);
                    case D_XP -> (int) (xpShown() & 0x7FFF);
                    case D_XP + 1 -> (int) (xpShown() >> 15);
                    case D_WORKING -> furnace.working();
                    case D_WANTING -> furnace.wanting();
                    default -> 0;
                };
            }

            private long xpShown() {
                return Math.min(MAX_XP_SHOWN, (long) Math.floor(furnace.experience()));
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return D_COUNT;
            }
        };
        addDataSlots(data);
    }

    /** Ticks a slot has cooked its item, 0 to {@value NexusFurnaceBlockEntity#COOK_TICKS}. */
    public int progress(int slot) { return data.get(D_PROGRESS + slot); }
    public int energy() { return data.get(D_ENERGY) | data.get(D_ENERGY + 1) << 15; }
    /** The whole points banked, as Collect XP would hand them over. */
    public int experience() { return data.get(D_XP) | data.get(D_XP + 1) << 15; }
    public int working() { return data.get(D_WORKING); }
    public int wanting() { return data.get(D_WANTING); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_COLLECT && furnace != null) {
            furnace.takeExperience(player);
            return true;
        }
        return false;
    }

    /**
     * Shift-click: from a furnace slot to the inventory; from the inventory a stack is shared out evenly over the input
     * slots (always into the one holding the least of that item).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < FIRST_PLAYER_SLOT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!shareOut(stack)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    private boolean shareOut(ItemStack stack) {
        boolean any = false;
        while (!stack.isEmpty()) {
            Slot best = null;
            for (int i = 0; i < SLOTS; i++) {
                Slot s = slots.get(i);
                if (!s.mayPlace(stack)) continue;
                ItemStack in = s.getItem();
                if (!in.isEmpty() && !ItemStack.isSameItemSameComponents(in, stack)) continue;
                if (in.getCount() >= Math.min(stack.getMaxStackSize(), s.getMaxStackSize(stack))) continue;
                if (best == null || in.getCount() < best.getItem().getCount()) best = s;
            }
            if (best == null) break;
            if (best.getItem().isEmpty()) best.set(stack.copyWithCount(1));
            else best.getItem().grow(1);
            best.setChanged();
            stack.shrink(1);
            any = true;
        }
        return any;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
