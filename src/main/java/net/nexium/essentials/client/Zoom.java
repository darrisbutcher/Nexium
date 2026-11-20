package net.nexium.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Zoom: hold the zoom key (none until you pick one, under Controls > Key Binds, in Nexium) and the view closes in smoothly; the
 * scroll wheel, while it is held, zooms further or less far (the hotbar does not change) and the level is remembered for next time;
 * let go and it eases back out. Mouse turning slows down by the same amount so aiming stays steady. Works in the free camera too.
 * All on the client.
 */
public final class Zoom {
    /** Hold to zoom. Unbound until the player picks a key. */
    public static final KeyMapping KEY = new KeyMapping("key.nexium.zoom", InputConstants.UNKNOWN.getValue(), NexiumEssentialsClient.CATEGORY);

    /** The weakest and strongest zoom, and where it starts (times magnification). */
    public static final double MIN = 1.25, MAX = 20.0, START = 4.0;
    /** How much one notch of the wheel changes the level. */
    private static final double STEP = 1.15;

    private static double level = START;
    private static float current = 1.0F, previous = 1.0F;

    public static void init() {
        KeyMappingHelper.registerKeyMapping(KEY);
        ClientTickEvents.END_CLIENT_TICK.register(Zoom::tick);
    }

    /** Whether the zoom key is being held with the game in view (not in a menu). */
    public static boolean held() {
        Minecraft mc = Minecraft.getInstance();
        return KEY.isDown() && mc.player != null && mc.gui.screen() == null;
    }

    private static void tick(Minecraft mc) {
        previous = current;
        float goal = held() ? (float) level : 1.0F;
        // Ease in log space, so zooming in and out feels the same speed at any level
        float eased = (float) Math.exp(Mth.lerp(0.35, Math.log(current), Math.log(goal)));
        current = Math.abs(eased - goal) < 0.005F ? goal : eased;
    }

    /** How much the view is magnified at this moment: 1 when not zooming. */
    public static float magnification(float partialTick) {
        return Mth.lerp(partialTick, previous, current);
    }

    /** What the field of view becomes with the zoom: the picture scales by the magnification, whatever the field of view started as. */
    public static float fov(float fov, float partialTick) {
        float magnification = magnification(partialTick);
        if (magnification <= 1.0001F) return fov;
        return zoomedFov(fov, magnification);
    }

    /** The field of view that shows the picture {@code magnification} times larger than {@code fov} does. */
    public static float zoomedFov(float fov, double magnification) {
        double half = Math.toRadians(fov) / 2.0;
        return (float) Math.toDegrees(2.0 * Math.atan(Math.tan(half) / magnification));
    }

    /** What mouse turning is multiplied by while zoomed: less, the closer in the view is. */
    public static double lookScale() {
        return 1.0 / Math.max(1.0, current);
    }

    /**
     * The scroll wheel while the zoom key is held: changes the zoom level instead of the hotbar slot. Returns whether it took the scroll.
     */
    public static boolean scroll(double amount) {
        if (!held()) return false;
        if (amount == 0) return true;
        level = Mth.clamp(level * Math.pow(STEP, amount), MIN, MAX);
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendOverlayMessage(Component.translatable("message.nexium.zoom", String.format(Locale.ROOT, "%.1f", level)));
        return true;
    }

    /** The remembered zoom level, for tests. */
    public static double level() {
        return level;
    }

    /** Puts the zoom back to not zooming at its starting level, for tests. */
    public static void reset() {
        level = START;
        current = previous = 1.0F;
    }

    private Zoom() {}
}
