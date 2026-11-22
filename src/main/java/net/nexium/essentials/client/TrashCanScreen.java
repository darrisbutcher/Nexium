package net.nexium.essentials.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.nexium.client.screen.BaseScreen;
import net.nexium.essentials.trash.TrashCanMenu;

/** The Trash Can: one slot (drop an item on it to destroy it), what it does, and the player's inventory. */
public class TrashCanScreen extends BaseScreen<TrashCanMenu> {
    private static final int MUTED = 0xFFAAAAAA, WARN = 0xFFFF8A5A;

    public TrashCanScreen(TrashCanMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TrashCanMenu.WIDTH, TrashCanMenu.HEIGHT, TrashCanMenu.INVENTORY_Y);
    }

    /** The centre of a slot, in screen coordinates (for tests, which click it with the real mouse). */
    public int[] slotCentre(Slot slot) {
        return new int[]{leftPos + slot.x + 8, topPos + slot.y + 8};
    }

    /** The centre of the destroying slot. */
    public int[] trashSlotCentre() {
        return slotCentre(menu.getSlot(TrashCanMenu.TRASH_SLOT));
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int y = topPos + 46;
        for (FormattedCharSequence line : font.split(Component.translatable("screen.nexium.trash_can.help"), TrashCanMenu.WIDTH - 20)) {
            g.centeredText(font, line, leftPos + TrashCanMenu.WIDTH / 2, y, MUTED);
            y += 10;
        }
        g.centeredText(font, Component.translatable("screen.nexium.trash_can.warn"), leftPos + TrashCanMenu.WIDTH / 2, y + 2, WARN);
    }
}
