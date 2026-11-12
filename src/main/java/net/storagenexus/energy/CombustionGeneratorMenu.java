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

/** The Combustion Generator's screen: the fuel slot, the flame, the energy it holds and the player inventory. */
public class CombustionGeneratorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int FUEL_X = 44, FUEL_Y = 46;
    public static final int INVENTORY_TOP = 84;
    /** Screen values travel as 16-bit numbers, so the energy is sent as two halves of 15 bits. */
    private static final int D_BURN = 0, D_ENERGY = 1, D_COUNT = 3;

    private final Container container;
    private final ContainerData data;

    /** Client side. */
    public CombustionGeneratorMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, new SimpleContainer(1), null);
    }

    /** Server side. */
    public CombustionGeneratorMenu(int id, Inventory inventory, CombustionGeneratorBlockEntity generator) {
        this(id, inventory, generator, generator);
    }

    private CombustionGeneratorMenu(int id, Inventory inventory, Container container, @Nullable CombustionGeneratorBlockEntity generator) {
        super(ModMenus.COMBUSTION_GENERATOR, id);
        this.container = container;
        container.startOpen(inventory.player);
        addSlot(new Slot(container, 0, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CombustionGeneratorBlockEntity.isFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = generator == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_BURN -> generator.burnTotal() <= 0 ? 0 : Math.round(1000.0F * generator.burnLeft() / generator.burnTotal());
                    case D_ENERGY -> (int) (generator.energy().stored() & 0x7FFF);
                    case D_ENERGY + 1 -> (int) (generator.energy().stored() >> 15);
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

    /** How much of the burning piece is left, 0 to 1 (0 when nothing burns). */
    public float burn() { return data.get(D_BURN) / 1000.0F; }

    public int energy() { return data.get(D_ENERGY) | data.get(D_ENERGY + 1) << 15; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
