package net.storagenexus.freezer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
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

/**
 * The Freezer screen: the tank, the mode button, a progress bar, the 3x3 output (take only) and the player inventory.
 * The mode button is menu button 0.
 */
public class FreezerMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 20;
    public static final int INVENTORY_TOP = 84;
    public static final int BTN_MODE = 0;

    private static final int FIRST_PLAYER_SLOT = FreezerBlockEntity.SLOTS;
    private static final int D_PROGRESS = 0, D_FLAGS = 1, D_MODE = 2, D_WATER = 3, D_COUNT = 4;
    private static final int FLAG_FULL = 1, FLAG_PAUSED = 2, FLAG_DRY = 4;

    private final Container container;
    private final ContainerData data;
    private final @Nullable FreezerBlockEntity freezer;

    /** Client side. */
    public FreezerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(FreezerBlockEntity.SLOTS), null);
    }

    /** Server side. */
    public FreezerMenu(int id, Inventory inventory, FreezerBlockEntity freezer) {
        this(id, inventory, freezer, freezer);
    }

    private FreezerMenu(int id, Inventory inventory, Container container, @Nullable FreezerBlockEntity freezer) {
        super(ModMenus.FREEZER, id);
        this.container = container;
        this.freezer = freezer;
        container.startOpen(inventory.player);
        for (int i = 0; i < FreezerBlockEntity.SLOTS; i++) {
            addSlot(new Slot(container, i, OUTPUT_X + (i % 3) * 18, OUTPUT_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));

        data = freezer == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_PROGRESS -> Math.round(freezer.progressFraction() * 1000);
                    case D_FLAGS -> (freezer.isFull() ? FLAG_FULL : 0) | (freezer.isPaused() ? FLAG_PAUSED : 0) | (freezer.isDry() ? FLAG_DRY : 0);
                    case D_MODE -> freezer.mode().ordinal();
                    // Millibuckets: at most 8000
                    case D_WATER -> (int) (freezer.water() * 1000 / FluidConstants.BUCKET);
                    default -> 0;
                };
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

    public float progress() { return data.get(D_PROGRESS) / 1000.0F; }
    public boolean full() { return (data.get(D_FLAGS) & FLAG_FULL) != 0; }
    public boolean paused() { return (data.get(D_FLAGS) & FLAG_PAUSED) != 0; }
    public boolean dry() { return (data.get(D_FLAGS) & FLAG_DRY) != 0; }
    public FreezerMode mode() { return FreezerMode.byOrdinal(data.get(D_MODE)); }
    /** Water in the tank, in millibuckets. */
    public int waterMilli() { return data.get(D_WATER); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_MODE && freezer != null) {
            freezer.cycleMode();
            return true;
        }
        return false;
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

    /** Shift-click moves the output to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index >= FIRST_PLAYER_SLOT) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
