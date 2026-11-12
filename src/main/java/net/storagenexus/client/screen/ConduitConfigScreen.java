package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.conduit.ConduitConfigMenu;
import net.storagenexus.conduit.SideConfig;

import java.util.Locale;

public class ConduitConfigScreen extends BaseScreen<ConduitConfigMenu> {
    /** Wool colours, white to black, for the 16 channels. */
    public static final int[] CHANNEL_COLORS = {
            0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF474F52,
            0xFF9D9D97, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA, 0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21
    };
    private static final String[] SIDES = {"D", "U", "N", "S", "W", "E"};
    private static final String[] COLOR_KEYS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static final String[] TYPE_KEYS = {"item", "fluid", "energy"};

    private final Button[] sideButtons = new Button[6];
    private final Button[] typeButtons = new Button[TYPE_KEYS.length];
    private Button modeButton, redstoneButton, rrButton;
    private final Button[] insertPicker = new Button[2], extractPicker = new Button[2];
    /** Channel pickers: insert left, extract right, on the row under the mode button. */
    private static final int PICKER_Y = 70, IN_SWATCH_X = 36, OUT_SWATCH_X = 104;

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
            typeButtons[i] = addRenderableWidget(Button.builder(Component.translatable("conduit.storagenexus." + TYPE_KEYS[i]),
                    b -> click(ConduitConfigMenu.BTN_TYPE + id)).bounds(leftPos + 8 + i * 46, topPos + 34, 44, 14).build());
        }
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_MODE))
                .bounds(leftPos + 8, topPos + 52, 128, 14).build());
        insertPicker[0] = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(ConduitConfigMenu.BTN_CHANNEL_PREV))
                .bounds(leftPos + IN_SWATCH_X - 14, topPos + PICKER_Y, 12, 14).build());
        insertPicker[1] = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(ConduitConfigMenu.BTN_CHANNEL_NEXT))
                .bounds(leftPos + IN_SWATCH_X + 16, topPos + PICKER_Y, 12, 14).build());
        extractPicker[0] = addRenderableWidget(Button.builder(Component.literal("<"), b -> click(ConduitConfigMenu.BTN_EXTRACT_CHANNEL_PREV))
                .bounds(leftPos + OUT_SWATCH_X - 14, topPos + PICKER_Y, 12, 14).build());
        extractPicker[1] = addRenderableWidget(Button.builder(Component.literal(">"), b -> click(ConduitConfigMenu.BTN_EXTRACT_CHANNEL_NEXT))
                .bounds(leftPos + OUT_SWATCH_X + 16, topPos + PICKER_Y, 12, 14).build());
        redstoneButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_REDSTONE))
                .bounds(leftPos + 8, topPos + 88, 128, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(ConduitConfigMenu.BTN_PRIORITY_DOWN))
                .bounds(leftPos + 8, topPos + 106, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(ConduitConfigMenu.BTN_PRIORITY_UP))
                .bounds(leftPos + 48, topPos + 106, 12, 12).build());
        rrButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_ROUND_ROBIN))
                .bounds(leftPos + 64, topPos + 106, 72, 12).build());
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
            Component name = Component.translatable("conduit.storagenexus." + TYPE_KEYS[i]);
            typeButtons[i].setMessage(i == type ? Component.literal("> ").append(name) : name);
        }
        SideConfig.Mode mode = SideConfig.Mode.values()[menu.get(ConduitConfigMenu.D_MODE) % 4];
        modeButton.setMessage(Component.translatable("mode.storagenexus." + mode.name().toLowerCase(Locale.ROOT)));
        for (Button b : insertPicker) b.active = mode.inserts();
        for (Button b : extractPicker) b.active = mode.extracts();
        SideConfig.RedstoneControl rc = SideConfig.RedstoneControl.values()[menu.get(ConduitConfigMenu.D_REDSTONE) % 4];
        redstoneButton.setMessage(Component.translatable("redstone.storagenexus." + rc.name().toLowerCase(Locale.ROOT)));
        rrButton.setMessage(Component.translatable(menu.get(ConduitConfigMenu.D_RR) == 1
                ? "screen.storagenexus.round_robin_on" : "screen.storagenexus.round_robin_off"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        SideConfig.Mode mode = mode();
        g.text(font, Component.translatable("screen.storagenexus.channel_in"), leftPos + 8, topPos + PICKER_Y + 3, palette().text(), false);
        g.text(font, Component.translatable("screen.storagenexus.channel_out"), leftPos + 70, topPos + PICKER_Y + 3, palette().text(), false);
        swatch(g, IN_SWATCH_X, menu.get(ConduitConfigMenu.D_CHANNEL) % 16, mode.inserts());
        swatch(g, OUT_SWATCH_X, menu.get(ConduitConfigMenu.D_EXTRACT_CHANNEL) % 16, mode.extracts());
        String prio = Integer.toString(menu.get(ConduitConfigMenu.D_PRIORITY) - 100);
        g.text(font, prio, leftPos + 34 - font.width(prio) / 2, topPos + 108, palette().text(), false);
        g.text(font, "F", leftPos + 143, topPos + 66, palette().text(), false);
        g.text(font, "S", leftPos + 143, topPos + 88, palette().text(), false);
        // What is on the selected side: green when this conduit connects to it, grey when not
        String neighbour = font.plainSubstrByWidth(NeighbourNames.describe(menu.pos(), selectedSide()).getString(), neighbourWidth());
        int colour = menu.get(ConduitConfigMenu.D_CONNECTED) == 1 ? 0xFF55FF55 : 0xFFA0A0A0;
        g.text(font, neighbour, leftPos + imageWidth - 8 - font.width(neighbour), topPos + 6, colour, false);
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
                .append(Component.translatable(connected ? "screen.storagenexus.side_connected" : "screen.storagenexus.side_not_connected"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int nameRight = leftPos + imageWidth - 8;
        if (mouseY >= topPos + 4 && mouseY < topPos + 15 && mouseX >= nameRight - neighbourWidth() && mouseX < nameRight) {
            g.setTooltipForNextFrame(font, neighbourTooltip(), mouseX, mouseY);
        }
        for (int i = 0; i < 2; i++) {
            int x = leftPos + (i == 0 ? IN_SWATCH_X : OUT_SWATCH_X);
            if (mouseX >= x && mouseX < x + 14 && mouseY >= topPos + PICKER_Y && mouseY < topPos + PICKER_Y + 14) {
                int channel = menu.get(i == 0 ? ConduitConfigMenu.D_CHANNEL : ConduitConfigMenu.D_EXTRACT_CHANNEL) % 16;
                g.setComponentTooltipForNextFrame(font, java.util.List.of(
                        Component.translatable(i == 0 ? "screen.storagenexus.channel_in.tooltip" : "screen.storagenexus.channel_out.tooltip",
                                Component.translatable("color.minecraft." + COLOR_KEYS[channel])),
                        Component.translatable(i == 0 ? "screen.storagenexus.channel_in.hint" : "screen.storagenexus.channel_out.hint")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
            }
        }
    }
}
