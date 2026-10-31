package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.dimension.RenameLinkPayload;

/** Shows and renames the link; shows whether the other end is loaded. */
public class DimensionalScreen extends BaseScreen<DimensionalMenu> {
    private static final String[] SIDE_LETTERS = {"D", "U", "N", "S", "W", "E"};

    private EditBox nameBox;
    /** Output only: one toggle per side, indexed by Direction 3D data value. */
    private final Button[] sideButtons = new Button[6];

    public DimensionalScreen(DimensionalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, DimensionalMenu.INVENTORY_TOP + 82, DimensionalMenu.INVENTORY_TOP);
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(font, leftPos + 8, topPos + 18, 112, 12, Component.translatable("screen.storagenexus.link_name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(menu.name());
        nameBox.setHint(Component.translatable("screen.storagenexus.not_linked"));
        nameBox.setEditable(!menu.name().isEmpty());
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.storagenexus.rename"), b -> {
            String name = nameBox.getValue().trim();
            if (!name.isEmpty() && !name.equals(menu.name())) {
                ClientPlayNetworking.send(new RenameLinkPayload(menu.pos(), name));
                menu.setClientName(name);
            }
        }).bounds(leftPos + 124, topPos + 17, 46, 14).build());
        if (!menu.isInput()) {
            for (int i = 0; i < 6; i++) {
                final int id = i;
                sideButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                }).bounds(leftPos + 8 + i * 18, topPos + 36, 16, 14).build());
            }
            updateSideButtons();
        }
    }

    private boolean sideOn(Direction d) {
        return (menu.sideMask() & (1 << d.get3DDataValue())) != 0;
    }

    /** "North: Chest (on)" for one side of the Output. */
    private Component sideInfo(Direction d) {
        return Component.empty().append(NeighbourNames.sideName(d)).append(": ")
                .append(NeighbourNames.describe(menu.pos(), d))
                .append(Component.translatable(sideOn(d) ? "screen.storagenexus.side_on" : "screen.storagenexus.side_off"));
    }

    private void updateSideButtons() {
        for (Direction d : Direction.values()) {
            Button b = sideButtons[d.get3DDataValue()];
            if (b == null) continue;
            b.setMessage(Component.literal(SIDE_LETTERS[d.get3DDataValue()])
                    .withStyle(sideOn(d) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            b.setTooltip(Tooltip.create(sideInfo(d)));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.isInput()) updateSideButtons();
    }

    @Override
    protected void renderExtraBg(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (menu.isInput()) {
            g.text(font, Component.translatable("screen.storagenexus.buffer"), leftPos + 8, topPos + 40, palette().text(), false);
            g.text(font, Component.translatable("screen.storagenexus.outputs"), leftPos + 8, topPos + 58, palette().text(), false);
            g.text(font, menu.statusA() + "/" + menu.statusB(), leftPos + 8, topPos + 68, palette().text(), false);
            g.text(font, Component.translatable("screen.storagenexus.speed"), leftPos + 136, topPos + 62, palette().text(), false);
        } else {
            Component status = menu.statusB() == 0 ? Component.translatable("screen.storagenexus.not_linked")
                    : Component.translatable(menu.statusA() == 1 ? "screen.storagenexus.input_online" : "screen.storagenexus.input_offline");
            g.text(font, Component.translatable("screen.storagenexus.filter"), leftPos + 118, topPos + 58, palette().text(), false);
            // The hovered side's neighbour, else a reminder of what the buttons do
            Component info = Component.translatable("screen.storagenexus.sides_hint");
            for (Direction d : Direction.values()) {
                Button b = sideButtons[d.get3DDataValue()];
                if (b != null && b.isHovered()) info = sideInfo(d);
            }
            g.text(font, font.plainSubstrByWidth(info.getString(), 106), leftPos + 8, topPos + 56, palette().text(), false);
            g.text(font, status, leftPos + 8, topPos + 80, palette().text(), false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox.isFocused()) {
            if (event.key() == 256) {
                onClose();
                return true;
            }
            return nameBox.keyPressed(event) || nameBox.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }
}
