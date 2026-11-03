package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.storagenexus.essentials.client.NexusEssentialsClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The magnet's key bind can be changed in the game's menus: it is on the real Options > Controls > Key Binds screen,
 * under the Nexium heading, as Toggle Magnet on M. Takes a screenshot scrolled to it.
 */
public class KeyBindsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        KeyMapping magnet = NexusEssentialsClient.TOGGLE_MAGNET;
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(magnet), "the magnet key is in the game's key list");
            check(magnet.getDefaultKey().getValue() == InputConstants.KEY_M, "it defaults to M");
        });

        ctx.setScreen(() -> new KeyBindsScreen(null, ctx.computeOnClient(mc -> mc.options)));
        ctx.waitTicks(5);
        String heading = ctx.computeOnClient(mc -> {
            try {
                Field listField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
                listField.setAccessible(true);
                KeyBindsList list = (KeyBindsList) listField.get(mc.gui.screen());
                Field keyField = KeyBindsList.KeyEntry.class.getDeclaredField("key");
                keyField.setAccessible(true);
                Field nameField = KeyBindsList.CategoryEntry.class.getDeclaredField("categoryName");
                nameField.setAccessible(true);
                String lastHeading = null;
                for (Object entry : list.children()) {
                    if (entry instanceof KeyBindsList.CategoryEntry c) lastHeading = ((AbstractWidget) nameField.get(c)).getMessage().getString();
                    if (entry instanceof KeyBindsList.KeyEntry k && keyField.get(k) == magnet) {
                        Method center = Arrays.stream(AbstractSelectionList.class.getDeclaredMethods())
                                .filter(m -> m.getName().equals("centerScrollOn")).findFirst().orElseThrow();
                        center.setAccessible(true);
                        center.invoke(list, k);
                        return lastHeading;
                    }
                }
                return null;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("couldn't read the Key Binds screen", e);
            }
        });
        check(heading != null, "Toggle Magnet is listed on the Key Binds screen");
        check("Nexium".equals(heading), "Toggle Magnet is under the Nexium heading, not '" + heading + "'");
        ctx.waitTicks(5);
        System.out.println("[nexus] key binds: " + ctx.takeScreenshot("nexus_key_binds_magnet"));
        ctx.setScreen(() -> null);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
