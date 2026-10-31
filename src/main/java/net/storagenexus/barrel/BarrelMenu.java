package net.storagenexus.barrel;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

/**
 * The barrel's slots over the player inventory. Never taller than a double chest (6 rows):
 * bigger barrels get wider instead, and the inventory is centred under them.
 */
public class BarrelMenu extends BaseMenu {
    public static final int MAX_ROWS = 6;

    private final Container container;
    private final int slots;

    /** Client side: only the slot count travels. */
    public BarrelMenu(int id, Inventory inventory, Integer slots) {
        this(id, inventory, new SimpleContainer(slots), slots);
    }

    public BarrelMenu(int id, Inventory inventory, Container container, int slots) {
        super(ModMenus.BARREL, id);
        this.container = container;
        this.slots = slots;
        container.startOpen(inventory.player);
        int cols = columns(slots);
        for (int i = 0; i < slots; i++) addCustomSlot(new Slot(container, i, 8 + (i % cols) * 18, 18 + (i / cols) * 18));
        addPlayerSlots(inventory, (imageWidth(slots) - 162) / 2, inventoryTop(slots));
    }

    public static int rows(int slots) { return Math.min(MAX_ROWS, (slots + 8) / 9); }
    public static int columns(int slots) { int r = rows(slots); return (slots + r - 1) / r; }
    public static int imageWidth(int slots) { return Math.max(176, 16 + columns(slots) * 18); }
    public static int inventoryTop(int slots) { return 32 + rows(slots) * 18; }
    public static int imageHeight(int slots) { return inventoryTop(slots) + 82; }
    public int slotCount() { return slots; }

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
