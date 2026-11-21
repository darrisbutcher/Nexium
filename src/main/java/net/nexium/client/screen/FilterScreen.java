package net.nexium.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.nexium.filter.FilterData;
import net.nexium.filter.FilterMenu;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;

/**
 * The filter editor. A Basic Filter has the whitelist/blacklist button and the exact-match one. An Advanced Filter has the
 * whitelist/blacklist button, a Match button (items and tags only, smeltable items, or furnace fuel) and a Tags button that opens the
 * tag picker.
 */
public class FilterScreen extends BaseScreen<FilterMenu> {
    private Button whitelist;
    private Button components;
    private Button special;
    private Button tagsButton;
    private FilterData.@Nullable Special shownSpecial;
    /** Set while the tag picker is open: this screen is only hidden, so the menu behind it must stay open. */
    private boolean picking;
    /** How many tags the picker last left chosen (the item in hand takes a moment to reach the client). */
    private @Nullable Integer pickedCount;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.advanced() ? 184 : 166,
                menu.advanced() ? FilterMenu.ADVANCED_INVENTORY_TOP : FilterMenu.INVENTORY_TOP);
    }

    @Override
    protected void init() {
        super.init();
        whitelist = addRenderableWidget(Button.builder(Component.empty(), b -> click(FilterMenu.BTN_WHITELIST))
                .bounds(leftPos + 8, topPos + 44, menu.advanced() ? 60 : 78, 16).build());
        if (menu.advanced()) {
            special = addRenderableWidget(Button.builder(Component.empty(), b -> click(FilterMenu.BTN_SPECIAL))
                    .bounds(leftPos + 72, topPos + 44, 96, 16).build());
            tagsButton = addRenderableWidget(Button.builder(Component.empty(), b -> openPicker())
                    .bounds(leftPos + 8, topPos + 64, 160, 16).build());
            tagsButton.setTooltip(Tooltip.create(Component.translatable("screen.nexium.tags.tip")));
            shownSpecial = null;
        } else {
            components = addRenderableWidget(Button.builder(Component.empty(), b -> click(FilterMenu.BTN_COMPONENTS))
                    .bounds(leftPos + 90, topPos + 44, 78, 16).build());
        }
        updateLabels();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void updateLabels() {
        whitelist.setMessage(Component.translatable(menu.whitelist() ? "screen.nexium.whitelist" : "screen.nexium.blacklist"));
        if (!menu.advanced()) {
            components.setMessage(Component.translatable(menu.matchComponents() ? "screen.nexium.match_exact" : "screen.nexium.match_item"));
            return;
        }
        FilterData.Special now = menu.special();
        special.setMessage(Component.translatable("screen.nexium.match." + now.key()));
        if (now != shownSpecial) {
            shownSpecial = now;
            special.setTooltip(Tooltip.create(Component.translatable("screen.nexium.match." + now.key() + ".tip")));
        }
        int tags = tagCount();
        tagsButton.setMessage(tags == 0 ? Component.translatable("screen.nexium.tags.none") : Component.translatable("screen.nexium.tags.some", tags));
    }

    /** How many tags are chosen. */
    public int tagCount() {
        return pickedCount != null ? pickedCount : menu.tags().size();
    }

    private void openPicker() {
        if (minecraft == null) return;
        picking = true;
        minecraft.gui.setScreen(new TagPickerScreen(this, new LinkedHashSet<>(menu.tags())));
    }

    /** The tag picker is done: this screen is shown again and behaves as usual. */
    void pickerClosed(int chosen) {
        picking = false;
        pickedCount = chosen;
    }

    FilterMenu filterMenu() {
        return menu;
    }

    /** Whether this is an Advanced Filter's editor (for tests). */
    public boolean menuAdvanced() {
        return menu.advanced();
    }

    /** The centre of the Tags button, in screen coordinates (for tests, which click it with the real mouse). */
    public int[] tagsCentre() {
        return new int[]{leftPos + 8 + 80, topPos + 64 + 8};
    }

    /** The centre of the Match button, in screen coordinates (for tests). */
    public int[] matchCentre() {
        return new int[]{leftPos + 72 + 48, topPos + 44 + 8};
    }

    @Override
    public void removed() {
        // Hiding this screen behind the tag picker must not close the filter's menu
        if (!picking) super.removed();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }
}
