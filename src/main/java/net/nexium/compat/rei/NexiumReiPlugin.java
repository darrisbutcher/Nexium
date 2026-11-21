package net.nexium.compat.rei;

import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.widgets.TextField;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.nexium.client.compat.RecipeTransfer;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.client.compat.RecipeViewerSearch;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.terminal.TerminalMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * REI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network (or the Backpack's grid, with a Crafting
 * module, from its own slots and then the inventory), and its tooltip lists what is missing; REI's search box and the terminals' stay the same. Only loaded when REI is installed.
 */
public class NexiumReiPlugin implements REIClientPlugin {
    private static final String CRAFTING_CATEGORY = "minecraft:plugins/crafting";

    /** The Backpack's crafting panel sticks out to the right of its screen: REI's item list moves aside for it. */
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(BackpackScreen.class, (ExclusionZonesProvider<BackpackScreen>) screen -> {
            int[] panel = screen.craftingPanelBounds();
            return panel == null ? List.of() : List.of(new Rectangle(panel[0], panel[1], panel[2], panel[3]));
        });
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(NexiumReiPlugin::handle);
        RecipeViewerSearch.connect(() -> {
            TextField field = REIRuntime.getInstance().getSearchTextField();
            return field == null ? null : field.getText();
        }, text -> {
            TextField field = REIRuntime.getInstance().getSearchTextField();
            if (field != null) field.setText(text);
        });
    }

    private static TransferHandler.Result handle(TransferHandler.Context context) {
        net.minecraft.world.inventory.AbstractCraftingMenu menu;
        if (context.getMenu() instanceof TerminalMenu terminal && terminal.isCrafting()) menu = terminal;
        else if (context.getMenu() instanceof BackpackMenu backpack && backpack.craftingActive()) menu = backpack;
        else return TransferHandler.Result.createNotApplicable();
        if (!CRAFTING_CATEGORY.equals(context.getDisplay().getCategoryIdentifier().toString())) return TransferHandler.Result.createNotApplicable();
        Optional<Identifier> id = context.getDisplay().getDisplayLocation();
        if (id.isEmpty()) return TransferHandler.Result.createNotApplicable();

        List<List<Item>> slots = new ArrayList<>();
        for (EntryIngredient ingredient : context.getDisplay().getInputEntries()) {
            List<Item> options = new ArrayList<>();
            for (EntryStack<?> entry : ingredient) {
                if (entry.getValue() instanceof ItemStack stack && !stack.isEmpty()) options.add(stack.getItem());
            }
            slots.add(options);
        }
        List<Integer> missing = RecipeTransfer.missing(menu, context.getMinecraft().player, slots);
        if (!missing.isEmpty()) {
            List<EntryIngredient> missingEntries = new ArrayList<>();
            for (int i : missing) missingEntries.add(context.getDisplay().getInputEntries().get(i));
            return TransferHandler.Result.createFailed(Component.translatable("screen.nexium.missing_ingredients")).tooltipMissing(missingEntries);
        }
        if (context.isActuallyCrafting()) {
            RecipeTransfer.fill(ResourceKey.create(Registries.RECIPE, id.get()), context.isStackedCrafting());
            context.getMinecraft().gui.setScreen(context.getContainerScreen());
        }
        return TransferHandler.Result.createSuccessful().blocksFurtherHandling();
    }
}
