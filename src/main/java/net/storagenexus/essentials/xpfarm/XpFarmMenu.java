package net.storagenexus.essentials.xpfarm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.registry.ModMenus;
import org.jspecify.annotations.Nullable;

/**
 * The Experience Farm screen: the bank and the energy, and three buttons to collect. There are no item slots but the
 * player's own. Values travel as 16-bit numbers, so the bank and the energy are each sent as two halves of 15 bits.
 */
public class XpFarmMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 204;
    public static final int INVENTORY_TOP = 122;
    /** Buttons: collect everything, or just enough for 1 or 10 levels. */
    public static final int BTN_COLLECT_ALL = 0, BTN_LEVEL_1 = 1, BTN_LEVEL_10 = 2;

    private static final int D_XP = 0, D_ENERGY = 2, D_FLAGS = 4, D_COUNT = 5;
    private static final int FLAG_FULL = 1, FLAG_PAUSED = 2, FLAG_UNPOWERED = 4, FLAG_WORKING = 8;

    private final ContainerData data;
    private final @Nullable XpFarmBlockEntity farm;
    private final BlockPos pos;

    /** Client side. */
    public XpFarmMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null, pos);
    }

    /** Server side. */
    public XpFarmMenu(int id, Inventory inventory, XpFarmBlockEntity farm) {
        this(id, inventory, farm, farm.getBlockPos());
    }

    private XpFarmMenu(int id, Inventory inventory, @Nullable XpFarmBlockEntity farm, BlockPos pos) {
        super(ModMenus.XP_FARM, id);
        this.farm = farm;
        this.pos = pos;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_TOP + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_TOP + 58));
        data = farm == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_XP -> (int) (farm.experience() & 0x7FFF);
                    case D_XP + 1 -> (int) (farm.experience() >> 15);
                    case D_ENERGY -> (int) (farm.energy().stored() & 0x7FFF);
                    case D_ENERGY + 1 -> (int) (farm.energy().stored() >> 15);
                    case D_FLAGS -> (farm.isFull() ? FLAG_FULL : 0) | (farm.isPaused() ? FLAG_PAUSED : 0)
                            | (farm.isUnpowered() ? FLAG_UNPOWERED : 0) | (farm.isWorking() ? FLAG_WORKING : 0);
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

    public BlockPos pos() { return pos; }

    /** Experience points banked. */
    public long experience() { return data.get(D_XP) | (long) data.get(D_XP + 1) << 15; }
    public long energy() { return data.get(D_ENERGY) | (long) data.get(D_ENERGY + 1) << 15; }
    public boolean full() { return (data.get(D_FLAGS) & FLAG_FULL) != 0; }
    public boolean paused() { return (data.get(D_FLAGS) & FLAG_PAUSED) != 0; }
    /** Waiting for Nexus Energy. */
    public boolean unpowered() { return (data.get(D_FLAGS) & FLAG_UNPOWERED) != 0; }
    public boolean working() { return (data.get(D_FLAGS) & FLAG_WORKING) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (farm == null) return false;
        switch (id) {
            case BTN_COLLECT_ALL -> farm.takeAll(player);
            case BTN_LEVEL_1 -> farm.takeLevels(player, 1);
            case BTN_LEVEL_10 -> farm.takeLevels(player, 10);
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return farm == null || (!farm.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(farm.getBlockPos())) <= 64);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
