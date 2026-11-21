package net.nexium.compat.jei;

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
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.nexium.Nexium;
import net.nexium.client.compat.RecipeTransfer;
import net.nexium.client.compat.RecipeViewerSearch;
import net.nexium.registry.ModMenus;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.terminal.TerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * JEI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network (or the Backpack's grid, with a Crafting
 * module, from its own slots and then the inventory), and marks in red the slots nothing can fill; JEI's search box and the
 * terminals' stay the same. Only loaded when JEI is installed.
 */
@JeiPlugin
public class NexiumJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Nexium.id("jei");
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
        registration.addRecipeTransferHandler(new Handler<>(registration.getTransferHelper(), TerminalMenu.class, ModMenus.TERMINAL,
                TerminalMenu::isCrafting), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new Handler<>(registration.getTransferHelper(), BackpackMenu.class, net.nexium.essentials.registry.ModMenus.BACKPACK,
                BackpackMenu::craftingActive), RecipeTypes.CRAFTING);
    }

    /** One transfer handler, for a menu with a crafting grid; {@code usable} says whether its grid is there right now. */
    private record Handler<M extends AbstractCraftingMenu>(IRecipeTransferHandlerHelper helper, Class<M> menuClass, MenuType<M> menuType,
                                                           Predicate<M> usable) implements IRecipeTransferHandler<M, RecipeHolder<CraftingRecipe>> {
        @Override
        public Class<? extends M> getContainerClass() {
            return menuClass;
        }

        @Override
        public Optional<MenuType<M>> getMenuType() {
            return Optional.of(menuType);
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        // Still the method every handler must implement; JEI's newer context method calls it by default.
        @SuppressWarnings("removal")
        @Override
        public @Nullable IRecipeTransferError transferRecipe(M menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                                             Player player, boolean maxTransfer, boolean doTransfer) {
            if (!usable.test(menu)) return helper.createInternalError();
            if (!RecipeTransfer.canCraft(menu, player, recipe.value())) {
                Component message = Component.translatable("screen.nexium.missing_ingredients");
                List<IRecipeSlotView> missing = slots == null ? List.of() : missingSlots(menu, player, slots);
                return missing.isEmpty() ? helper.createUserErrorWithTooltip(message) : helper.createUserErrorForMissingSlots(message, missing);
            }
            if (doTransfer) RecipeTransfer.fill(recipe.id(), maxTransfer);
            return null;
        }

        /** The recipe's input slots nothing in the network, the inventory or the grid can fill; JEI draws them red. */
        private static List<IRecipeSlotView> missingSlots(AbstractCraftingMenu menu, Player player, IRecipeSlotsView slots) {
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            List<List<Item>> options = new ArrayList<>();
            for (IRecipeSlotView slot : inputs) options.add(slot.getItemStacks().map(ItemStack::getItem).toList());
            List<IRecipeSlotView> missing = new ArrayList<>();
            for (int i : RecipeTransfer.missing(menu, player, options)) missing.add(inputs.get(i));
            return missing;
        }
    }
}
