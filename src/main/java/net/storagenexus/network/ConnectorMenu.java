package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

/** Priority buttons for both connectors, plus the filter slot on the Filtered Connector. */
public class ConnectorMenu extends BaseMenu {
    public static final int BTN_MINUS_10 = 0, BTN_MINUS_1 = 1, BTN_PLUS_1 = 2, BTN_PLUS_10 = 3;
    private static final int PRIORITY_OFFSET = 1000;

    private final ConnectorBlockEntity be;
    private final ContainerData data;

    public ConnectorMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null);
    }

    public ConnectorMenu(int id, Inventory inventory, ConnectorBlockEntity be) {
        super(ModMenus.CONNECTOR, id);
        this.be = be;
        Container filter = be != null ? be.filterContainer() : new SimpleContainer(1);
        addCustomSlot(new Slot(filter, 0, 152, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FilterItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addPlayerSlots(inventory, 8, 51);
        this.data = be != null ? new ContainerData() {
            @Override public int get(int i) { return i == 0 ? be.priority() + PRIORITY_OFFSET : 0; }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(data);
    }

    public int priority() {
        return data.get(0) - PRIORITY_OFFSET;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return true;
        int delta = switch (id) {
            case BTN_MINUS_10 -> -10;
            case BTN_MINUS_1 -> -1;
            case BTN_PLUS_1 -> 1;
            case BTN_PLUS_10 -> 10;
            default -> 0;
        };
        be.setPriority(be.priority() + delta);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) <= 64);
    }
}
