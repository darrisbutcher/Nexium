package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;
import org.jetbrains.annotations.Nullable;

/** Mode button and filter slot of an Import/Export Bus. */
public class NetworkBusMenu extends BaseMenu {
    public static final int BTN_MODE = 0;

    private final @Nullable NetworkBusBlockEntity be;
    private final BlockPos pos;

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
        addPlayerSlots(inventory, 8, 51);
    }

    public BlockPos pos() { return pos; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be != null && id == BTN_MODE) be.toggleMode();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 64);
    }
}
