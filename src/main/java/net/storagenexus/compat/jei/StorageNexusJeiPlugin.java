package net.storagenexus.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.storagenexus.StorageNexus;
import net.storagenexus.client.compat.RecipeTransfer;
import net.storagenexus.client.compat.RecipeViewerSearch;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.terminal.TerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network, and marks in red the slots
 * nothing can fill; JEI's search box and the terminals' stay the same. Only loaded when JEI is installed.
 */
@JeiPlugin
public class StorageNexusJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return StorageNexus.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RecipeViewerSearch.connect(() -> runtime.getIngredientFilter().getFilterText(), text -> runtime.getIngredientFilter().setFilterText(text));
    }

    @Override
    public void onRuntimeUnavailable() {
        RecipeViewerSearch.disconnect();
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new Handler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    private record Handler(IRecipeTransferHandlerHelper helper) implements IRecipeTransferHandler<TerminalMenu, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends TerminalMenu> getContainerClass() {
            return TerminalMenu.class;
        }

        @Override
        public Optional<MenuType<TerminalMenu>> getMenuType() {
            return Optional.of(ModMenus.TERMINAL);
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        // Still the method every handler must implement; JEI's newer context method calls it by default.
        @SuppressWarnings("removal")
        @Override
        public @Nullable IRecipeTransferError transferRecipe(TerminalMenu menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                                             Player player, boolean maxTransfer, boolean doTransfer) {
            if (!menu.isCrafting()) return helper.createInternalError();
            if (!RecipeTransfer.canCraft(menu, player, recipe.value())) {
                Component message = Component.translatable("screen.storagenexus.missing_ingredients");
                List<IRecipeSlotView> missing = slots == null ? List.of() : missingSlots(menu, player, slots);
                return missing.isEmpty() ? helper.createUserErrorWithTooltip(message) : helper.createUserErrorForMissingSlots(message, missing);
            }
            if (doTransfer) RecipeTransfer.fill(recipe.id(), maxTransfer);
            return null;
        }

        /** The recipe's input slots nothing in the network, the inventory or the grid can fill; JEI draws them red. */
        private static List<IRecipeSlotView> missingSlots(TerminalMenu menu, Player player, IRecipeSlotsView slots) {
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            List<List<Item>> options = new ArrayList<>();
            for (IRecipeSlotView slot : inputs) options.add(slot.getItemStacks().map(ItemStack::getItem).toList());
            List<IRecipeSlotView> missing = new ArrayList<>();
            for (int i : RecipeTransfer.missing(menu, player, options)) missing.add(inputs.get(i));
            return missing;
        }
    }
}
