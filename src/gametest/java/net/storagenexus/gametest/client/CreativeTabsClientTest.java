package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.storagenexus.registry.ModTab;

import java.util.List;

/** The three Nexium creative tabs in a real game: each is opened and shown. */
public class CreativeTabsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            ctx.waitTicks(10);
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            // Vanilla's tabs fill the first page, so the mod's tabs are on the next one: press the page arrow
            ctx.getInput().setCursorPos(905, 185);
            ctx.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(5);
            System.out.println("[nexus] creative page 2: " + ctx.takeScreenshot("nexus_creative_page2"));
            List<CreativeModeTab> tabs = List.of(ModTab.TAB, ModTab.TOOLS_TAB, ModTab.FARMS_TAB);
            String[] names = {"storage", "tools", "farms_power"};
            for (int i = 0; i < 3; i++) {
                CreativeModeTab tab = tabs.get(i);
                ctx.runOnClient(mc -> {
                    if (mc.gui.screen() instanceof CreativeModeInventoryScreen screen) {
                        try {
                            var selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                            selectTab.setAccessible(true);
                            selectTab.invoke(screen, tab);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                ctx.waitTicks(5);
                int count = ctx.computeOnClient(mc -> mc.gui.screen() instanceof CreativeModeInventoryScreen screen ? screen.getMenu().items.size() : -1);
                if (count <= 0) throw new AssertionError("the " + names[i] + " tab shows no items: " + count);
                System.out.println("[nexus] creative tab " + names[i] + " shows " + count + " items: "
                        + ctx.takeScreenshot("nexus_creative_tab_" + names[i]));
            }
        }
    }
}
