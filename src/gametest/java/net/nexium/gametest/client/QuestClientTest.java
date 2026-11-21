package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.nexium.client.screen.QuestScreen;
import net.nexium.quest.QuestBook;
import net.nexium.quest.Quests;

/**
 * The quest book in a real game: a player who already holds some of the first chapter's items opens it; those quests are
 * ticked with a Nexium reward in the inventory, the rest show how far along they are, the next chapter is locked until the
 * first is finished, and then it opens.
 */
public class QuestClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            // Joining a world puts every Nexium recipe in the recipe book
            check(world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getRecipeBook()
                            .contains(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.nexium.Nexium.id("conduit_wrench")))),
                    "the Nexium recipes are unlocked when a world starts");
            var chapters = QuestBook.chapters();
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                // Three of the first chapter's seven quests are met already
                for (int i = 0; i < 3; i++) {
                    var q = chapters.get(0).quests().get(i);
                    player.getInventory().add(new ItemStack(q.item(), q.count()));
                }
                // A later chapter's item, held but not counting yet
                var later = chapters.get(1).quests().get(3);
                player.getInventory().add(new ItemStack(later.item(), later.count()));
                Quests.check(player);
                Quests.send(player, true);
            });
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof QuestScreen), "the quest book opens");
            check(ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).done(chapters.get(0).quests().get(0))), "a quest already met is ticked");
            check(!ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).done(chapters.get(0).quests().get(5))), "one not met is not");
            check(ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).isOpen(0)) && !ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).isOpen(1)),
                    "the first chapter is open and the second locked");
            System.out.println("[nexus] quests chapter 1: " + ctx.takeScreenshot("nexus_quests_storage"));

            // Finish the first chapter: the second opens and the item already held counts
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                for (int i = 3; i < chapters.get(0).quests().size(); i++) {
                    var q = chapters.get(0).quests().get(i);
                    player.getInventory().add(new ItemStack(q.item(), q.count()));
                }
                Quests.check(player);
                Quests.send(player, false);
            });
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).isOpen(1)), "finishing the first chapter opens the second");
            ctx.runOnClient(mc -> ((QuestScreen) mc.gui.screen()).showChapter(1));
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> ((QuestScreen) mc.gui.screen()).done(chapters.get(1).quests().get(3))), "the item held counts as soon as the chapter opens");
            System.out.println("[nexus] quests chapter 2: " + ctx.takeScreenshot("nexus_quests_power"));
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
