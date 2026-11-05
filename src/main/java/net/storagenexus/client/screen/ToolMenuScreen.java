package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.aiot.MiningMode;
import net.storagenexus.aiot.ToolMenuMenu;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.nexium.NexiumArmor;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModComponents;

/**
 * The tool menu, opened with G (by default) while holding an AIOT or a Nexium tool. At the top, Silk Touch / Fortune
 * and Haste II; below, on an AIOT, its module slots, each with a switch to turn that module on or off. Labels follow
 * the held tool, so they update as soon as the server has made a change. Switches that don't apply to the tool are
 * greyed out, and every button's hint shows beside the panel, never over the other buttons. The tool's own hotbar slot is outlined: it can't be moved while the menu is open.
 */
public class ToolMenuScreen extends BaseScreen<ToolMenuMenu> {
    private static final int BUTTON_X = 8, BUTTON_WIDTH = 160, MODULE_BUTTON_X = 28, MODULE_BUTTON_WIDTH = 140;
    private static final int MODULES_LABEL_Y = ToolMenuMenu.MODULE_SLOT_Y - 12;
    private Button modeButton, hasteButton, armorButton, veinButton, tunnelButton;
    private final Button[] moduleButtons = new Button[AiotModules.SLOTS];
    private final SideHints hints = new SideHints();

    public ToolMenuScreen(ToolMenuMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, ToolMenuMenu.INVENTORY_Y + 82, ToolMenuMenu.INVENTORY_Y);
        this.titleLabelX = 30;
        this.titleLabelY = 9;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ToolMenuMenu.BTN_SWAP_SILK_TOUCH_FORTUNE))
                .bounds(leftPos + BUTTON_X, topPos + 22, BUTTON_WIDTH, 16).build());
        hasteButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ToolMenuMenu.BTN_TOGGLE_HASTE))
                .bounds(leftPos + BUTTON_X, topPos + 40, BUTTON_WIDTH, 16).build());
        // Vein size and tunnel length share a row, a half each
        int half = (BUTTON_WIDTH - 2) / 2;
        veinButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ToolMenuMenu.BTN_VEIN_SIZE))
                .bounds(leftPos + BUTTON_X, topPos + 58, half, 16).build());
        tunnelButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ToolMenuMenu.BTN_TUNNEL))
                .bounds(leftPos + BUTTON_X + half + 2, topPos + 58, half, 16).build());
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            final int slot = i;
            moduleButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> click(ToolMenuMenu.BTN_TOGGLE_MODULE + slot))
                    .bounds(leftPos + MODULE_BUTTON_X, topPos + ToolMenuMenu.MODULE_SLOT_Y + i * ToolMenuMenu.MODULE_ROW,
                            MODULE_BUTTON_WIDTH, 16).build());
        }
        // To the armor menu, when wearing Nexium armor
        armorButton = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.tool_menu.armor"), b -> {
            minecraft.player.closeContainer();
            minecraft.gui.setScreen(new ArmorMenuScreen());
        }).bounds(leftPos + imageWidth - 8 - 44, topPos + 5, 44, 16).build());
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
        ItemStack tool = menu.tool();
        MiningMode mode = tool.get(ModComponents.MINING_MODE);
        if (AiotEnchanting.canSwap(tool) && mode != null && minecraft.level != null) {
            var enchantment = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
            set(modeButton, true, Component.translatable("screen.storagenexus.tool_menu.mode",
                    Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(tool, mode))), "screen.storagenexus.tool_menu.mode.tooltip");
        } else {
            set(modeButton, false, Component.translatable("screen.storagenexus.tool_menu.mode.none"), "screen.storagenexus.tool_menu.mode.none.tooltip");
        }
        if (tool.is(NexiumMaterials.GRANTS_HASTE)) {
            set(hasteButton, true, Component.translatable(tool.has(ModComponents.HASTE_OFF)
                    ? "screen.storagenexus.tool_menu.haste_off" : "screen.storagenexus.tool_menu.haste_on"), "screen.storagenexus.tool_menu.haste.tooltip");
        } else {
            set(hasteButton, false, Component.translatable("screen.storagenexus.tool_menu.haste_none"), "screen.storagenexus.tool_menu.haste_none.tooltip");
        }
        armorButton.visible = NexiumArmor.piecesWorn(minecraft.player) > 0;
        hints.set(armorButton, Component.translatable("screen.storagenexus.tool_menu.armor.tooltip"));
        boolean aiot = tool.getItem() instanceof AiotItem;
        veinButton.visible = aiot;
        ItemStack vein = ItemStack.EMPTY;
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            if (menu.getSlot(i).getItem().getItem() instanceof ModuleItem m && m.type() == ModuleType.VEIN_MINING) vein = menu.getSlot(i).getItem();
        }
        tunnelButton.visible = aiot;
        ItemStack area = ItemStack.EMPTY;
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            if (menu.getSlot(i).getItem().getItem() instanceof ModuleItem m && m.type() == ModuleType.AREA_MINING) area = menu.getSlot(i).getItem();
        }
        if (area.isEmpty()) {
            set(tunnelButton, false, Component.translatable("screen.storagenexus.tool_menu.tunnel_none"), "screen.storagenexus.tool_menu.tunnel_none.tooltip");
        } else {
            int length = Math.max(1, Math.min(menu.tunnelMax(), area.getOrDefault(ModComponents.TUNNEL_LENGTH, 1)));
            set(tunnelButton, true, Component.translatable(length == 1 ? "screen.storagenexus.tool_menu.tunnel_off"
                    : "screen.storagenexus.tool_menu.tunnel_length", length), "screen.storagenexus.tool_menu.tunnel.tooltip");
        }
        if (vein.isEmpty()) {
            set(veinButton, false, Component.translatable("screen.storagenexus.tool_menu.vein_none"), "screen.storagenexus.tool_menu.vein_none.tooltip");
        } else {
            int max = menu.veinMax(); // the server's limit, synced with the menu
            int size = Math.max(1, Math.min(max, vein.getOrDefault(ModComponents.VEIN_SIZE, max)));
            set(veinButton, true, Component.translatable("screen.storagenexus.tool_menu.vein_size", size),
                    "screen.storagenexus.tool_menu.vein_size.tooltip");
        }
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            Button button = moduleButtons[i];
            button.visible = aiot;
            ItemStack module = menu.getSlot(i).getItem();
            if (!(module.getItem() instanceof ModuleItem item)) {
                set(button, false, Component.translatable("screen.storagenexus.tool_menu.module_empty"), "screen.storagenexus.tool_menu.module_empty.tooltip");
                continue;
            }
            boolean on = !module.has(ModComponents.MODULE_OFF);
            button.active = true;
            button.setMessage(Component.translatable(on ? "screen.storagenexus.tool_menu.module_on" : "screen.storagenexus.tool_menu.module_off",
                    module.getHoverName()));
            Component tip = Component.translatable("item.storagenexus." + item.type().id() + "_module.desc");
            if (item.type() == ModuleType.NEXUS_LINK) {
                GlobalPos link = module.get(ModComponents.TERMINAL_LINK);
                tip = Component.empty().append(tip).append("\n").append(link == null
                        ? Component.translatable("tooltip.storagenexus.nexus_link.unlinked")
                        : Component.translatable("tooltip.storagenexus.nexus_link.linked", ModuleItem.describe(link)));
            }
            hints.set(button, tip);
        }
    }

    private void set(Button button, boolean active, Component message, String hintKey) {
        button.active = active;
        button.setMessage(message);
        hints.set(button, Component.translatable(hintKey));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hints.render(g, leftPos, leftPos + imageWidth, mouseX, mouseY);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // The tool, top left
        palette().slot(g, leftPos + 8, topPos + 5);
        g.item(menu.tool(), leftPos + 8, topPos + 5);
        Component modules = Component.translatable(menu.tool().getItem() instanceof AiotItem
                ? "screen.storagenexus.tool_menu.modules" : "screen.storagenexus.tool_menu.modules_aiot_only");
        g.text(font, modules, leftPos + 8, topPos + MODULES_LABEL_Y, palette().text(), false);
        // Outline the tool's own hotbar slot: it stays put while the menu is open
        int x = leftPos + 8 + menu.toolSlot() * 18, y = topPos + ToolMenuMenu.INVENTORY_Y + 58;
        int c = 0xFFB04CFF;
        g.fill(x - 1, y - 1, x + 17, y, c);
        g.fill(x - 1, y + 16, x + 17, y + 17, c);
        g.fill(x - 1, y, x, y + 16, c);
        g.fill(x + 16, y, x + 17, y + 16, c);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // The same key closes it again
        if (ToolMenuClient.TOOL_MENU.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
