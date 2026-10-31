package net.storagenexus.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

    private static final String[] TYPE_KEYS = {"item", "fluid"};

    private final Button[] sideButtons = new Button[6];
    private final Button[] typeButtons = new Button[TYPE_KEYS.length];
    private Button modeButton, redstoneButton, rrButton;

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
                .bounds(leftPos + 8, topPos + 52, 82, 14).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(ConduitConfigMenu.BTN_CHANNEL_PREV))
                .bounds(leftPos + 94, topPos + 52, 12, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(ConduitConfigMenu.BTN_CHANNEL_NEXT))
                .bounds(leftPos + 124, topPos + 52, 12, 14).build());
        redstoneButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_REDSTONE))
                .bounds(leftPos + 8, topPos + 70, 128, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(ConduitConfigMenu.BTN_PRIORITY_DOWN))
                .bounds(leftPos + 8, topPos + 88, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(ConduitConfigMenu.BTN_PRIORITY_UP))
                .bounds(leftPos + 48, topPos + 88, 12, 12).build());
        rrButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(ConduitConfigMenu.BTN_ROUND_ROBIN))
                .bounds(leftPos + 64, topPos + 88, 72, 12).build());
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
        int channel = menu.get(ConduitConfigMenu.D_CHANNEL) % 16;
        g.fill(leftPos + 108, topPos + 52, leftPos + 122, topPos + 66, 0xFF000000);
        g.fill(leftPos + 109, topPos + 53, leftPos + 121, topPos + 65, CHANNEL_COLORS[channel]);
        String prio = Integer.toString(menu.get(ConduitConfigMenu.D_PRIORITY) - 100);
        g.text(font, prio, leftPos + 34 - font.width(prio) / 2, topPos + 90, palette().text(), false);
        g.text(font, "F", leftPos + 143, topPos + 66, palette().text(), false);
        g.text(font, "S", leftPos + 143, topPos + 88, palette().text(), false);
        Component conn = Component.translatable(menu.get(ConduitConfigMenu.D_CONNECTED) == 1
                ? "screen.storagenexus.connected" : "screen.storagenexus.not_connected");
        g.text(font, conn, leftPos + imageWidth - 8 - font.width(conn), topPos + 6, palette().text(), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 108 && mouseX < leftPos + 122 && mouseY >= topPos + 52 && mouseY < topPos + 66) {
            int channel = menu.get(ConduitConfigMenu.D_CHANNEL) % 16;
            g.setComponentTooltipForNextFrame(font, java.util.List.of(Component.translatable("color.minecraft." + COLOR_KEYS[channel])), mouseX, mouseY);
        }
    }
}
