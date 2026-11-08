package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.builder.BuilderItem;
import net.storagenexus.builder.BuilderMenu;
import net.storagenexus.builder.BuilderSettings;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.registry.ModComponents;

/**
 * The Construction Builder's tool menu: Mode, Plane and Size, Undo (with how many builds it can still take back), and
 * the Nexus Link slot with its on/off switch. Laid out like the AIOT tool menu, hints beside the panel.
 */
public class BuilderScreen extends BaseScreen<BuilderMenu> {
    private static final int BUTTON_X = 8, BUTTON_WIDTH = 160, MODULE_BUTTON_X = 28, MODULE_BUTTON_WIDTH = 140;
    private Button modeButton, planeButton, sizeButton, undoButton, moduleButton;
    private final SideHints hints = new SideHints();

    public BuilderScreen(BuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, BuilderMenu.INVENTORY_Y + 82, BuilderMenu.INVENTORY_Y);
        this.titleLabelX = 30;
        this.titleLabelY = 9;
    }

    @Override
    protected void init() {
        super.init();
        int half = (BUTTON_WIDTH - 2) / 2;
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(BuilderMenu.BTN_MODE))
                .bounds(leftPos + BUTTON_X, topPos + 22, BUTTON_WIDTH, 16).build());
        planeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(BuilderMenu.BTN_PLANE))
                .bounds(leftPos + BUTTON_X, topPos + 40, half, 16).build());
        sizeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(BuilderMenu.BTN_SIZE))
                .bounds(leftPos + BUTTON_X + half + 2, topPos + 40, half, 16).build());
        undoButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(BuilderMenu.BTN_UNDO))
                .bounds(leftPos + BUTTON_X, topPos + 58, BUTTON_WIDTH, 16).build());
        moduleButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(BuilderMenu.BTN_MODULE))
                .bounds(leftPos + MODULE_BUTTON_X, topPos + BuilderMenu.MODULE_SLOT_Y, MODULE_BUTTON_WIDTH, 16).build());
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
        ItemStack builder = menu.tool();
        if (!(builder.getItem() instanceof BuilderItem item)) return;
        set(modeButton, true, Component.translatable("screen.storagenexus.builder.mode",
                Component.translatable("screen.storagenexus.builder.mode." + BuilderSettings.mode(builder).name().toLowerCase())),
                Component.translatable("screen.storagenexus.builder.mode." + BuilderSettings.mode(builder).name().toLowerCase() + ".tooltip"));
        set(planeButton, true, Component.translatable("screen.storagenexus.builder.plane",
                Component.translatable("screen.storagenexus.builder.plane." + BuilderSettings.plane(builder).name().toLowerCase())),
                Component.translatable("screen.storagenexus.builder.plane.tooltip"));
        set(sizeButton, true, Component.translatable("screen.storagenexus.builder.size", BuilderSettings.size(builder), item.tier().maxBlocks()),
                Component.translatable("screen.storagenexus.builder.size.tooltip"));
        int left = menu.undoLeft();
        set(undoButton, left > 0, Component.translatable("screen.storagenexus.builder.undo", left),
                Component.translatable(left > 0 ? "screen.storagenexus.builder.undo.tooltip" : "screen.storagenexus.builder.undo.none"));
        ItemStack module = menu.getSlot(0).getItem();
        if (module.getItem() instanceof ModuleItem) {
            boolean on = !module.has(ModComponents.MODULE_OFF);
            GlobalPos link = module.get(ModComponents.TERMINAL_LINK);
            set(moduleButton, true, Component.translatable(on ? "screen.storagenexus.tool_menu.module_on" : "screen.storagenexus.tool_menu.module_off",
                    module.getHoverName()), Component.empty().append(Component.translatable("screen.storagenexus.builder.module.tooltip"))
                    .append("\n").append(link == null ? Component.translatable("tooltip.storagenexus.nexus_link.unlinked")
                            : Component.translatable("tooltip.storagenexus.nexus_link.linked", ModuleItem.describe(link))));
        } else {
            set(moduleButton, false, Component.translatable("screen.storagenexus.builder.module_empty"),
                    Component.translatable("screen.storagenexus.builder.module_empty.tooltip"));
        }
    }

    private void set(Button button, boolean active, Component message, Component hint) {
        button.active = active;
        button.setMessage(message);
        hints.set(button, hint);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hints.render(g, leftPos, leftPos + imageWidth, mouseX, mouseY);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        palette().slot(g, leftPos + 8, topPos + 5);
        g.item(menu.tool(), leftPos + 8, topPos + 5);
        g.text(font, Component.translatable("screen.storagenexus.tool_menu.modules"), leftPos + 8,
                topPos + BuilderMenu.MODULE_SLOT_Y - 12, palette().text(), false);
        var lines = font.split(Component.translatable("screen.storagenexus.builder.note"), 160);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            g.text(font, lines.get(i), leftPos + 8, topPos + BuilderMenu.NOTE_Y + i * 9, palette().text(), false);
        }
        // The builder's own hotbar slot is outlined: it stays put while the menu is open
        int x = leftPos + 8 + menu.toolSlot() * 18, y = topPos + BuilderMenu.INVENTORY_Y + 58;
        int c = 0xFFB04CFF;
        g.fill(x - 1, y - 1, x + 17, y, c);
        g.fill(x - 1, y + 16, x + 17, y + 17, c);
        g.fill(x - 1, y, x, y + 16, c);
        g.fill(x + 16, y, x + 17, y + 16, c);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ToolMenuClient.TOOL_MENU.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
