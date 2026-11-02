package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.aiot.MiningMode;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.aiot.ToolMenuPayload;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModComponents;

/**
 * The tool menu, opened with G (by default) while holding an AIOT or a Nexium tool: switch between Silk Touch and
 * Fortune, and turn Haste II on or off. The buttons send the change to the server and the labels follow the held
 * tool, so they update as soon as the server has made the change. Switches that don't apply to the tool are greyed
 * out with a tooltip saying why.
 */
public class ToolMenuScreen extends Screen {
    private static final int WIDTH = 180, HEIGHT = 84;
    private int left, top;
    private Button modeButton, hasteButton;

    public ToolMenuScreen() {
        super(Component.translatable("screen.storagenexus.tool_menu"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(ToolMenuPayload.Action.SWAP_SILK_TOUCH_FORTUNE))
                .bounds(left + 8, top + 30, WIDTH - 16, 20).build());
        hasteButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(ToolMenuPayload.Action.TOGGLE_HASTE))
                .bounds(left + 8, top + 56, WIDTH - 16, 20).build());
        refresh();
    }

    private ItemStack held() {
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }

    private void send(ToolMenuPayload.Action action) {
        ClientPlayNetworking.send(new ToolMenuPayload(action));
    }

    @Override
    public void tick() {
        // Closes if the tool is put away (or breaks); otherwise the labels follow the server's changes
        if (!ToolMenu.appliesTo(held())) onClose();
        else refresh();
    }

    private void refresh() {
        ItemStack stack = held();
        MiningMode mode = stack.get(ModComponents.MINING_MODE);
        if (AiotEnchanting.canSwap(stack) && mode != null && minecraft.level != null) {
            var enchantment = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
            modeButton.active = true;
            modeButton.setMessage(Component.translatable("screen.storagenexus.tool_menu.mode",
                    Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(stack, mode))));
            modeButton.setTooltip(Tooltip.create(Component.translatable("screen.storagenexus.tool_menu.mode.tooltip")));
        } else {
            modeButton.active = false;
            modeButton.setMessage(Component.translatable("screen.storagenexus.tool_menu.mode.none"));
            modeButton.setTooltip(Tooltip.create(Component.translatable("screen.storagenexus.tool_menu.mode.none.tooltip")));
        }
        if (stack.is(NexiumMaterials.GRANTS_HASTE)) {
            hasteButton.active = true;
            hasteButton.setMessage(Component.translatable(stack.has(ModComponents.HASTE_OFF)
                    ? "screen.storagenexus.tool_menu.haste_off" : "screen.storagenexus.tool_menu.haste_on"));
            hasteButton.setTooltip(Tooltip.create(Component.translatable("screen.storagenexus.tool_menu.haste.tooltip")));
        } else {
            hasteButton.active = false;
            hasteButton.setMessage(Component.translatable("screen.storagenexus.tool_menu.haste_none"));
            hasteButton.setTooltip(Tooltip.create(Component.translatable("screen.storagenexus.tool_menu.haste_none.tooltip")));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Palette.DARK.panel(g, left, top, WIDTH, HEIGHT);
        Palette.DARK.slot(g, left + 8, top + 8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        ItemStack stack = held();
        g.item(stack, left + 8, top + 8);
        g.text(font, stack.getHoverName(), left + 30, top + 9, Palette.DARK.text(), false);
        g.text(font, title, left + 30, top + 19, 0xFFA0A0A0, false);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
