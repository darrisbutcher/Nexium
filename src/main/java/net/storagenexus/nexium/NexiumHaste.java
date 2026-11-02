package net.storagenexus.nexium;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModComponents;

/**
 * Nexium mining tools give the real Haste II effect while held in the main hand, unless it is turned off in the tool
 * menu. With Efficiency V that is exactly netherite with Efficiency V under a Haste II beacon (49 mining speed: stone
 * and netherrack break instantly), and like the beacon's it also speeds up attacks by 20%.
 * <p>
 * The effect is infinite, ambient and without particles, so its icon shows a steady infinity sign instead of
 * blinking. That combination is its signature: when the player stops holding the tool, an infinite ambient Haste II is
 * taken off, and a beacon's (which is never infinite) or a command's (never ambient) is left alone. It does not stack
 * with a beacon's Haste II: the effect is the same.
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
        boolean wanted = grants(player.getMainHandItem()) && !player.isSpectator();
        MobEffectInstance current = player.getEffect(MobEffects.HASTE);
        boolean ours = current != null && current.isInfiniteDuration() && current.isAmbient() && current.getAmplifier() == AMPLIFIER;
        if (wanted && !ours && (current == null || current.getAmplifier() <= AMPLIFIER)) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, MobEffectInstance.INFINITE_DURATION, AMPLIFIER,
                    true, false, true));
        } else if (!wanted && ours) {
            player.removeEffect(MobEffects.HASTE);
        }
    }

    private NexiumHaste() {}
}
