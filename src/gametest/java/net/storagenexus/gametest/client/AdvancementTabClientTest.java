package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.Identifier;

/**
 * The Nexium tab on the real advancements screen: after a moment in a world all 33 nodes are done (the tick
 * trigger), and the screen opens on the Nexium tab. Takes a screenshot of it.
 */
public class AdvancementTabClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(40);
            Identifier rootId = Identifier.fromNamespaceAndPath("storagenexus", "guide/root");
            long done = ctx.computeOnClient(mc -> {
                var advancements = mc.player.connection.getAdvancements();
                return advancements.progress().entrySet().stream()
                        .filter(e -> e.getKey().id().getNamespace().equals("storagenexus")
                                && e.getKey().id().getPath().startsWith("guide/") && e.getValue().isDone())
                        .count();
            });
            check(done == 33, "all 33 Nexium nodes are done from the start, got " + done);

            ctx.setScreen(() -> {
                var advancements = ctx.computeOnClient(mc -> mc.player.connection.getAdvancements());
                AdvancementHolder root = advancements.get(rootId);
                advancements.setSelectedTab(root, false);
                return new AdvancementsScreen(advancements);
            });
            ctx.waitTicks(10);
            System.out.println("[nexus] advancement tab: " + ctx.takeScreenshot("nexus_advancement_tab"));
            ctx.setScreen(() -> null);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
