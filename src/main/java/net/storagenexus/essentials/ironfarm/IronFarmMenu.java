package net.storagenexus.essentials.ironfarm;

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
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModMenus;
import org.jspecify.annotations.Nullable;

/**
 * The Iron Farm screen: the 27 output slots (take only) and the player inventory.
 * Timers travel in 15-bit halves because a menu data slot is 16 bits.
 */
public class IronFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 194;
    public static final int BREAD_X = 8, BREAD_Y = 20;
    public static final int OUTPUT_TOP = 44;
    public static final int INVENTORY_TOP = 112;

    private static final int FIRST_PLAYER_SLOT = IronFarmBlockEntity.SIZE;
    private static final int D_PROGRESS = 0, D_BOOST = 1, D_COOLDOWN = 3, D_FLAGS = 5, D_SPEED_UP = 6, D_GOLEM_TICKS = 7, D_COUNT = 8;
    private static final int FLAG_FULL = 1, FLAG_BOOSTING = 2, FLAG_PAUSED = 4, FLAG_UNPOWERED = 8;

    private final Container container;
    private final ContainerData data;

    /** Client side. */
    public IronFarmMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(IronFarmBlockEntity.SIZE), null);
    }

    /** Server side. */
    public IronFarmMenu(int id, Inventory inventory, IronFarmBlockEntity farm) {
        this(id, inventory, farm, farm);
    }

    private IronFarmMenu(int id, Inventory inventory, Container container, @Nullable IronFarmBlockEntity farm) {
        super(ModMenus.IRON_FARM, id);
        this.container = container;
        container.startOpen(inventory.player);
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, 8 + (i % 9) * 18, OUTPUT_TOP + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addSlot(new Slot(container, IronFarmBlockEntity.BREAD_SLOT, BREAD_X, BREAD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
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
                    case D_FLAGS -> (farm.isFull() ? FLAG_FULL : 0) | (farm.isPaused() ? FLAG_PAUSED : 0)
                            | (farm.isUnpowered() ? FLAG_UNPOWERED : 0);
                    case D_GOLEM_TICKS -> NexusConfig.get().ironFarmGolemTicks;
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

    private static int low(long value) {
        return (int) (Math.min(value, (1L << 30) - 1) & 0x7FFF);
    }

    private static int high(long value) {
        return (int) (Math.min(value, (1L << 30) - 1) >> 15);
    }

    private int joined(int index) {
        return data.get(index) | data.get(index + 1) << 15;
    }

    /** Work towards the next golem, 0 to 1. */
    public float progress() { return data.get(D_PROGRESS) / 1000.0F; }
    /** Boost left, counting the bread in the slot, in seconds. */
    public int boostSeconds() { return joined(D_BOOST); }
    /** Seconds until bread can go in again. */
    public int cooldownSeconds() { return joined(D_COOLDOWN); }
    public boolean coolingDown() { return cooldownSeconds() > 0; }
    public boolean boosting() { return (data.get(D_FLAGS) & FLAG_BOOSTING) != 0; }
    public boolean full() { return (data.get(D_FLAGS) & FLAG_FULL) != 0; }
    public boolean paused() { return (data.get(D_FLAGS) & FLAG_PAUSED) != 0; }
    /** Waiting for Nexus Energy. */
    public boolean unpowered() { return (data.get(D_FLAGS) & FLAG_UNPOWERED) != 0; }
    public int speedUp() { return Math.max(1, data.get(D_SPEED_UP)); }
    public int golemTicks() { return Math.max(1, data.get(D_GOLEM_TICKS)); }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** Shift-click moves output to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < FIRST_PLAYER_SLOT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
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
