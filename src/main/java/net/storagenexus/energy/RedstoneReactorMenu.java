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

/** The Redstone Reactor's screen: nine rod slots, three coolant slots, heat, output, depth buttons and the player inventory. */
public class RedstoneReactorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 190;
    public static final int INVENTORY_TOP = 108;
    public static final int ROD_X = 8, ROD_Y = 20, COOLANT_X = 72;
    public static final int BTN_RETRACT = 0, BTN_INSERT = 1;
    /** Screen values travel as 16-bit numbers, so the energy is sent as two halves of 15 bits. */
    private static final int D_DEPTH = 0, D_HEAT = 1, D_OUTPUT = 2, D_ENERGY = 3, D_FLAGS = 5, D_COUNT = 6;
    private static final int FIRST_PLAYER_SLOT = RedstoneReactorBlockEntity.SIZE;

    private final Container container;
    private final ContainerData data;
    private final @Nullable RedstoneReactorBlockEntity reactor;

    /** Client side. */
    public RedstoneReactorMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(RedstoneReactorBlockEntity.SIZE), null);
    }

    /** Server side. */
    public RedstoneReactorMenu(int id, Inventory inventory, RedstoneReactorBlockEntity reactor) {
        this(id, inventory, reactor, reactor);
    }

    private RedstoneReactorMenu(int id, Inventory inventory, Container container, @Nullable RedstoneReactorBlockEntity reactor) {
        super(ModMenus.REDSTONE_REACTOR, id);
        this.container = container;
        this.reactor = reactor;
        container.startOpen(inventory.player);
        for (int i = 0; i < RedstoneReactorBlockEntity.RODS; i++) {
            addSlot(new Slot(container, i, ROD_X + i % 3 * 18, ROD_Y + i / 3 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return RedstoneReactorBlockEntity.isLiveRod(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int c = 0; c < RedstoneReactorBlockEntity.COOLANT; c++) {
            addSlot(new Slot(container, RedstoneReactorBlockEntity.RODS + c, COOLANT_X, ROD_Y + c * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return RedstoneReactorBlockEntity.coolantKind(stack) > 0; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = reactor == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_DEPTH -> reactor.depth();
                    case D_HEAT -> (int) Math.round(reactor.heat() * 1000);
                    case D_OUTPUT -> reactor.output();
                    case D_ENERGY -> (int) (reactor.energy().stored() & 0x7FFF);
                    case D_ENERGY + 1 -> (int) (reactor.energy().stored() >> 15);
                    case D_FLAGS -> reactor.isShutDown() ? 1 : 0;
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

    public int depth() { return data.get(D_DEPTH); }
    /** Heat in tenths of a percent. */
    public int heatPermille() { return data.get(D_HEAT); }
    public int output() { return data.get(D_OUTPUT); }
    public int energy() { return data.get(D_ENERGY) | data.get(D_ENERGY + 1) << 15; }
    public boolean shutDown() { return data.get(D_FLAGS) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (reactor == null) return false;
        if (id == BTN_RETRACT && reactor.depth() > 0) {
            reactor.setDepth(reactor.depth() - 1);
            return true;
        }
        if (id == BTN_INSERT && reactor.depth() < RedstoneReactorBlockEntity.DEPTHS - 1) {
            reactor.setDepth(reactor.depth() + 1);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < FIRST_PLAYER_SLOT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
        } else if (RedstoneReactorBlockEntity.isLiveRod(stack)) {
            if (!moveItemStackTo(stack, 0, RedstoneReactorBlockEntity.RODS, false)) return ItemStack.EMPTY;
        } else if (RedstoneReactorBlockEntity.coolantKind(stack) > 0) {
            if (!moveItemStackTo(stack, RedstoneReactorBlockEntity.RODS, FIRST_PLAYER_SLOT, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
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
