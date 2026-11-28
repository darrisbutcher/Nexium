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
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.nexium.client.screen.BackpackScreen;
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
 * JEI's item list moves aside for the Backpack's crafting panel, and JEI's "+" button on crafting recipes fills the Crafting Terminal's grid from the network (or the Backpack's grid, with a Crafting
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

    /** The Backpack's crafting panel sticks out to the right of its screen: JEI's item list moves aside for it. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(BackpackScreen.class, new IGuiContainerHandler<BackpackScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(BackpackScreen screen) {
                int[] panel = screen.craftingPanelBounds();
                return panel == null ? List.of() : List.of(new Rect2i(panel[0], panel[1], panel[2], panel[3]));
            }
        });
    }

    /**
     * JEI only lists a crafting recipe it has an extension for, and it matches the recipe's own class: the charge-keeping Battery and Cell
     * recipes are a class of ours (a ShapedRecipe underneath), so without this they were missing from JEI.
     */
    @Override
    public void registerVanillaCategoryExtensions(mezz.jei.api.registration.IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(net.nexium.recipe.ChargeKeepingShapedRecipe.class,
                new mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension<net.nexium.recipe.ChargeKeepingShapedRecipe>() {
                    private net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay display(RecipeHolder<net.nexium.recipe.ChargeKeepingShapedRecipe> holder) {
                        return (net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay) holder.value().display().getFirst();
                    }

                    @Override
                    public List<net.minecraft.world.item.crafting.display.SlotDisplay> getIngredients(RecipeHolder<net.nexium.recipe.ChargeKeepingShapedRecipe> holder) {
                        return display(holder).ingredients();
                    }

                    @Override
                    public int getWidth(RecipeHolder<net.nexium.recipe.ChargeKeepingShapedRecipe> holder) {
                        return display(holder).width();
                    }

                    @Override
                    public int getHeight(RecipeHolder<net.nexium.recipe.ChargeKeepingShapedRecipe> holder) {
                        return display(holder).height();
                    }

                    @Override
                    public void setRecipe(RecipeHolder<net.nexium.recipe.ChargeKeepingShapedRecipe> holder, mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,
                                          mezz.jei.api.gui.ingredient.ICraftingGridHelper grid, mezz.jei.api.recipe.IFocusGroup focuses) {
                        var d = display(holder);
                        grid.createAndSetIngredientsFromDisplays(builder, d.ingredients(), d.width(), d.height());
                        grid.createAndSetOutputs(builder, d.result());
                    }
                });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new Handler<>(registration.getTransferHelper(), TerminalMenu.class, ModMenus.TERMINAL,
                TerminalMenu::isCrafting), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new Handler<>(registration.getTransferHelper(), BackpackMenu.class, net.nexium.essentials.registry.ModMenus.BACKPACK,
                BackpackMenu::craftingActive), RecipeTypes.CRAFTING);
        encoderHandler(registration, RecipeTypes.CRAFTING);
        encoderHandler(registration, RecipeTypes.SMELTING);
        encoderHandler(registration, RecipeTypes.BLASTING);
        encoderHandler(registration, RecipeTypes.SMOKING);
        encoderHandler(registration, RecipeTypes.CAMPFIRE_COOKING);
        encoderHandler(registration, RecipeTypes.STONECUTTING);
    }

    /** JEI's "+" for the Pattern Encoder: crafting recipes fill its grid, and in processing mode so do smelting and stonecutting recipes. */
    private <T extends net.minecraft.world.item.crafting.Recipe<?>> void encoderHandler(IRecipeTransferRegistration registration, IRecipeType<RecipeHolder<T>> type) {
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<net.nexium.autocraft.PatternEncoderMenu, RecipeHolder<T>>() {
            private final IRecipeTransferHandlerHelper helper = registration.getTransferHelper();

            @Override
            public Class<? extends net.nexium.autocraft.PatternEncoderMenu> getContainerClass() {
                return net.nexium.autocraft.PatternEncoderMenu.class;
            }

            @Override
            public Optional<MenuType<net.nexium.autocraft.PatternEncoderMenu>> getMenuType() {
                return Optional.of(ModMenus.PATTERN_ENCODER);
            }

            @Override
            public IRecipeType<RecipeHolder<T>> getRecipeType() {
                return type;
            }

            @SuppressWarnings("removal")
            @Override
            public @Nullable IRecipeTransferError transferRecipe(net.nexium.autocraft.PatternEncoderMenu menu, RecipeHolder<T> recipe, IRecipeSlotsView slots,
                                                                 Player player, boolean maxTransfer, boolean doTransfer) {
                var displays = recipe.value().display();
                if (displays.isEmpty() || !net.nexium.autocraft.PatternEncoderMenu.canFillFrom(displays.getFirst(), menu.processing())) {
                    return helper.createUserErrorWithTooltip(Component.translatable("screen.nexium.encoder.jei_processing"));
                }
                if (doTransfer) net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.nexium.autocraft.EncoderFillPayload(recipe.id()));
                return null;
            }
        }, type);
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
