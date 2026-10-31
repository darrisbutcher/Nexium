package net.storagenexus.barrel;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

/** A grid of 9 columns and 4 to 7 rows over the player inventory. */
public class BarrelMenu extends BaseMenu {
    private final Container container;
    private final int rows;

    /** Client side: only the row count travels. */
    public BarrelMenu(int id, Inventory inventory, Integer rows) {
        this(id, inventory, new SimpleContainer(rows * 9), rows);
    }

    public BarrelMenu(int id, Inventory inventory, Container container, int rows) {
        super(ModMenus.BARREL, id);
        this.container = container;
        this.rows = rows;
        container.startOpen(inventory.player);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) addCustomSlot(new Slot(container, c + r * 9, 8 + c * 18, 18 + r * 18));
        }
        addPlayerSlots(inventory, 8, inventoryTop(rows));
    }

    public static int inventoryTop(int rows) { return 32 + rows * 18; }
    public static int imageHeight(int rows) { return inventoryTop(rows) + 82; }
    public int rows() { return rows; }

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
