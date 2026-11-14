package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;
import org.jetbrains.annotations.Nullable;

/** Mode button, the six face buttons and the filter slot of an Import/Export Bus. */
public class NetworkBusMenu extends BaseMenu {
    public static final int BTN_MODE = 0;
    /** The buttons from here on turn the bus: {@code BTN_FACE + direction.get3DDataValue()}. */
    public static final int BTN_FACE = 1;
    /** Where the player's inventory starts, below the controls. */
    public static final int INVENTORY_TOP = 71;

    private final @Nullable NetworkBusBlockEntity be;
    private final BlockPos pos;
    private final ContainerData data;

    /** Client side: the position comes from the opening data. */
    public NetworkBusMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null, pos);
    }

    public NetworkBusMenu(int id, Inventory inventory, NetworkBusBlockEntity be) {
        this(id, inventory, be, be.getBlockPos());
    }

    private NetworkBusMenu(int id, Inventory inventory, @Nullable NetworkBusBlockEntity be, BlockPos pos) {
        super(ModMenus.NETWORK_BUS, id);
        this.be = be;
        this.pos = pos;
        Container filter = be != null ? be.filterContainer() : new SimpleContainer(1);
        addCustomSlot(new Slot(filter, 0, 152, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FilterItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
        // The sides with an inventory or tank to work with, kept up to date while the screen is open
        this.data = be != null ? new ContainerData() {
            @Override public int get(int i) { return be.workableFaces(); }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(data);
    }

    public BlockPos pos() { return pos; }

    /** The sides it could work with, one bit each (bit i is {@code Direction.from3DDataValue(i)}). */
    public int workableFaces() { return data.get(0); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return true;
        if (id == BTN_MODE) be.toggleMode();
        else if (id >= BTN_FACE && id < BTN_FACE + 6) be.face(Direction.from3DDataValue(id - BTN_FACE));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 64);
    }
}
