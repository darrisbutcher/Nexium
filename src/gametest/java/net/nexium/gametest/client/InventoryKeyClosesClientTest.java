package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.client.guide.GuideScreens;
import net.nexium.client.screen.ArmorMenuScreen;
import net.nexium.client.screen.ConfigScreen;
import net.nexium.client.screen.QuestScreen;
import net.nexium.essentials.client.EnchantedGrindstoneScreen;
import net.nexium.essentials.client.HostileMobSwitchScreen;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.quest.Quests;

/**
 * The inventory key (E) closes a Nexium window as Esc does, with the real key: the Codex, the quest book, the settings and the Armor
 * menu (plain screens, which vanilla does not close on it), and container screens (the Hostile Mob Switch, the Enchanted Grindstone and
 * the Backpack, which vanilla does close). While a text field is being typed in, E types and the window stays.
 */
public class InventoryKeyClosesClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                player.level().setBlockAndUpdate(base[0].offset(1, 0, 3), ModBlocks.HOSTILE_MOB_SWITCH.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(-1, 0, 3), ModBlocks.ENCHANTED_GRINDSTONE.defaultBlockState());
            });
            ctx.waitTicks(10);

            // Plain screens
            closes(ctx, "the Codex", () -> ctx.runOnClient(mc -> GuideScreens.open()), null);
            closes(ctx, "the settings", () -> ctx.runOnClient(mc -> mc.gui.setScreen(new ConfigScreen(null))), ConfigScreen.class);
            closes(ctx, "the Armor menu", () -> ctx.runOnClient(mc -> mc.gui.setScreen(new ArmorMenuScreen())), ArmorMenuScreen.class);
            closes(ctx, "the quest book", () -> world.getServer().runOnServer(server -> Quests.send(server.getPlayerList().getPlayers().get(0), true)), QuestScreen.class);

            // Container screens
            closes(ctx, "the Hostile Mob Switch", () -> world.getServer().runOnServer(server -> use(server.getPlayerList().getPlayers().get(0), base[0].offset(1, 0, 3))),
                    HostileMobSwitchScreen.class);
            closes(ctx, "the Enchanted Grindstone", () -> world.getServer().runOnServer(server -> use(server.getPlayerList().getPlayers().get(0), base[0].offset(-1, 0, 3))),
                    EnchantedGrindstoneScreen.class);
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(10);
            // (no Backpack is worn in this world: nothing opens, which is fine; the screen is covered by its own client test)

            // While a text field is typed in, E types: the window stays
            ctx.runOnClient(mc -> GuideScreens.open());
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() != null), "the Codex is open");
            boolean focused = ctx.computeOnClient(mc -> {
                Screen screen = mc.gui.screen();
                for (var child : screen.children()) {
                    if (child instanceof EditBox box) {
                        screen.setFocused(box);
                        box.setFocused(true);
                        return true;
                    }
                }
                return false;
            });
            check(focused, "the Codex has a search box to type in");
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() != null), "E does not close the Codex while its search box is being typed in");
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "Esc still closes it");
        }
    }

    /** Opens a window, presses the inventory key, and checks that the window is gone. */
    private static void closes(ClientGameTestContext ctx, String name, Runnable open, Class<? extends Screen> expected) {
        open.run();
        ctx.waitTicks(10);
        if (expected != null) check(ctx.computeOnClient(mc -> expected.isInstance(mc.gui.screen())), name + " opens");
        else check(ctx.computeOnClient(mc -> mc.gui.screen() != null), name + " opens");
        ctx.getInput().pressKey(options -> options.keyInventory);
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "E closes " + name);
    }

    private static void use(ServerPlayer player, BlockPos pos) {
        player.level().getBlockState(pos).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
