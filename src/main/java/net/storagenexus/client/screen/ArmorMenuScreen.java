package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.nexium.ArmorBonus;
import net.storagenexus.nexium.ArmorBonusPayload;
import net.storagenexus.nexium.NexiumArmor;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/**
 * The armor menu: Nexium armor's bonuses, each with an on/off switch. Opened with the tool menu key (G) with no tool in
 * hand, or from the tool menu's Armor button. Labels follow the worn armor, so they update as soon as the server has
 * made a change; bonuses the worn pieces don't give are greyed out, and every hint shows beside the panel.
 */
public class ArmorMenuScreen extends Screen {
    private static final int WIDTH = 200, ROW = 22, TOP = 32;
    private int left, top, height_;
    private final Button[] buttons = new Button[ArmorBonus.values().length];
    private final SideHints hints = new SideHints();

    public ArmorMenuScreen() {
        super(Component.translatable("screen.storagenexus.armor_menu"));
    }

    @Override
    protected void init() {
        height_ = TOP + ArmorBonus.values().length * ROW + 6;
        left = (width - WIDTH) / 2;
        top = (height - height_) / 2;
        for (ArmorBonus bonus : ArmorBonus.values()) {
            buttons[bonus.ordinal()] = addRenderableWidget(Button.builder(Component.empty(),
                            b -> ClientPlayNetworking.send(new ArmorBonusPayload(bonus)))
                    .bounds(left + 8, top + TOP + bonus.ordinal() * ROW, WIDTH - 16, 18).build());
        }
        refresh();
    }

    @Override
    public void tick() {
        refresh();
    }

    private void refresh() {
        Player player = minecraft.player;
        if (player == null) return;
        for (ArmorBonus bonus : ArmorBonus.values()) {
            Button button = buttons[bonus.ordinal()];
            Component name = Component.translatable("armor_bonus.storagenexus." + bonus.id());
            Component hint = Component.translatable("armor_bonus.storagenexus." + bonus.id() + ".desc");
            if (!NexiumArmor.available(player, bonus)) {
                button.active = false;
                button.setMessage(Component.translatable(bonus.needsFullSet()
                        ? "screen.storagenexus.armor_menu.needs_set" : "screen.storagenexus.armor_menu.needs_helmet", name));
            } else {
                button.active = true;
                button.setMessage(Component.translatable(NexiumArmor.switchedOn(player, bonus)
                        ? "screen.storagenexus.armor_menu.on" : "screen.storagenexus.armor_menu.off", name));
            }
            if (bonus == ArmorBonus.NEXUS_RESTOCK) {
                GlobalPos link = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NEXIUM_CHESTPLATE)
                        ? player.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.TERMINAL_LINK) : null;
                hint = Component.empty().append(hint).append("\n").append(link == null
                        ? Component.translatable("tooltip.storagenexus.nexus_restock.unlinked")
                        : Component.translatable("tooltip.storagenexus.nexus_restock.linked", ModuleItem.describe(link)));
            }
            hints.set(button, hint);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Palette.DARK.panel(g, left, top, WIDTH, height_);
        Palette.DARK.slot(g, left + 8, top + 7);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Player player = minecraft.player;
        g.item(new ItemStack(ModItems.NEXIUM_CHESTPLATE), left + 8, top + 7);
        g.text(font, title, left + 30, top + 8, Palette.DARK.text(), false);
        int worn = player == null ? 0 : NexiumArmor.piecesWorn(player);
        g.text(font, Component.translatable("screen.storagenexus.armor_menu.pieces", worn), left + 30, top + 18, 0xFFA0A0A0, false);
        hints.render(g, left, left + WIDTH, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
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
