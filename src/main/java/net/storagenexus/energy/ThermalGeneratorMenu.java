package net.storagenexus.energy;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModMenus;
import org.jspecify.annotations.Nullable;

/** The Thermal Generator's screen: the lava tank, the energy it holds, and the player inventory. */
public class ThermalGeneratorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int INVENTORY_TOP = 84;
    /** Screen values travel as 16-bit numbers, so the energy is sent as two halves of 15 bits. */
    private static final int D_BURN = 0, D_ENERGY = 1, D_LAVA = 3, D_COUNT = 4;

    private final @Nullable ThermalGeneratorBlockEntity generator;
    private final ContainerData data;

    /** Client side. */
    public ThermalGeneratorMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ThermalGeneratorBlockEntity) null);
    }

    /** Server side. */
    public ThermalGeneratorMenu(int id, Inventory inventory, @Nullable ThermalGeneratorBlockEntity generator) {
        super(ModMenus.THERMAL_GENERATOR, id);
        this.generator = generator;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = generator == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_BURN -> generator.burnLeft();
                    case D_ENERGY -> (int) (generator.energy().stored() & 0x7FFF);
                    case D_ENERGY + 1 -> (int) (generator.energy().stored() >> 15);
                    // Millibuckets: at most 4000
                    case D_LAVA -> (int) (generator.lava() * 1000 / FluidConstants.BUCKET);
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

    public boolean burning() { return data.get(D_BURN) > 0; }

    public int energy() { return data.get(D_ENERGY) | data.get(D_ENERGY + 1) << 15; }

    public int lavaMilli() { return data.get(D_LAVA); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return generator == null || (!generator.isRemoved() && player.distanceToSqr(generator.getBlockPos().getX() + 0.5,
                generator.getBlockPos().getY() + 0.5, generator.getBlockPos().getZ() + 0.5) <= 64);
    }
}
