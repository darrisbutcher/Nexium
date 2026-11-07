package net.storagenexus.essentials.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;

/**
 * The Farm Assembler's screen: the finished-farm slot (take only) and the player's inventory. What it holds travels as
 * data slots (the target, then how much it holds and needs of each requirement); the buttons are menu button clicks.
 */
public class FarmAssemblerMenu extends AbstractContainerMenu {
    // Layout, shared with the screen
    public static final int WIDTH = 340, HEIGHT = 238;
    public static final int LIST_X = 7, LIST_Y = 17, LIST_W = 114, LIST_ROW = 16;
    public static final int ADD_Y = 134, BUTTON_H = 14;
    public static final int RIGHT_X = 128, RIGHT_W = 204;
    public static final int HEADER_Y = 17, MAKES_Y = 37, ROWS_Y = 48, ROW_H = 12;
    public static final int OUTPUT_X = RIGHT_X + RIGHT_W - 17, OUTPUT_Y = HEADER_Y + 1;
    public static final int BUILD_W = 44, BUILD_X = OUTPUT_X - 5 - BUILD_W;
    public static final int INVENTORY_X = (WIDTH - 162) / 2 + 1, INVENTORY_Y = 158;

    // Buttons
    public static final int BUTTON_ADD = 100, BUTTON_BUILD = 101;

    private static final int D_TARGET = 0, D_COUNTS = 1, D_NEEDED = D_COUNTS + AssemblyTarget.MAX_REQUIREMENTS;
    private static final int D_SIZE = D_NEEDED + AssemblyTarget.MAX_REQUIREMENTS;
    private static final int OUTPUT_SLOT = 0, FIRST_PLAYER_SLOT = 1;

    private final @Nullable FarmAssemblerBlockEntity assembler;
    private final Container output;
    private final ContainerData data;

    /** Client side. */
    public FarmAssemblerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null, new SimpleContainer(1), new SimpleContainerData(D_SIZE));
    }

    /** Server side. */
    public FarmAssemblerMenu(int id, Inventory inventory, FarmAssemblerBlockEntity assembler) {
        this(id, inventory, assembler, assembler.output(), new ContainerData() {
            @Override
            public int get(int index) {
                if (index == D_TARGET) return assembler.target().ordinal();
                List<Requirement> reqs = assembler.target().requirements();
                if (index < D_NEEDED) {
                    int i = index - D_COUNTS;
                    return i < reqs.size() ? assembler.counts()[i] : 0;
                }
                int i = index - D_NEEDED;
                return i < reqs.size() ? reqs.get(i).needed() : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return D_SIZE;
            }
        });
    }

    private FarmAssemblerMenu(int id, Inventory inventory, @Nullable FarmAssemblerBlockEntity assembler, Container output, ContainerData data) {
        super(ModMenus.FARM_ASSEMBLER, id);
        this.assembler = assembler;
        this.output = output;
        this.data = data;
        addSlot(new Slot(output, 0, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        addDataSlots(data);
    }

    public AssemblyTarget target() {
        return AssemblyTarget.byIndex(data.get(D_TARGET));
    }

    /** How much it holds towards requirement {@code i}. */
    public int count(int i) {
        return data.get(D_COUNTS + i);
    }

    /** How much requirement {@code i} needs (the server's numbers, which can differ from this client's config). */
    public int needed(int i) {
        return data.get(D_NEEDED + i);
    }

    public boolean complete() {
        int n = target().requirements().size();
        for (int i = 0; i < n; i++) if (count(i) < needed(i)) return false;
        return true;
    }

    public boolean outputFull() {
        return !output.getItem(0).isEmpty();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (assembler == null) return false;
        if (id >= 0 && id < AssemblyTarget.values().length) {
            assembler.select(AssemblyTarget.byIndex(id), player);
            return true;
        }
        if (id == BUTTON_ADD) {
            assembler.addFrom(player);
            return true;
        }
        if (id == BUTTON_BUILD && player.level() instanceof ServerLevel level) {
            return assembler.build(level);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return assembler == null || Container.stillValidBlockEntity(assembler, player);
    }

    /** Shift-click: the finished farm goes to the inventory; from the inventory, what the target still needs goes in. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index == OUTPUT_SLOT) {
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, stack);
            return original;
        }
        if (assembler != null) {
            int taken = assembler.accept(stack);
            if (taken > 0) {
                stack.shrink(taken);
                slot.setChanged();
            }
        }
        return ItemStack.EMPTY;
    }
}
