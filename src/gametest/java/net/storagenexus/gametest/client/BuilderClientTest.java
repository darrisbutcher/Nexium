package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.builder.BuilderItem;
import net.storagenexus.builder.BuilderSettings;
import net.storagenexus.builder.BuilderTier;
import net.storagenexus.client.AiotHud;
import net.storagenexus.client.AiotPreview;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.client.screen.BuilderScreen;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/**
 * The Construction Builder in a real game: with a Nexium builder in hand, looking at a 5x5 stone platform, the 25 spots
 * it would fill are outlined in blue and the readout shows its mode and size. A right-click builds them; G opens its
 * menu, which shows one build to undo; its Undo button takes the build back. Screenshots of each step.
 */
public class BuilderClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] origin = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                BlockPos base = player.blockPosition();
                origin[0] = base;
                // A 5x5 stone platform in the ground just ahead (south), with the player looking at its middle
                for (int x = -2; x <= 2; x++) for (int z = 1; z <= 5; z++) {
                    player.level().setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                }
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 28.0F);
                ItemStack builder = new ItemStack(BuilderItem.of(BuilderTier.NEXIUM));
                ItemStack link = new ItemStack(ModItems.MODULES.get(ModuleType.NEXUS_LINK));
                link.set(ModComponents.TERMINAL_LINK, GlobalPos.of(player.level().dimension(), base.below(3)));
                BuilderSettings.setModule(builder, link);
                player.getInventory().setItem(0, builder);
                player.getInventory().setItem(1, new ItemStack(Items.STONE, 64));
                player.getInventory().setItem(2, new ItemStack(Items.OAK_PLANKS, 64));
                player.getInventory().setItem(3, new ItemStack(BuilderItem.of(BuilderTier.IRON)));
                player.getInventory().setItem(4, new ItemStack(BuilderItem.of(BuilderTier.DIAMOND)));
                player.getInventory().setItem(5, new ItemStack(BuilderItem.of(BuilderTier.NETHERITE)));
            });
            ctx.waitTicks(5);
            // The view is the client's: look down at the platform's middle
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(28.0F);
            });
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[0]);
            ctx.waitTicks(20);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear()); // the recipe toasts would cover the menu's title
            int outlined = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(outlined == 25, "the 25 spots above the platform are outlined, got " + outlined);
            String hud = ctx.computeOnClient(BuilderClientTest::hud);
            check(hud.equals("Extend (any) | Size: 128 / 128 | Nexus Link: on"), "the readout shows the builder, got: " + hud);
            System.out.println("[nexus] builder preview: " + ctx.takeScreenshot("nexus_builder_preview"));

            // Right-click builds them
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(10);
            int built = world.getServer().computeOnServer(server -> countStone(server.getPlayerList().getPlayers().get(0), origin[0]));
            check(built == 25, "25 stone placed, got " + built);
            System.out.println("[nexus] builder built: " + ctx.takeScreenshot("nexus_builder_built"));

            // G opens the builder's menu; it has one build to undo
            ctx.getInput().pressKey(options -> ToolMenuClient.TOOL_MENU);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BuilderScreen), "G opens the builder menu");
            check(ctx.computeOnClient(mc -> mc.gui.screen().getTitle().getString()).equals("Nexium Builder"), "titled Nexium Builder");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            String undo = ctx.computeOnClient(mc -> widget(mc, 3).getMessage().getString());
            check(undo.equals("Undo last build (1 left)"), "Undo shows one build, got: " + undo);
            System.out.println("[nexus] builder menu: " + ctx.takeScreenshot("nexus_builder_menu"));
            // Hover the Mode button for its hint beside the panel
            hover(ctx, 0);
            System.out.println("[nexus] builder menu hint: " + ctx.takeScreenshot("nexus_builder_menu_hint"));
            // Mode steps to Line
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> BuilderSettings.mode(mc.player.getMainHandItem()) == BuilderSettings.Mode.LINE), "Mode steps to Line");
            // Undo takes the build back
            hover(ctx, 3);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(10);
            int left = world.getServer().computeOnServer(server -> countStone(server.getPlayerList().getPlayers().get(0), origin[0]));
            check(left == 0, "Undo removes the 25 blocks, " + left + " left");
            String after = ctx.computeOnClient(mc -> widget(mc, 3).getMessage().getString());
            check(after.equals("Undo last build (0 left)"), "nothing more to undo, got: " + after);
            check(ctx.computeOnClient(mc -> !widget(mc, 3).active), "Undo is greyed out");
            int stone = ctx.computeOnClient(mc -> mc.player.getInventory().countItem(Items.STONE));
            check(stone == 64, "the stone came back, have " + stone);
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(2);
            System.out.println("[nexus] builder menu after undo: " + ctx.takeScreenshot("nexus_builder_menu_undone"));
            ctx.getInput().pressKey(options -> ToolMenuClient.TOOL_MENU);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "the same key closes it");

            // Line along the look, with planks from the offhand
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, player.getInventory().getItem(2).copyAndClear());
            });
            ctx.waitTicks(10);
            int line = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(line == 5, "Line outlines one row of 5, got " + line);
            System.out.println("[nexus] builder line preview: " + ctx.takeScreenshot("nexus_builder_line"));
        }
    }

    private static int countStone(ServerPlayer player, BlockPos base) {
        int n = 0;
        for (int x = -2; x <= 2; x++) for (int z = 1; z <= 5; z++) {
            if (player.level().getBlockState(base.offset(x, 0, z)).is(Blocks.STONE)) n++;
        }
        return n;
    }

    private static AbstractWidget widget(Minecraft mc, int index) {
        return (AbstractWidget) mc.gui.screen().children().get(index);
    }

    private static void hover(ClientGameTestContext ctx, int index) {
        double[] at = ctx.computeOnClient(mc -> {
            var w = widget(mc, index);
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(w.getX() + w.getWidth() / 2.0) * scale, (w.getY() + w.getHeight() / 2.0) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(3);
    }

    private static String hud(Minecraft mc) {
        return AiotHud.lines(mc, mc.player.getMainHandItem()).stream().map(l -> l.text().getString())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
