package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.backpack.BackpackMenu;
import net.storagenexus.essentials.backpack.BackpackModuleItem;
import net.storagenexus.essentials.backpack.BackpackModules;
import net.storagenexus.essentials.client.NexusEssentialsClient;

/**
 * A Backpack's screen: its 36 slots top left, the player's inventory below, and on the right its module slots, each with
 * a button that switches that module on or off. Torch counts above 99 show in full while the Torch Stack module is on.
 * The Backpack's own slot in the inventory is outlined when it was opened from there: it can't be moved while the
 * screen is open. The Open Backpack key closes the screen again.
 */
public class BackpackScreen extends BaseScreen<BackpackMenu> {
    private static final int TOGGLE_X = BackpackMenu.MODULE_X + 20, TOGGLE_WIDTH = 48;
    private final Button[] toggles = new Button[BackpackModules.SLOTS];
    private final SideHints hints = new SideHints();

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BackpackMenu.WIDTH, BackpackMenu.HEIGHT, BackpackMenu.INVENTORY_Y);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            final int slot = i;
            toggles[i] = addRenderableWidget(Button.builder(Component.empty(), b -> click(BackpackMenu.BTN_TOGGLE_MODULE + slot))
                    .bounds(leftPos + TOGGLE_X, topPos + BackpackMenu.MODULE_Y + i * BackpackMenu.MODULE_ROW, TOGGLE_WIDTH, 16).build());
        }
        refresh();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            Button button = toggles[i];
            ItemStack module = menu.getSlot(i).getItem();
            if (!(module.getItem() instanceof BackpackModuleItem item)) {
                button.active = false;
                button.setMessage(Component.translatable("screen.nexium.backpack.module_empty").withStyle(ChatFormatting.DARK_GRAY));
                hints.set(button, Component.translatable("screen.nexium.backpack.module_empty.tooltip"));
                continue;
            }
            boolean on = !module.has(net.storagenexus.registry.ModComponents.MODULE_OFF);
            button.active = true;
            button.setMessage(Component.translatable(on ? "screen.nexium.backpack.module_on" : "screen.nexium.backpack.module_off")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
            hints.set(button, Component.empty()
                    .append(Component.translatable("item.nexium." + item.type().id() + "_module.desc")).append("\n")
                    .append(Component.translatable("screen.nexium.backpack.module_click").withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hints.render(g, leftPos, leftPos + imageWidth, mouseX, mouseY);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.nexium.backpack.modules"), leftPos + BackpackMenu.MODULE_X, topPos + 6, palette().text(), false);
        // Outline the slot the Backpack was opened from: it stays put while the screen is open
        int source = menu.source();
        if (source >= 0 && source < 36) {
            int x = leftPos + BackpackMenu.GRID_X + (source >= 9 ? (source - 9) % 9 : source) * 18;
            int y = topPos + BackpackMenu.INVENTORY_Y + (source >= 9 ? (source - 9) / 9 * 18 : 58);
            int c = 0xFFB04CFF;
            g.fill(x - 1, y - 1, x + 17, y, c);
            g.fill(x - 1, y + 16, x + 17, y + 17, c);
            g.fill(x - 1, y, x, y + 16, c);
            g.fill(x + 16, y, x + 17, y + 16, c);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // The same key closes it again
        if (NexusEssentialsClient.OPEN_BACKPACK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * A count of 1,000 or more is four digits, wider than a slot: it is drawn smaller so it stays inside, like the three-digit ones.
     * (Every other slot is drawn as usual.)
     */
    @Override
    protected void extractSlot(GuiGraphicsExtractor g, net.minecraft.world.inventory.Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        if (!menu.isStorageSlot(slot) || stack.getCount() < 1000) {
            super.extractSlot(g, slot, mouseX, mouseY);
            return;
        }
        int x = slot.x, y = slot.y;
        g.item(stack, x, y, x + y * imageWidth);
        g.itemDecorations(font, stack, x, y, "");
        String count = Integer.toString(stack.getCount());
        float scale = 0.7F;
        g.pose().pushMatrix();
        g.pose().translate(x + 17 - font.width(count) * scale, y + 10);
        g.pose().scale(scale, scale);
        g.text(font, Component.literal(count), 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }
}
