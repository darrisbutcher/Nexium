package net.storagenexus.energy;

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

/** The Battery's screen: how much it holds, and the player inventory. */
public class BatteryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int INVENTORY_TOP = 84;
    /** Screen values travel as 16-bit numbers, so each long is sent as two halves of 15 bits. */
    private static final int D_STORED = 0, D_CAPACITY = 2, D_COUNT = 4;

    private final ContainerData data;
    private final @Nullable BatteryBlockEntity battery;

    /** Client side. */
    public BatteryMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (BatteryBlockEntity) null);
    }

    /** Server side. */
    public BatteryMenu(int id, Inventory inventory, @Nullable BatteryBlockEntity battery) {
        super(ModMenus.BATTERY, id);
        this.battery = battery;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = battery == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_STORED -> (int) (battery.energy().stored() & 0x7FFF);
                    case D_STORED + 1 -> (int) (battery.energy().stored() >> 15);
                    case D_CAPACITY -> (int) (battery.energy().capacity() & 0x7FFF);
                    case D_CAPACITY + 1 -> (int) (battery.energy().capacity() >> 15);
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

    public int stored() { return data.get(D_STORED) | data.get(D_STORED + 1) << 15; }

    public int capacity() { return data.get(D_CAPACITY) | data.get(D_CAPACITY + 1) << 15; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return battery == null || (!battery.isRemoved() && player.distanceToSqr(battery.getBlockPos().getX() + 0.5,
                battery.getBlockPos().getY() + 0.5, battery.getBlockPos().getZ() + 0.5) <= 64);
    }
}
