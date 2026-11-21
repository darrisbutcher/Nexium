package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * A click the way a player makes it: the cursor is moved to a spot and the left button pressed, through the game's own mouse handling,
 * so it arrives as the event a real click does. Calling a screen's {@code mouseClicked} with an event built by hand does not: it takes
 * whatever button number the test gives it, and the left button is {@link InputConstants#MOUSE_BUTTON_LEFT} (1, not 0), which is the
 * mistake that once let a screen that ignored every real left click pass its tests.
 */
final class RealMouse {
    private RealMouse() {
    }

    /** A left click at the screen (GUI) position {@code x, y}, then a few ticks for whatever it does to happen. */
    static void leftClick(ClientGameTestContext ctx, int[] position) {
        leftClick(ctx, position[0], position[1]);
    }

    static void leftClick(ClientGameTestContext ctx, double guiX, double guiY) {
        moveTo(ctx, guiX, guiY);
        ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(3);
    }

    /** Moves the cursor to the screen (GUI) position {@code position}, so it rests there (a hint shows, say). */
    static void moveTo(ClientGameTestContext ctx, int[] position) {
        moveTo(ctx, position[0], position[1]);
    }

    static void moveTo(ClientGameTestContext ctx, double guiX, double guiY) {
        // The mouse works in window pixels; the screen in GUI units, which are that divided by the GUI scale
        double[] pixels = ctx.computeOnClient(mc -> {
            var window = mc.getWindow();
            return new double[]{guiX * window.getScreenWidth() / window.getGuiScaledWidth(), guiY * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        ctx.getInput().setCursorPos(pixels[0], pixels[1]);
        ctx.waitTicks(2);
    }
}
