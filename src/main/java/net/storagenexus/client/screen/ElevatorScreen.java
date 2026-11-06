package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.storagenexus.elevator.ElevatorBlockEntity;
import net.storagenexus.elevator.ElevatorGoPayload;
import net.storagenexus.elevator.ElevatorOpenPayload;
import net.storagenexus.elevator.ElevatorRenamePayload;
import net.storagenexus.elevator.Floor;

import java.util.List;

/**
 * The elevator screen: a name box for this floor on top, then every floor in the column, highest first. Click one to go
 * there. The floor you are on is marked, floors with no room to stand are greyed out, and the mouse wheel scrolls a long list.
 */
public class ElevatorScreen extends Screen {
    private static final int WIDTH = 190, ROW = 20, ROWS = 6, TOP = 50;
    private final ElevatorOpenPayload data;
    private int left, top, panelHeight, scroll;
    private EditBox nameBox;
    private final Button[] rows = new Button[ROWS];
    private final List<Floor> floors;
    private final Floor[] shown = new Floor[ROWS];

    public ElevatorScreen(ElevatorOpenPayload data) {
        super(Component.translatable("block.storagenexus.elevator"));
        this.data = data;
        // Highest floor first, like looking at the buttons in a lift
        this.floors = data.floors().reversed();
    }

    @Override
    protected void init() {
        panelHeight = TOP + ROWS * ROW + 8;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        nameBox = new EditBox(font, left + 8, top + 22, WIDTH - 16 - 48, 14, Component.translatable("screen.storagenexus.elevator.name"));
        nameBox.setMaxLength(ElevatorBlockEntity.MAX_NAME);
        nameBox.setValue(data.ownName());
        nameBox.setHint(Component.translatable("screen.storagenexus.elevator.name_hint"));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.elevator.rename"), b -> rename())
                .bounds(left + WIDTH - 8 - 44, top + 21, 44, 16).build());
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            rows[i] = addRenderableWidget(Button.builder(Component.empty(), b -> go(shown[row]))
                    .bounds(left + 8, top + TOP + i * ROW, WIDTH - 16, ROW - 2).build());
        }
        refresh();
    }

    private void rename() {
        String name = nameBox.getValue().trim();
        ClientPlayNetworking.send(new ElevatorRenamePayload(data.pos(), name));
        // Show it in the list straight away; an empty name goes back to the numbered one
        for (int i = 0; i < floors.size(); i++) {
            Floor f = floors.get(i);
            if (f.y() != data.pos().getY()) continue;
            String shownName = name.isEmpty()
                    ? Component.translatable("screen.storagenexus.elevator.floor", floors.size() - i).getString() : name;
            floors.set(i, new Floor(f.y(), shownName, f.blocked()));
        }
        refresh();
    }

    /** Presses the button in this row (for the client test). */
    public void pressRow(int row) {
        go(shown[row]);
    }

    private void go(Floor floor) {
        if (floor == null || floor.blocked() || floor.y() == data.pos().getY()) return;
        ClientPlayNetworking.send(new ElevatorGoPayload(data.pos(), floor.y()));
        onClose();
    }

    private void refresh() {
        scroll = Math.clamp(scroll, 0, Math.max(0, floors.size() - ROWS));
        for (int i = 0; i < ROWS; i++) {
            Button b = rows[i];
            Floor f = scroll + i < floors.size() ? floors.get(scroll + i) : null;
            shown[i] = f;
            b.visible = f != null;
            if (f == null) continue;
            boolean here = f.y() == data.pos().getY();
            Component label = Component.literal(f.name()).append(Component.literal("  y " + f.y()).withStyle(ChatFormatting.GRAY));
            if (here) label = Component.translatable("screen.storagenexus.elevator.here", label).withStyle(ChatFormatting.GREEN);
            b.setMessage(label);
            b.active = !here && !f.blocked();
            b.setTooltip(f.blocked() ? Tooltip.create(Component.translatable("screen.storagenexus.elevator.blocked")) : null);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (floors.size() > ROWS) {
            scroll -= (int) Math.signum(scrollY);
            refresh();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        Palette.DARK.panel(g, left, top, WIDTH, panelHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, left + 8, top + 8, Palette.DARK.text(), false);
        Component count = Component.translatable("screen.storagenexus.elevator.count", floors.size());
        g.text(font, count, left + WIDTH - 8 - font.width(count), top + 8, 0xFFA0A0A0, false);
        if (floors.size() > ROWS) {
            Component range = Component.translatable("screen.storagenexus.elevator.scroll");
            g.text(font, range, left + 8, top + TOP - 10, 0xFF808080, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
