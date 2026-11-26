package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitConfigMenu;
import net.nexium.conduit.ConduitType;
import net.nexium.registry.ModBlocks;

/** A real right-click on a conduit: an empty hand opens the side menu, another item does not. */
public class ConduitEmptyHandClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                at[0] = player.blockPosition().offset(0, 0, -2);
                level.setBlockAndUpdate(at[0], ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                ((ConduitBundleBlockEntity) level.getBlockEntity(at[0])).addConduit(ConduitType.ITEM, 1);
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(at[0])));
            ctx.waitTicks(4);

            // A stick in hand: no menu
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().get(0);
                p.getInventory().setItem(p.getInventory().getSelectedSlot(), new ItemStack(Items.STICK));
            });
            ctx.waitTicks(3);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "a stick in hand opens no menu");

            // Empty hand: the side menu opens
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory().clearContent());
            ctx.waitTicks(3);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitFor(mc -> mc.gui.screen() != null);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> sc && sc.getMenu() instanceof ConduitConfigMenu),
                    "an empty-handed right-click opens the conduit menu");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] conduit empty hand menu: " + ctx.takeScreenshot("nexus_conduit_empty_hand_menu"));
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitFor(mc -> mc.gui.screen() == null);

            // Wearing a Power Meter as its facade, the same click opens the dashboard, and sneaking opens the side menu
            world.getServer().runOnServer(server -> {
                var be = (ConduitBundleBlockEntity) server.overworld().getBlockEntity(at[0]);
                be.addConduit(ConduitType.ENERGY, 1);
                be.setFacade(ModBlocks.POWER_METER.defaultBlockState());
            });
            ctx.waitTicks(5);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitFor(mc -> mc.gui.screen() != null);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof net.nexium.client.screen.PowerMeterScreen),
                    "a conduit wearing the Power Meter opens the dashboard on a right-click");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] meter facade dashboard: " + ctx.takeScreenshot("nexus_meter_facade_dashboard"));
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitFor(mc -> mc.gui.screen() == null);
            ctx.getInput().holdKey(options -> options.keyShift);
            ctx.waitTicks(3);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitFor(mc -> mc.gui.screen() != null);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> sc && sc.getMenu() instanceof ConduitConfigMenu),
                    "sneaking opens the conduit menu instead");
            ctx.getInput().releaseKey(options -> options.keyShift);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
