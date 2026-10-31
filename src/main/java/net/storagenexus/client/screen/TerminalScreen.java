package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.terminal.TerminalMenu;

/** Storage Terminal: the network grid and your inventory. See {@link TerminalView} for the controls. */
public class TerminalScreen extends BaseScreen<TerminalMenu> {
    private final TerminalView view;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TerminalMenu.WIDTH, TerminalMenu.imageHeight(false, menu.rows()),
                TerminalMenu.inventoryTop(false, menu.rows()));
        this.view = new TerminalView(menu, this.font);
    }

    @Override
    protected Palette palette() {
        return Palette.DARK;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = 6;
        view.init(this::addRenderableWidget, leftPos, topPos);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        view.tick();
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        view.extractBackground(g, leftPos, topPos);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractContents(g, mouseX, mouseY, partialTick);
        view.extractGrid(g, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return view.mouseClicked(event, leftPos, topPos) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return view.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return view.searchConsumesKey(event) || super.keyPressed(event);
    }
}
