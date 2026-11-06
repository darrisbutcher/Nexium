package net.storagenexus.essentials.farm;

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
import net.storagenexus.essentials.registry.ModMenus;
import org.jspecify.annotations.Nullable;

/**
 * The farm screen: the 27 output slots (take only), the input slot for the kinds that have one, and the player
 * inventory. The cycle length travels in 15-bit halves because a menu data slot is 16 bits.
 */
public class FarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 194;
    public static final int INPUT_X = 8, INPUT_Y = 20;
    public static final int OUTPUT_TOP = 44;
    public static final int INVENTORY_TOP = 112;

    private static final int FIRST_PLAYER_SLOT = FarmBlockEntity.SIZE;
    private static final int D_PROGRESS = 0, D_FLAGS = 1, D_CYCLE = 2, D_COUNT = 4;
    private static final int FLAG_FULL = 1, FLAG_PAUSED = 2, FLAG_STARVED = 4;

    private final Container container;
    private final ContainerData data;
    private final FarmKind kind;

    /** Client side. */
    public FarmMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(FarmBlockEntity.SIZE), null,
                inventory.player.level().getBlockState(pos).getBlock() instanceof FarmBlock block ? block.kind() : FarmKind.ZOMBIFIED_PIGLIN);
    }

    /** Server side. */
    public FarmMenu(int id, Inventory inventory, FarmBlockEntity farm) {
        this(id, inventory, farm, farm, farm.kind());
    }

    private FarmMenu(int id, Inventory inventory, Container container, @Nullable FarmBlockEntity farm, FarmKind kind) {
        super(ModMenus.FARM, id);
        this.container = container;
        this.kind = kind;
        container.startOpen(inventory.player);
        for (int i = 0; i < FarmBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, 8 + (i % 9) * 18, OUTPUT_TOP + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addSlot(new Slot(container, FarmBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return kind.acceptsInput(stack);
            }

            @Override
            public boolean isActive() {
                return kind.hasInput();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));

        data = farm == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_PROGRESS -> Math.round(farm.progressFraction() * 1000);
                    case D_FLAGS -> (farm.isFull() ? FLAG_FULL : 0) | (farm.isPaused() ? FLAG_PAUSED : 0) | (farm.isStarved() ? FLAG_STARVED : 0);
                    case D_CYCLE -> kind.cycleTicks() & 0x7FFF;
                    case D_CYCLE + 1 -> kind.cycleTicks() >> 15;
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

    public FarmKind kind() { return kind; }

    /** Work towards the next cycle, 0 to 1. */
    public float progress() { return data.get(D_PROGRESS) / 1000.0F; }
    public boolean full() { return (data.get(D_FLAGS) & FLAG_FULL) != 0; }
    public boolean paused() { return (data.get(D_FLAGS) & FLAG_PAUSED) != 0; }
    public boolean starved() { return (data.get(D_FLAGS) & FLAG_STARVED) != 0; }
    /** The server's cycle length in ticks, which can differ from this client's config. */
    public int cycleTicks() { return Math.max(1, data.get(D_CYCLE) | data.get(D_CYCLE + 1) << 15); }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** Shift-click: output goes to the inventory; input from the inventory goes to the input slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < FIRST_PLAYER_SLOT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
        } else if (kind.acceptsInput(stack)) {
            if (!moveItemStackTo(stack, FarmBlockEntity.INPUT_SLOT, FarmBlockEntity.INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
