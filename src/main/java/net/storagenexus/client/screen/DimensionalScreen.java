package net.storagenexus.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.dimension.RenameLinkPayload;

/** Shows and renames the link; shows whether the other end is loaded. */
public class DimensionalScreen extends BaseScreen<DimensionalMenu> {
    private EditBox nameBox;

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
    }

    @Override
    protected void renderExtraBg(GuiGraphics g, int mouseX, int mouseY) {
        if (menu.isInput()) {
            g.drawString(font, Component.translatable("screen.storagenexus.buffer"), leftPos + 8, topPos + 40, TEXT, false);
            g.drawString(font, Component.translatable("screen.storagenexus.outputs"), leftPos + 8, topPos + 58, TEXT, false);
            g.drawString(font, menu.statusA() + "/" + menu.statusB(), leftPos + 8, topPos + 68, TEXT, false);
            g.drawString(font, Component.translatable("screen.storagenexus.speed"), leftPos + 136, topPos + 62, TEXT, false);
        } else {
            Component status = menu.statusB() == 0 ? Component.translatable("screen.storagenexus.not_linked")
                    : Component.translatable(menu.statusA() == 1 ? "screen.storagenexus.input_online" : "screen.storagenexus.input_offline");
            g.drawString(font, Component.translatable("screen.storagenexus.filter"), leftPos + 118, topPos + 58, TEXT, false);
            g.drawString(font, Component.translatable("screen.storagenexus.output_hint"), leftPos + 8, topPos + 40, TEXT, false);
            g.drawString(font, status, leftPos + 8, topPos + 80, TEXT, false);
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
