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
