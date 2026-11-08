package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.disk.DiskDriveBlockEntity;
import net.storagenexus.disk.DiskDriveMenu;
import net.storagenexus.disk.DiskUsage;
import net.storagenexus.disk.StorageDiskItem;
import net.storagenexus.registry.ModComponents;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The Disk Drive's screen: its 8 disks (each with a fill bar: green with room, yellow over 75%, red full), priority
 * (shift-click steps by 10), the filter slot, item and fluid totals over its disks, and Move to disks.
 */
public class DiskDriveScreen extends BaseScreen<DiskDriveMenu> {
    private static final int GREEN = 0xFF5DFF6A, YELLOW = 0xFFFFD23A, RED = 0xFFFF4A3A;
    private final SideHints hints = new SideHints();
    private Button minus, plus, move;

    public DiskDriveScreen(DiskDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, DiskDriveMenu.INVENTORY_Y + 82, DiskDriveMenu.INVENTORY_Y);
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> click(shift() ? DiskDriveMenu.BTN_MINUS_10 : DiskDriveMenu.BTN_MINUS_1))
                .bounds(leftPos + 88, topPos + 28, 18, 14).build());
        plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> click(shift() ? DiskDriveMenu.BTN_PLUS_10 : DiskDriveMenu.BTN_PLUS_1))
                .bounds(leftPos + 150, topPos + 28, 18, 14).build());
        move = addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.disk_drive.move"), b -> click(DiskDriveMenu.BTN_MOVE))
                .bounds(leftPos + 8, topPos + 104, 160, 16).build());
        hints.set(minus, Component.translatable("screen.storagenexus.disk_drive.priority.tooltip"));
        hints.set(plus, Component.translatable("screen.storagenexus.disk_drive.priority.tooltip"));
        hints.set(move, Component.translatable("screen.storagenexus.disk_drive.move.tooltip"));
    }

    private static boolean shift() {
        return Minecraft.getInstance().hasShiftDown();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hints.render(g, leftPos, leftPos + imageWidth, mouseX, mouseY);
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int text = palette().text();
        long items = 0, itemCap = 0, fluid = 0, fluidCap = 0;
        for (int i = 0; i < DiskDriveBlockEntity.SLOTS; i++) {
            int x = leftPos + DiskDriveMenu.slotX(i), y = topPos + DiskDriveMenu.slotY(i) + 17;
            ItemStack disk = menu.getSlot(i).getItem();
            g.fill(x, y, x + 16, y + 3, 0xFF1E1E22);
            if (!(disk.getItem() instanceof StorageDiskItem d)) continue;
            DiskUsage usage = disk.getOrDefault(ModComponents.DISK_USAGE, DiskUsage.EMPTY);
            float f = StorageDiskItem.fill(disk);
            int color = f >= 1 ? RED : f >= 0.75f ? YELLOW : GREEN;
            g.fill(x + 1, y + 1, x + 1 + Math.max(1, Math.round(14 * f)), y + 2, color);
            if (d.isFluid()) {
                fluid += usage.stored();
                fluidCap += d.capacity();
            } else {
                items += usage.stored();
                itemCap += d.capacity();
            }
        }
        g.text(font, Component.translatable("screen.storagenexus.priority"), leftPos + 88, topPos + 18, text, false);
        String p = Integer.toString(menu.priority());
        g.text(font, p, leftPos + 128 - font.width(p) / 2, topPos + 31, 0xFFFFFFFF, true);
        g.text(font, Component.translatable("screen.storagenexus.filter"), leftPos + 108, topPos + 52, text, false);
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ROOT);
        g.text(font, Component.translatable("screen.storagenexus.disk_drive.items", nf.format(items), nf.format(itemCap)),
                leftPos + 8, topPos + 68, text, false);
        bar(g, leftPos + 8, topPos + 78, itemCap == 0 ? 0 : (float) items / itemCap, 0xFF5DC8FF);
        g.text(font, Component.translatable("screen.storagenexus.disk_drive.fluids", nf.format(fluid / FluidConstants.BUCKET),
                nf.format(fluidCap / FluidConstants.BUCKET)), leftPos + 8, topPos + 86, text, false);
        bar(g, leftPos + 8, topPos + 96, fluidCap == 0 ? 0 : (float) fluid / fluidCap, 0xFF2F6FE0);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, float fraction, int color) {
        g.fill(x, y, x + 160, y + 4, 0xFF1E1E22);
        if (fraction > 0) g.fill(x + 1, y + 1, x + 1 + Math.max(1, Math.round(158 * Math.min(1, fraction))), y + 3, color);
    }
}
