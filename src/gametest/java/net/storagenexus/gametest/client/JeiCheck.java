package net.storagenexus.gametest.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.storagenexus.terminal.TerminalMenu;

import java.util.Optional;

/** Test-only JEI plugin: keeps JEI's runtime so the client test can press JEI's "+" on the Crafting Terminal. */
@JeiPlugin
public class JeiCheck implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("storagenexus-gametest", "jei_check");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /** JEI's search text, or null without a runtime. */
    public static String filterText() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static void setFilterText(String text) {
        if (runtime != null) runtime.getIngredientFilter().setFilterText(text);
    }

    /**
     * Checks JEI's "+" for a recipe the network can't make (a diamond block, with no diamonds anywhere) without doing it:
     * returns how many slots our handler marked as missing, or -1 if it didn't say.
     */
    @SuppressWarnings({"removal", "unchecked"})
    public static int missingForDiamondBlock(Minecraft mc) {
        if (runtime == null) return -2;
        TerminalMenu menu = (TerminalMenu) mc.player.containerMenu;
        IRecipeCategory<RecipeHolder<CraftingRecipe>> category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
        var handler = runtime.getRecipeTransferManager().<TerminalMenu, RecipeHolder<CraftingRecipe>>getRecipeTransferHandler(menu, category).orElseThrow();
        RecipeHolder<CraftingRecipe> recipe = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                .filter(r -> r.id().identifier().getPath().equals("diamond_block")).findFirst().orElseThrow();
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
                runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        var error = handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), mc.player, false, false);
        return error == null ? 0 : error.getMissingCountHint();
    }

    /** Finds the handler JEI would use for the open terminal and crafting recipes, and runs it for a crafting table. */
    @SuppressWarnings({"removal", "unchecked"})
    public static String pressPlus(Minecraft mc) {
        if (runtime == null) return "no JEI runtime";
        TerminalMenu menu = (TerminalMenu) mc.player.containerMenu;
        IRecipeCategory<RecipeHolder<CraftingRecipe>> category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
        Optional<IRecipeTransferHandler<TerminalMenu, RecipeHolder<CraftingRecipe>>> handler =
                runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category);
        if (handler.isEmpty()) return "no handler";
        RecipeHolder<CraftingRecipe> recipe = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
                .filter(r -> r.id().identifier().getPath().equals("crafting_table")).findFirst().orElse(null);
        if (recipe == null) return "no crafting table recipe in JEI";
        var error = handler.get().transferRecipe(menu, recipe, null, mc.player, false, true);
        return handler.get().getClass().getName() + (error == null ? " ok" : " error " + error.getType());
    }
}
