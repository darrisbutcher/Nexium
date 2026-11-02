package net.storagenexus.nexium;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Effects Nexium gear gives while it is held or worn: Haste II from the tools, Fire Resistance and Night Vision from
 * the armor. They are infinite, ambient and without particles, so the icon shows a steady infinity sign instead of
 * blinking. That combination is their signature: when the gear stops giving one, an infinite ambient effect at that
 * level is taken off, while a beacon's (never infinite), a potion's (never infinite) or a command's (never ambient)
 * is left alone. They don't stack with a beacon's or a potion's: the effect is the same.
 */
public final class HeldEffects {
    /** Gives or takes the effect at this amplifier, for whether the gear wants it now. */
    public static void set(ServerPlayer player, Holder<MobEffect> effect, int amplifier, boolean wanted) {
        MobEffectInstance current = player.getEffect(effect);
        boolean ours = current != null && current.isInfiniteDuration() && current.isAmbient() && current.getAmplifier() == amplifier;
        if (wanted && !ours && (current == null || current.getAmplifier() <= amplifier)) {
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
        } else if (!wanted && ours) {
            player.removeEffect(effect);
        }
    }

    private HeldEffects() {}
}
