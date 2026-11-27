package net.nexium.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nexium.autocraft.AutocrafterBlockEntity;
import net.nexium.autocraft.AutocrafterMenu;
import net.nexium.util.Fmt;

/** The Autocrafter: nine pattern slots and its energy. */
public class AutocrafterScreen extends BaseScreen<AutocrafterMenu> {
    public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, AutocrafterMenu.INVENTORY_TOP + 82, AutocrafterMenu.INVENTORY_TOP);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, Component.translatable("screen.nexium.autocrafter.patterns"), leftPos + 8, topPos + 22, palette().text(), false);
        g.text(font, Component.literal(Fmt.compact(menu.stored()) + " / " + Fmt.compact(AutocrafterBlockEntity.BUFFER) + " NE"),
                leftPos + 8, topPos + 80, 0xFF55D86B, false);
    }
}
