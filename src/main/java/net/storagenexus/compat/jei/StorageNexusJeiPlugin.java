package net.storagenexus.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.storagenexus.StorageNexus;
import net.storagenexus.client.compat.RecipeTransfer;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.terminal.TerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** JEI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network. Only loaded when JEI is installed. */
@JeiPlugin
public class StorageNexusJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return StorageNexus.id("jei");
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
                return helper.createUserErrorWithTooltip(Component.translatable("screen.storagenexus.missing_ingredients"));
            }
            if (doTransfer) RecipeTransfer.fill(recipe.id(), maxTransfer);
            return null;
        }
    }
}
