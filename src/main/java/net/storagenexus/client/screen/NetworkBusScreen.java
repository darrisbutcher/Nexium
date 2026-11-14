package net.storagenexus.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.network.NetworkBusBlock;
import net.storagenexus.network.NetworkBusMenu;

/** The bus's mode, the side it faces (six buttons, only for sides with an inventory or tank) and its filter slot. */
public class NetworkBusScreen extends BaseScreen<NetworkBusMenu> {
    private static final int FACE_X = 11, FACE_Y = 51, FACE_W = 24, FACE_H = 14, FACE_GAP = 2;
    private static final int SELECTED = 0xFFFFFF55;

    private Button modeButton;
    private final Button[] faceButtons = new Button[6];

    public NetworkBusScreen(NetworkBusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, NetworkBusMenu.INVENTORY_TOP + 82, NetworkBusMenu.INVENTORY_TOP);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 10_000;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NetworkBusMenu.BTN_MODE);
        }).bounds(leftPos + 8, topPos + 18, 90, 16).build());
        for (int i = 0; i < faceButtons.length; i++) {
            Direction side = Direction.from3DDataValue(i);
            faceButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NetworkBusMenu.BTN_FACE + side.get3DDataValue());
                }
            }).bounds(leftPos + FACE_X + i * (FACE_W + FACE_GAP), topPos + FACE_Y, FACE_W, FACE_H).build());
        }
        updateFaceButtons();
    }

    private BlockState state() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getBlockState(menu.pos()) : null;
    }

    private Component modeLabel() {
        BlockState s = state();
        boolean export = s != null && s.getBlock() instanceof NetworkBusBlock && s.getValue(NetworkBusBlock.EXPORT);
        return Component.translatable(export ? "screen.storagenexus.bus_export" : "screen.storagenexus.bus_import");
    }

    /** The side the bus faces now, or null before the block is known. */
    private Direction facing() {
        BlockState s = state();
        return s != null && s.getBlock() instanceof NetworkBusBlock ? s.getValue(NetworkBusBlock.FACING) : null;
    }

    /** Each button shows its side's initial, is only usable where there is something to work with, and says what is there. */
    private void updateFaceButtons() {
        Direction facing = facing();
        for (int i = 0; i < faceButtons.length; i++) {
            Direction side = Direction.from3DDataValue(i);
            boolean workable = (menu.workableFaces() >> i & 1) != 0;
            Component name = NeighbourNames.sideName(side);
            Component initial = Component.literal(name.getString().substring(0, 1));
            faceButtons[i].setMessage(side == facing ? initial.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW) : initial);
            faceButtons[i].active = workable;
            Component what = workable || side == facing ? NeighbourNames.describe(menu.pos(), side) : Component.translatable("screen.storagenexus.bus_nothing_to_work_with");
            faceButtons[i].setTooltip(Tooltip.create(Component.empty().append(name).append(": ").append(what)));
        }
    }

    /** Whether a side's button can be pressed (for the client test). */
    public boolean faceButtonActive(Direction side) {
        return faceButtons[side.get3DDataValue()].active;
    }

    /** The middle of a side's button in GUI pixels (for the client test). */
    public int[] faceButtonCentre(Direction side) {
        Button b = faceButtons[side.get3DDataValue()];
        return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modeButton.setMessage(modeLabel());
        updateFaceButtons();
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        BlockState s = state();
        if (s != null && s.getBlock() instanceof NetworkBusBlock) {
            Direction facing = s.getValue(NetworkBusBlock.FACING);
            Component name = NeighbourNames.describe(menu.pos(), facing);
            g.text(font, Component.translatable("screen.storagenexus.bus_target", name), leftPos + 8, topPos + 38, palette().text(), false);
            // A frame round the button of the side it faces now
            int x = leftPos + FACE_X + facing.get3DDataValue() * (FACE_W + FACE_GAP) - 1, y = topPos + FACE_Y - 1;
            int w = FACE_W + 2, h = FACE_H + 2;
            g.fill(x, y, x + w, y + 1, SELECTED);
            g.fill(x, y + h - 1, x + w, y + h, SELECTED);
            g.fill(x, y, x + 1, y + h, SELECTED);
            g.fill(x + w - 1, y, x + w, y + h, SELECTED);
        }
        g.text(font, Component.translatable("screen.storagenexus.filter"), leftPos + 118, topPos + 24, palette().text(), false);
    }
}
