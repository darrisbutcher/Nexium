package net.nexium.energy;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;

/**
 * The screen of an item with a slot for one battery cell (the Jetpack, the Crafting Storage Tablet). The cell lives in the item
 * ({@code nexium:installed_cell}); this screen's slot is a window on it, kept in step with the item both ways.
 */
public class CellMenu extends BaseMenu {
    public static final int INVENTORY_TOP = 84;
    public static final int CELL_X = 80, CELL_Y = 30;

    private final int source;
    private final Player player;
    private final SimpleContainer cell = new SimpleContainer(1) {
        @Override public void setChanged() {
            super.setChanged();
            cellChanged();
        }
    };
    private boolean syncing;

    public CellMenu(int id, Inventory inventory, Integer source) {
        super(ModMenus.CELL, id);
        this.source = source;
        this.player = inventory.player;
        if (!player.level().isClientSide()) {
            syncing = true;
            cell.setItem(0, ItemEnergy.cell(holder()));
            syncing = false;
        }
        addCustomSlot(new Slot(cell, 0, CELL_X, CELL_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BatteryCellItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
    }

    /** The cell slot changed (a player moved a cell): the item takes it. */
    private void cellChanged() {
        if (syncing || player.level().isClientSide()) return;
        ItemStack held = holder();
        if (held.getItem() instanceof CellHolder) ItemEnergy.setCell(held, cell.getItem(0));
    }

    /** The item this screen is for (the real stack). */
    public ItemStack holder() {
        return CellSource.find(player, source);
    }

    /** What the item draws and when it was last seen: the energy it uses, for the screen's hint. */
    public CellHolder holderItem() {
        return holder().getItem() instanceof CellHolder h ? h : null;
    }

    public ItemStack cellStack() {
        return cell.getItem(0);
    }

    /** The item can use charge while this is open (a Jetpack hovering), so the slot follows what the item holds. */
    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            ItemStack inside = ItemEnergy.cell(holder());
            if (!ItemStack.matches(inside, cell.getItem(0))) {
                syncing = true;
                try {
                    cell.setItem(0, inside);
                } finally {
                    syncing = false;
                }
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player p) {
        return holder().getItem() instanceof CellHolder;
    }
}
