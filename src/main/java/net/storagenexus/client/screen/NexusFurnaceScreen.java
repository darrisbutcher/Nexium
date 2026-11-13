package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.storagenexus.energy.NexusFurnaceBlockEntity;
import net.storagenexus.energy.NexusFurnaceMenu;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** The Nexus Furnace's screen, drawn with plain rectangles like the other machines': eight slots, a bar each, energy and experience. */
public class NexusFurnaceScreen extends AbstractContainerScreen<NexusFurnaceMenu> {
    private static final int OUTLINE = 0xFF000000, BODY = 0xFF535353, HIGHLIGHT = 0xFF6B6B6B, SHADOW = 0xFF262626;
    private static final int SLOT_FILL = 0xFF333333, SLOT_DARK = 0xFF262626, SLOT_LIGHT = 0xFF6B6B6B;
    private static final int TEXT = 0xFFE0E0E0, MUTED = 0xFFA0A0A0, WARN = 0xFFFF6A5A;
    private static final int COOK = 0xFFF58C28, COOK_HOT = 0xFFFFD65A, ENERGY = 0xFFE02828, ENERGY_TOP = 0xFFFF7A6A;
    private static final int BAR_X = 8, BAR_Y = 64, BAR_W = 160, BAR_H = 6;

    public NexusFurnaceScreen(NexusFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, NexusFurnaceMenu.WIDTH, NexusFurnaceMenu.HEIGHT);
        inventoryLabelY = NexusFurnaceMenu.INVENTORY_TOP - 11;
    }

    @Override
    protected void init() {
        super.init();
        collect = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.furnace.collect", menu.experience()), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NexusFurnaceMenu.BTN_COLLECT);
        }).bounds(leftPos + 88, topPos + 3, 80, 12).build());
    }

    private Button collect;

    @Override
    protected void containerTick() {
        super.containerTick();
        if (collect != null) {
            collect.setMessage(Component.translatable("screen.storagenexus.furnace.collect", menu.experience()));
            collect.active = menu.experience() > 0;
        }
    }

    private static String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component status;
        int colour = MUTED;
        int working = menu.working(), wanting = menu.wanting();
        if (wanting == 0) {
            status = Component.translatable("screen.storagenexus.furnace.idle");
        } else if (working == 0) {
            status = Component.translatable("screen.storagenexus.furnace.no_power", NexusFurnaceBlockEntity.ENERGY_PER_SLOT);
            colour = WARN;
        } else if (working < wanting) {
            status = Component.translatable("screen.storagenexus.furnace.low_power", working, wanting);
            colour = WARN;
        } else {
            status = Component.translatable("screen.storagenexus.furnace.working", working, working * NexusFurnaceBlockEntity.ENERGY_PER_SLOT);
        }
        g.text(font, status, BAR_X, BAR_Y + BAR_H + 4, colour, false);
        g.text(font, Component.translatable("screen.storagenexus.furnace.energy", number(menu.energy()), number(NexusFurnaceBlockEntity.CAPACITY)),
                BAR_X, BAR_Y + BAR_H + 15, MUTED, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, HIGHLIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, HIGHLIGHT);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, SHADOW);
        for (Slot s : menu.slots) slot(g, leftPos + s.x, topPos + s.y);

        // A bar under each input slot fills as its item cooks
        for (int i = 0; i < NexusFurnaceBlockEntity.SLOTS; i++) {
            int bx = leftPos + NexusFurnaceMenu.INPUT_X + i * 18, by = topPos + NexusFurnaceMenu.INPUT_Y + 18;
            g.fill(bx, by, bx + 16, by + 4, SLOT_DARK);
            int filled = Math.round(16.0F * menu.progress(i) / NexusFurnaceBlockEntity.COOK_TICKS);
            if (filled > 0) {
                g.fill(bx, by, bx + filled, by + 4, COOK);
                g.fill(bx, by, bx + filled, by + 1, COOK_HOT);
            }
        }

        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, SLOT_FILL);
        int full = Math.round(BAR_W * Math.min(1.0F, (float) menu.energy() / NexusFurnaceBlockEntity.CAPACITY));
        if (full > 0) {
            g.fill(bx, by, bx + full, by + BAR_H, ENERGY);
            g.fill(bx, by, bx + full, by + 1, ENERGY_TOP);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.storagenexus.furnace.energy", number(menu.energy()), number(NexusFurnaceBlockEntity.CAPACITY)),
                    Component.translatable("screen.storagenexus.furnace.tip", NexusFurnaceBlockEntity.ENERGY_PER_SLOT,
                            NexusFurnaceBlockEntity.COOK_TICKS / 20.0).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        g.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        g.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }
}
