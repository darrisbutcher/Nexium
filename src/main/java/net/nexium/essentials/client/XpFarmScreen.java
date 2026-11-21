package net.nexium.essentials.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.nexium.client.screen.Palette;
import net.nexium.essentials.farm.Overclock;
import net.nexium.essentials.xpfarm.ExperienceMath;
import net.nexium.essentials.xpfarm.XpFarmBlockEntity;
import net.nexium.essentials.xpfarm.XpFarmMenu;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** The Experience Farm's screen, drawn with plain rectangles like the other machines': the bank, the energy and three buttons. */
public class XpFarmScreen extends AbstractContainerScreen<XpFarmMenu> {
    private static final Palette PALETTE = Palette.DARK;
    private static final int MUTED = 0xFFA0A0A0, WARN = 0xFFFF6A5A, GOOD = 0xFF8CE060;
    private static final int XP = 0xFF58C828, XP_TOP = 0xFFB4F878, ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int BAR_X = 8, XP_Y = 29, XP_H = 8, ENERGY_Y = 53, ENERGY_H = 6, BAR_W = 160;
    private static final int COLLECT_Y = 76, LEVELS_Y = 92, BUTTON_H = 14;

    private Button collectAll, level1, level10, overclockButton;

    public XpFarmScreen(XpFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, XpFarmMenu.WIDTH, XpFarmMenu.HEIGHT);
        inventoryLabelY = XpFarmMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();
        collectAll = button(8, COLLECT_Y, BAR_W, Component.translatable("screen.nexium.xp_farm.collect_all", number(menu.experience())),
                XpFarmMenu.BTN_COLLECT_ALL, "screen.nexium.xp_farm.collect_all.tip");
        level1 = button(8, LEVELS_Y, 79, Component.translatable("screen.nexium.xp_farm.level_1"),
                XpFarmMenu.BTN_LEVEL_1, "screen.nexium.xp_farm.level.tip");
        level10 = button(89, LEVELS_Y, 79, Component.translatable("screen.nexium.xp_farm.level_10"),
                XpFarmMenu.BTN_LEVEL_10, "screen.nexium.xp_farm.level.tip");
        overclockButton = addRenderableWidget(OverclockUi.create(leftPos, topPos, imageWidth, menu.containerId, XpFarmMenu.BTN_OVERCLOCK, XpFarmMenu.BTN_OVERCLOCK_BACK));
        updateButtons();
    }

    private Button button(int x, int y, int width, Component label, int id, String tip) {
        Button b = addRenderableWidget(Button.builder(label, btn -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, width, BUTTON_H).build());
        b.setTooltip(Tooltip.create(Component.translatable(tip)));
        return b;
    }

    private void updateButtons() {
        if (collectAll == null) return;
        OverclockUi.refresh(overclockButton, menu.overclock(), XpFarmBlockEntity.ENERGY_PER_TICK);
        collectAll.setMessage(Component.translatable("screen.nexium.xp_farm.collect_all", number(menu.experience())));
        boolean any = menu.experience() > 0;
        collectAll.active = any;
        level1.active = any;
        level10.active = any;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    /** The middle of a button in GUI pixels: 0 collect all, 1 one level, 2 ten levels (for the client test). */
    public int[] buttonCentre(int index) {
        Button b = index == 0 ? collectAll : index == 1 ? level1 : level10;
        return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, PALETTE.text(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PALETTE.text(), false);
        Component status;
        int colour = MUTED;
        if (menu.paused()) {
            status = Component.translatable("screen.nexium.xp_farm.paused");
        } else if (menu.full()) {
            status = Component.translatable("screen.nexium.xp_farm.full");
            colour = WARN;
        } else if (menu.unpowered()) {
            status = Component.translatable("screen.nexium.no_power", XpFarmBlockEntity.ENERGY_PER_TICK);
            colour = WARN;
        } else {
            status = Component.translatable("screen.nexium.xp_farm.working", XpFarmBlockEntity.XP_PER_TICK * 20 * Overclock.multiplier(menu.overclock()));
            colour = GOOD;
        }
        g.text(font, status, BAR_X, 17, colour, false);
        long bank = menu.experience();
        g.text(font, Component.translatable("screen.nexium.xp_farm.bank", number(bank), number(XpFarmBlockEntity.CAPACITY)),
                BAR_X, XP_Y + XP_H + 3, MUTED, false);
        Component level = Component.translatable("screen.nexium.xp_farm.level", ExperienceMath.levelOf(bank));
        g.text(font, level, BAR_X + BAR_W - font.width(level), XP_Y + XP_H + 3, MUTED, false);
        g.text(font, Component.translatable("screen.nexium.xp_farm.energy", number(menu.energy()), number(energyCapacity())),
                BAR_X, ENERGY_Y + ENERGY_H + 3, MUTED, false);
    }

    private long energyCapacity() {
        return Overclock.capacity(XpFarmBlockEntity.ENERGY_PER_TICK, menu.overclock());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        PALETTE.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) PALETTE.slot(g, leftPos + s.x, topPos + s.y);

        bar(g, XP_Y, XP_H, (float) menu.experience() / XpFarmBlockEntity.CAPACITY, XP, XP_TOP);
        bar(g, ENERGY_Y, ENERGY_H, (float) menu.energy() / energyCapacity(), ENERGY, ENERGY_TOP);
    }

    private void bar(GuiGraphicsExtractor g, int y, int h, float fraction, int colour, int top) {
        int bx = leftPos + BAR_X, by = topPos + y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + h + 1, PALETTE.slotDark());
        g.fill(bx, by, bx + BAR_W, by + h, PALETTE.slotFill());
        int filled = Math.round(BAR_W * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            g.fill(bx, by, bx + filled, by + h, colour);
            g.fill(bx, by, bx + filled, by + 1, top);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(BAR_X, ENERGY_Y, BAR_W, ENERGY_H, mouseX, mouseY)) {
            net.nexium.client.Tooltips.show(g, font, List.of(
                    Component.translatable("screen.nexium.xp_farm.energy", number(menu.energy()), number(energyCapacity())),
                    Component.translatable("screen.nexium.xp_farm.energy.tip", XpFarmBlockEntity.ENERGY_PER_TICK * Overclock.multiplier(menu.overclock()),
                            XpFarmBlockEntity.XP_PER_TICK * 20 * Overclock.multiplier(menu.overclock())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    /** The middle of the Overclock button in GUI pixels (for the client test). */
    public int[] overclockCentre() {
        return new int[]{overclockButton.getX() + overclockButton.getWidth() / 2, overclockButton.getY() + overclockButton.getHeight() / 2};
    }
}
