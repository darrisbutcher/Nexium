package net.nexium.quest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.nexium.Nexium;

import java.util.ArrayList;
import java.util.List;

/**
 * Every Nexium recipe is in the recipe book from the start: a player gets all of them when they join a world (a new world, or one they
 * come back to, which also picks up recipes a newer version added), and again after a reload of the data packs. Recipes of other mods
 * and the game's own unlock the way they always do.
 */
public final class RecipeUnlock {
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> unlock(handler.getPlayer()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (!success) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) unlock(player);
        });
    }

    /** Puts every Nexium recipe the recipe book can hold into the player's recipe book. Returns how many were new to them. */
    public static int unlock(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return 0;
        List<RecipeHolder<?>> mine = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getLearnableRecipes()) {
            if (holder.id().identifier().getNamespace().equals(Nexium.MOD_ID)) mine.add(holder);
        }
        return player.awardRecipes(mine);
    }

    private RecipeUnlock() {}
}
