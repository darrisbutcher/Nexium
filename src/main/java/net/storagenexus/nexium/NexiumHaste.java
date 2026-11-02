package net.storagenexus.nexium;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModComponents;

/**
 * Nexium mining tools give the real Haste II effect while held in the main hand, unless it is turned off in the tool
 * menu. With Efficiency V that is exactly netherite with Efficiency V under a Haste II beacon (49 mining speed: stone
 * and netherrack break instantly), and like the beacon's it also speeds up attacks by 20%.
 * <p>
 * The effect is given and taken by {@link HeldEffects}, so a beacon's or a command's Haste is never taken away.
 */
public final class NexiumHaste {
    private static final int AMPLIFIER = 1; // Haste II
    private static final int CHECK_EVERY_TICKS = 5;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % CHECK_EVERY_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) update(player);
        });
    }

    /** Whether this stack gives Haste II when held: a Nexium mining tool whose Haste isn't turned off. */
    public static boolean grants(ItemStack stack) {
        return stack.is(NexiumMaterials.GRANTS_HASTE) && !stack.has(ModComponents.HASTE_OFF);
    }

    /** Gives or takes the Haste II for what the player holds now. */
    public static void update(ServerPlayer player) {
        HeldEffects.set(player, MobEffects.HASTE, AMPLIFIER, grants(player.getMainHandItem()) && !player.isSpectator());
    }

    private NexiumHaste() {}
}
