package net.nexium.essentials.trash;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModMenus;

/**
 * The Trash Can's screen: one slot that destroys whatever is put in it (a whole stack at a click, or one item with a right click),
 * and the player's inventory. The slot never holds anything, and a shift-click does nothing here, so an item is only ever destroyed by
 * dropping it on that slot on purpose.
 */
public class TrashCanMenu extends AbstractContainerMenu {
    // Layout, shared with the screen
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int SLOT_X = 80, SLOT_Y = 24, INVENTORY_X = 8, INVENTORY_Y = 84;

    public static final int TRASH_SLOT = 0;

    private final ContainerLevelAccess access;

    /** Client side. */
    public TrashCanMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    /** Server side. */
    public TrashCanMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.TRASH_CAN, id);
        this.access = access;
        addSlot(new Slot(new SimpleContainer(1), 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            /** A whole stack goes in at once, however big. */
            @Override
            public int getMaxStackSize(ItemStack stack) {
                return Integer.MAX_VALUE;
            }

            /** Whatever is put in is destroyed: the slot stays empty. */
            @Override
            public void set(ItemStack stack) {
                if (stack.isEmpty()) return;
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F));
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
    }

    /** Nothing moves by shift-click: items are destroyed only by dropping them on the slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TRASH_CAN);
    }
}
