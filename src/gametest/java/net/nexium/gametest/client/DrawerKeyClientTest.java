package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.drawer.DrawerWood;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;
import net.nexium.upgrade.UpgradeMenu;

/** The Drawer Key in a real game: right-clicks lock and unlock a drawer, the padlock shows on its front, and the upgrade screen has four slots. */
public class DrawerKeyClientTest implements FabricClientGameTest {
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
                level.setBlockAndUpdate(at[0], ModBlocks.DRAWERS.get(DrawerWood.OAK)[0].defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                if (level.getBlockEntity(at[0]) instanceof DrawerBlockEntity d) d.insertFromHand(0, new ItemStack(Items.DIAMOND, 64));
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(ModItems.DRAWER_KEY, 4));
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(at[0]).add(0, 0, 0.5)));
            ctx.waitTicks(4);
            check(!locked(world, at[0]), "the drawer starts unlocked");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] drawer key unlocked: " + ctx.takeScreenshot("nexus_drawer_key_unlocked"));

            // A real right-click on the front with the key locks it
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(6);
            check(locked(world, at[0]), "a right-click with the key locks the drawer");
            check(keys(world) == 4, "and the key is not used up: " + keys(world));
            ctx.waitTicks(25);
            System.out.println("[nexus] drawer key locked: " + ctx.takeScreenshot("nexus_drawer_key_locked"));

            // ... and a second one unlocks it
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(6);
            check(!locked(world, at[0]), "a second right-click unlocks it");
            check(keys(world) == 4, "still four keys: " + keys(world));
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(6);
            check(locked(world, at[0]), "and a third locks it again");

            // The upgrade screen: four slots, no lock slot (empty hand, the top)
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory().clearContent());
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(at[0]), Direction.UP, at[0], false)));
            ctx.waitFor(mc -> mc.gui.screen() != null);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> sc && sc.getMenu() instanceof UpgradeMenu m
                    && m.slots.size() == 4 + 36), "the upgrade screen has four slots and the inventory");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] drawer upgrade screen: " + ctx.takeScreenshot("nexus_drawer_upgrade_screen_no_lock_slot"));
        }
    }

    private static boolean locked(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer(server -> server.overworld().getBlockEntity(pos) instanceof DrawerBlockEntity d && d.isLocked());
    }

    private static int keys(TestSingleplayerContext world) {
        return world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory().countItem(ModItems.DRAWER_KEY));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
