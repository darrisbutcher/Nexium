package net.nexium.filter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashSet;
import java.util.Set;

/**
 * The two matches a filter can ask the game about: whether an item can be smelted (a furnace, blast furnace or smoker has a recipe for
 * it, so modded and data pack recipes count) and whether it burns as furnace fuel. Fuel is a property of the item; what can be smelted
 * comes from the recipes, so it is collected when a server starts and again after every data pack reload. The conduits that use it
 * run on the server, which is the only place it is filled in.
 */
public final class FilterSpecials {
    private static volatile Set<Item> smeltable = Set.of();

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(FilterSpecials::refresh);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) refresh(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> smeltable = Set.of());
    }

    /** Collects every item that is the input of a smelting, blasting or smoking recipe. */
    static void refresh(MinecraftServer server) {
        Set<Item> out = new HashSet<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof AbstractCookingRecipe cooking)) continue;
            var type = cooking.getType();
            if (type != RecipeType.SMELTING && type != RecipeType.BLASTING && type != RecipeType.SMOKING) continue;
            cooking.input().items().forEach(item -> out.add(item.value()));
        }
        smeltable = Set.copyOf(out);
    }

    /** Whether a furnace, blast furnace or smoker can smelt this item. */
    public static boolean isSmeltable(Item item) {
        return smeltable.contains(item);
    }

    /** Whether this item burns as furnace fuel. */
    public static boolean isFuel(Item item) {
        return new ItemStack(item).has(DataComponents.COOKING_FUEL);
    }

    private FilterSpecials() {}
}
