package net.nexium.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nexium.conduit.ConduitConfigMenu;
import net.nexium.conduit.SideConfig;

import java.util.Locale;

public class ConduitConfigScreen extends BaseScreen<ConduitConfigMenu> {
    /** Wool colours, white to black, for the 16 channels. */
    public static final int[] CHANNEL_COLORS = {
            0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF474F52,
            0xFF9D9D97, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA, 0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21
    };
    private static final String[] SIDES = {"D", "U", "N", "S", "W", "E"};
    public static final String[] COLOR_KEYS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static final String[] TYPE_KEYS = {"item", "fluid", "energy"};

    private final Button[] sideButtons = new Button[6];
    private final Button[] typeButtons = new Button[TYPE_KEYS.length];
    private Button modeButton, redstoneButton, rrButton;
    private final Button[] insertPicker = new Button[2];
    /**
     * Channel pickers: the insert colour (arrows and a swatch) on the row under the mode button, and under it the extract colours: a
     * grid of the 16 colours, each switched on or off by a click (a side can extract on several at once).
     */
    private static final int PICKER_Y = 70, IN_SWATCH_X = 36;
    private static final int OUT_Y = 88, GRID_X = 36, CELL_W = 10, CELL_H = 8, PITCH_X = 11, PITCH_Y = 9;

    public ConduitConfigScreen(ConduitConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, ConduitConfigMenu.INVENTORY_TOP + 82, ConduitConfigMenu.INVENTORY_TOP);
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            final int id = i;
            sideButtons[i] = addRenderableWidget(Button.builder(Component.literal(SIDES[i]), b -> click(ConduitConfigMenu.BTN_SIDE + id))
                    .bounds(leftPos + 8 + i * 24, topPos + 16, 22, 14).build());
        }
        for (int i = 0; i < TYPE_KEYS.length; i++) {
            final int id = i;
            typeButtons[i] = addRenderableWidget(Button.builder(Component.translatable("conduit.nexium." + TYPE_KEYS[i]),
                    b -> click(ConduitConfigMenu.BTN_TYPE + id)).bounds(leftPos + 8 + i * 46, topPos + 34, 44, 14).build());
        }
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_MODE))
                .bounds(leftPos + 8, topPos + 52, 128, 14).build());
        insertPicker[0] = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(ConduitConfigMenu.BTN_CHANNEL_PREV))
                .bounds(leftPos + IN_SWATCH_X - 14, topPos + PICKER_Y, 12, 14).build());
        insertPicker[1] = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(ConduitConfigMenu.BTN_CHANNEL_NEXT))
                .bounds(leftPos + IN_SWATCH_X + 16, topPos + PICKER_Y, 12, 14).build());
        redstoneButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_REDSTONE))
                .bounds(leftPos + 8, topPos + 108, 128, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(ConduitConfigMenu.BTN_PRIORITY_DOWN))
                .bounds(leftPos + 8, topPos + 126, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(ConduitConfigMenu.BTN_PRIORITY_UP))
                .bounds(leftPos + 48, topPos + 126, 12, 12).build());
        rrButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_ROUND_ROBIN))
                .bounds(leftPos + 64, topPos + 126, 72, 12).build());
        inventoryLabelY = 10_000;
        updateLabels();
    }

    private void updateLabels() {
        int side = menu.get(ConduitConfigMenu.D_SIDE);
        int type = menu.get(ConduitConfigMenu.D_TYPE);
        int types = menu.get(ConduitConfigMenu.D_TYPES);
        for (int i = 0; i < 6; i++) sideButtons[i].setMessage(Component.literal(i == side ? "[" + SIDES[i] + "]" : SIDES[i]));
        for (int i = 0; i < TYPE_KEYS.length; i++) {
            typeButtons[i].active = (types & (1 << i)) != 0;
            Component name = Component.translatable("conduit.nexium." + TYPE_KEYS[i]);
            typeButtons[i].setMessage(i == type ? Component.literal("> ").append(name) : name);
        }
        SideConfig.Mode mode = SideConfig.Mode.values()[menu.get(ConduitConfigMenu.D_MODE) % 4];
        modeButton.setMessage(Component.translatable("mode.nexium." + mode.name().toLowerCase(Locale.ROOT)));
        for (Button b : insertPicker) b.active = mode.inserts();
        SideConfig.RedstoneControl rc = SideConfig.RedstoneControl.values()[menu.get(ConduitConfigMenu.D_REDSTONE) % 4];
        redstoneButton.setMessage(Component.translatable("redstone.nexium." + rc.name().toLowerCase(Locale.ROOT)));
        rrButton.setMessage(Component.translatable(menu.get(ConduitConfigMenu.D_RR) == 1
                ? "screen.nexium.round_robin_on" : "screen.nexium.round_robin_off"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SideConfig.Mode mode = mode();
        g.text(font, Component.translatable("screen.nexium.channel_in"), leftPos + 8, topPos + PICKER_Y + 3, palette().text(), false);
        g.text(font, Component.translatable("screen.nexium.channel_out"), leftPos + 8, topPos + OUT_Y + 5, palette().text(), false);
        swatch(g, IN_SWATCH_X, menu.get(ConduitConfigMenu.D_CHANNEL) % 16, mode.inserts());
        extractGrid(g, mode.extracts());
        String prio = Integer.toString(menu.get(ConduitConfigMenu.D_PRIORITY) - 100);
        g.text(font, prio, leftPos + 34 - font.width(prio) / 2, topPos + 128, palette().text(), false);
        g.text(font, "F", leftPos + 143, topPos + 66, palette().text(), false);
        g.text(font, "S", leftPos + 143, topPos + 88, palette().text(), false);
        // What is on the selected side: green when this conduit connects to it, grey when not
        String neighbour = font.plainSubstrByWidth(NeighbourNames.describe(menu.pos(), selectedSide()).getString(), neighbourWidth());
        int colour = menu.get(ConduitConfigMenu.D_CONNECTED) == 1 ? 0xFF55FF55 : 0xFFA0A0A0;
        g.text(font, neighbour, leftPos + imageWidth - 8 - font.width(neighbour), topPos + 6, colour, false);
    }

    /** The set of extract colours, as the menu sends it. */
    private int extractMask() {
        return menu.get(ConduitConfigMenu.D_EXTRACT_CHANNELS) & 0xFFFF;
    }

    /** The grid cell under the point, or -1. Colour 0 is top left, 7 top right, 8 bottom left. */
    private int extractCellAt(double x, double y) {
        double gx = x - (leftPos + GRID_X), gy = y - (topPos + OUT_Y);
        if (gx < 0 || gy < 0) return -1;
        int col = (int) (gx / PITCH_X), row = (int) (gy / PITCH_Y);
        if (col > 7 || row > 1 || gx - col * PITCH_X >= CELL_W || gy - row * PITCH_Y >= CELL_H) return -1;
        return row * 8 + col;
    }

    /** Sixteen colour squares: a bright frame round the ones this side extracts on, the rest dimmed (all dimmed when the mode never extracts). */
    private void extractGrid(GuiGraphicsExtractor g, boolean used) {
        int mask = extractMask();
        for (int i = 0; i < 16; i++) {
            int x = leftPos + GRID_X + (i % 8) * PITCH_X, y = topPos + OUT_Y + (i / 8) * PITCH_Y;
            boolean on = (mask >> i & 1) != 0;
            g.fill(x, y, x + CELL_W, y + CELL_H, on && used ? 0xFFFFFFFF : 0xFF000000);
            g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, CHANNEL_COLORS[i]);
            if (!on || !used) g.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, on ? 0xB0333333 : 0x90202020);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // The grid first: a container screen answers every click itself, so what comes after super would never run
        int cell = extractCellAt(event.x(), event.y());
        if (cell >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT && mode().extracts()) {
            click(ConduitConfigMenu.BTN_EXTRACT_TOGGLE + cell);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Which colours the grid has on, for tests. */
    public int extractColours() {
        return extractMask();
    }

    /** Where the centre of colour {@code channel}'s square is, for tests. */
    public int[] extractCellCentre(int channel) {
        return new int[]{leftPos + GRID_X + (channel % 8) * PITCH_X + CELL_W / 2, topPos + OUT_Y + (channel / 8) * PITCH_Y + CELL_H / 2};
    }

    private SideConfig.Mode mode() {
        return SideConfig.Mode.values()[menu.get(ConduitConfigMenu.D_MODE) % 4];
    }

    /** A channel colour square; dimmed when the side's mode doesn't use that direction. */
    private void swatch(GuiGraphicsExtractor g, int x, int channel, boolean used) {
        int left = leftPos + x, top = topPos + PICKER_Y;
        g.fill(left, top, left + 14, top + 14, 0xFF000000);
        g.fill(left + 1, top + 1, left + 13, top + 13, CHANNEL_COLORS[channel]);
        if (!used) g.fill(left + 1, top + 1, left + 13, top + 13, 0xB0333333);
    }

    private Direction selectedSide() {
        return Direction.from3DDataValue(menu.get(ConduitConfigMenu.D_SIDE) % 6);
    }

    /** Room for the neighbour's name right of the title. */
    private int neighbourWidth() {
        return imageWidth - 16 - font.width(title) - 6;
    }

    /** Full "North: Chest (connected)" when the shortened name is hovered. */
    private Component neighbourTooltip() {
        boolean connected = menu.get(ConduitConfigMenu.D_CONNECTED) == 1;
        return Component.empty().append(NeighbourNames.sideName(selectedSide())).append(": ")
                .append(NeighbourNames.describe(menu.pos(), selectedSide()))
                .append(Component.translatable(connected ? "screen.nexium.side_connected" : "screen.nexium.side_not_connected"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int nameRight = leftPos + imageWidth - 8;
        if (mouseY >= topPos + 4 && mouseY < topPos + 15 && mouseX >= nameRight - neighbourWidth() && mouseX < nameRight) {
            g.setComponentTooltipForNextFrame(font, net.nexium.client.Tooltips.wrapped(font, java.util.List.of(neighbourTooltip())), mouseX, mouseY);
        }
        int inX = leftPos + IN_SWATCH_X;
        if (mouseX >= inX && mouseX < inX + 14 && mouseY >= topPos + PICKER_Y && mouseY < topPos + PICKER_Y + 14) {
            int channel = menu.get(ConduitConfigMenu.D_CHANNEL) % 16;
            g.setComponentTooltipForNextFrame(font, net.nexium.client.Tooltips.wrapped(font, java.util.List.of(
                    Component.translatable("screen.nexium.channel_in.tooltip", Component.translatable("color.minecraft." + COLOR_KEYS[channel])),
                    Component.translatable("screen.nexium.channel_in.hint").withStyle(net.minecraft.ChatFormatting.GRAY))), mouseX, mouseY);
        }
        int cell = extractCellAt(mouseX, mouseY);
        if (cell >= 0) {
            boolean on = (extractMask() >> cell & 1) != 0;
            g.setComponentTooltipForNextFrame(font, net.nexium.client.Tooltips.wrapped(font, java.util.List.of(
                    Component.translatable("color.minecraft." + COLOR_KEYS[cell]).withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.translatable(on ? "screen.nexium.channel_out.on" : "screen.nexium.channel_out.off").withStyle(on ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY),
                    Component.translatable("screen.nexium.channel_out.hint").withStyle(net.minecraft.ChatFormatting.GRAY))), mouseX, mouseY);
        }
    }
}
