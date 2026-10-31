package net.storagenexus.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
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
import net.storagenexus.client.compat.RecipeTransfer;
import net.storagenexus.terminal.TerminalMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** REI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network. Only loaded when REI is installed. */
public class StorageNexusReiPlugin implements REIClientPlugin {
    private static final String CRAFTING_CATEGORY = "minecraft:plugins/crafting";

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(StorageNexusReiPlugin::handle);
    }

    private static TransferHandler.Result handle(TransferHandler.Context context) {
        if (!(context.getMenu() instanceof TerminalMenu menu) || !menu.isCrafting()) return TransferHandler.Result.createNotApplicable();
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
        if (!RecipeTransfer.canCraft(menu, context.getMinecraft().player, slots)) {
            return TransferHandler.Result.createFailed(Component.translatable("screen.storagenexus.missing_ingredients"));
        }
        if (context.isActuallyCrafting()) {
            RecipeTransfer.fill(ResourceKey.create(Registries.RECIPE, id.get()), context.isStackedCrafting());
            context.getMinecraft().gui.setScreen(context.getContainerScreen());
        }
        return TransferHandler.Result.createSuccessful().blocksFurtherHandling();
    }
}
