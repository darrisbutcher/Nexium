package net.nexium.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;
import org.jspecify.annotations.Nullable;

/** The Charging Station's screen: its four slots, what it holds, and the player inventory. */
public class ChargingStationMenu extends BaseMenu {
    public static final int INVENTORY_TOP = 96;
    public static final int SLOT_Y = 24;
    /** Screen values travel as 16-bit numbers, so the stored amount is sent as two halves of 15 bits. */
    private static final int D_STORED = 0, D_OFF = 2, D_COUNT = 3;
    /** Switches the station off or on. */
    public static final int BTN_SWITCH = 0;

    private final ContainerData data;
    private final @Nullable ChargingStationBlockEntity station;

    /** Client side. */
    public ChargingStationMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ChargingStationBlockEntity) null);
    }

    /** Server side. */
    public ChargingStationMenu(int id, Inventory inventory, @Nullable ChargingStationBlockEntity station) {
        super(ModMenus.CHARGING_STATION, id);
        this.station = station;
        Container slots = station != null ? station.slots() : new SimpleContainer(ChargingStationBlockEntity.SLOTS);
        for (int i = 0; i < ChargingStationBlockEntity.SLOTS; i++) {
            addCustomSlot(new Slot(slots, i, slotX(i), SLOT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return ItemEnergy.chargeable(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
        data = station == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                if (index == D_OFF) return station.isOff() ? 1 : 0;
                return index == D_STORED ? (int) (station.energy().stored() & 0x7FFF) : (int) (station.energy().stored() >> 15);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() { return D_COUNT; }
        };
        addDataSlots(data);
    }

    public static int slotX(int i) { return 26 + i * 36; }

    /** Whether the station is switched off. */
    public boolean off() { return data.get(D_OFF) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_SWITCH && station != null) {
            station.setOff(!station.isOff());
            return true;
        }
        return false;
    }

    public int stored() { return data.get(D_STORED) | data.get(D_STORED + 1) << 15; }

    @Override
    public boolean stillValid(Player player) {
        return station == null || (!station.isRemoved() && player.distanceToSqr(station.getBlockPos().getX() + 0.5,
                station.getBlockPos().getY() + 0.5, station.getBlockPos().getZ() + 0.5) <= 64);
    }
}
