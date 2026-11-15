package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.client.FreeCamera;

import java.util.Arrays;

/**
 * The free camera in a real game: the key is on the Key Binds list (K by default), it detaches the camera while the player stays
 * where they are, the movement keys fly it, clicks do not reach the world, the key brings it back, and being hurt brings it back
 * too. Takes a screenshot from the camera looking back at the player.
 */
public class FreeCameraClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(FreeCamera.TOGGLE), "the free camera key is in the game's key list");
            check(FreeCamera.TOGGLE.getDefaultKey().getValue() == InputConstants.KEY_K, "it defaults to K");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                base[0] = player.blockPosition();
                for (int x = -6; x <= 6; x++) for (int z = -14; z <= 8; z++) for (int y = 0; y <= 6; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                // A stone block three blocks in front of where the camera will be after it has flown back, level with it
                player.level().setBlockAndUpdate(base[0].offset(0, 1, -3), Blocks.STONE.defaultBlockState());
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 0.0F);
            });
            ctx.waitTicks(15);
            Vec3 start = ctx.computeOnClient(mc -> mc.player.position());

            // On: the camera is somewhere other than the player
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> FreeCamera.active() && mc.getCameraEntity() != mc.player && mc.getCameraEntity() == FreeCamera.camera()),
                    "K detaches the camera from the player");
            check(ctx.computeOnClient(mc -> !(mc.player.input instanceof KeyboardInput)), "and the player's own keys are parked");

            // The scroll wheel: on its own it does nothing (not the speed, not the hotbar); with the speed key (Left Ctrl) held it changes the speed
            check(ctx.computeOnClient(mc -> FreeCamera.SPEED_MODIFIER.getDefaultKey().getValue() == InputConstants.KEY_LCONTROL), "the speed key defaults to Left Ctrl");
            double speed0 = ctx.computeOnClient(mc -> FreeCamera.speed());
            int slot0 = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
            ctx.getInput().scroll(1);
            ctx.waitTicks(3);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> FreeCamera.speed()) == speed0, "plain scroll does not change the speed");
            check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == slot0, "and does not change the hotbar slot either");
            ctx.getInput().holdKey(FreeCamera.SPEED_MODIFIER);
            ctx.waitTicks(2);
            ctx.getInput().scroll(1);
            ctx.waitTicks(3);
            double faster = ctx.computeOnClient(mc -> FreeCamera.speed());
            check(faster > speed0, "Left Ctrl and scroll up make it faster: " + speed0 + " to " + faster);
            ctx.getInput().scroll(-1);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            double slower = ctx.computeOnClient(mc -> FreeCamera.speed());
            check(slower < speed0, "and scroll down slower: " + slower);
            ctx.getInput().releaseKey(FreeCamera.SPEED_MODIFIER);
            ctx.waitTicks(2);
            ctx.getInput().scroll(1);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> FreeCamera.speed()) == slower, "with Ctrl let go, the scroll does nothing again");
            check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == slot0, "and the hotbar slot is still the same");

            // Fly back (north, behind the player) with the sneak key down for a moment too: the camera moves, the player does not
            ctx.getInput().holdKey(options -> options.keyDown);
            ctx.waitTicks(16);
            ctx.getInput().releaseKey(options -> options.keyDown);
            ctx.waitTicks(3);
            Vec3 cameraAt = ctx.computeOnClient(mc -> mc.getCameraEntity().position());
            Vec3 playerAt = ctx.computeOnClient(mc -> mc.player.position());
            check(cameraAt.distanceTo(start.add(0, 1.62, 0)) > 4.0, "the camera flew away from where it started: " + cameraAt + " from " + start);
            check(playerAt.distanceTo(start) < 0.01, "while the player stayed put: " + playerAt + " from " + start);
            check(ctx.computeOnClient(mc -> mc.player.input.keyPresses.equals(net.minecraft.world.entity.player.Input.EMPTY)), "no key is pressed for the player");
            // Clicks do not reach the world: in creative a left-click breaks what the crosshair is on, and the camera is looking at the stone
            boolean onStone = ctx.computeOnClient(mc -> mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult b && b.getBlockPos().equals(base[0].offset(0, 1, -3)));
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(6);
            check(world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).level().getBlockState(base[0].offset(0, 1, -3)).is(Blocks.STONE)),
                    "the stone is still there after a left-click with the camera away (crosshair on it: " + onStone + ")");

            // With the stone out of the way, the camera looks back at the player standing where they were (and no hand is drawn)
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level().setBlockAndUpdate(base[0].offset(0, 1, -3), Blocks.AIR.defaultBlockState()));
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] free camera: " + ctx.takeScreenshot("nexus_free_camera"));

            // Off: back on the player
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> !FreeCamera.active() && mc.getCameraEntity() == mc.player), "K puts the camera back on the player");
            check(ctx.computeOnClient(mc -> mc.player.input instanceof KeyboardInput), "and the player's keys work again");

            // Being hurt puts it back at once
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(5);
            world.getServer().runCommand("xp add @p 7 levels");
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] experience bar shown: " + ctx.takeScreenshot("nexus_free_camera_xp_off"));
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> FreeCamera.active()), "it goes on again");
            ctx.waitTicks(25);
            System.out.println("[nexus] experience bar hidden: " + ctx.takeScreenshot("nexus_free_camera_xp_on"));
            world.getServer().runCommand("damage @p 1");
            ctx.waitTicks(6);
            check(ctx.computeOnClient(mc -> !FreeCamera.active() && mc.getCameraEntity() == mc.player), "when the player is hurt the camera comes back by itself");
            check(ctx.computeOnClient(mc -> mc.player.input instanceof KeyboardInput), "with the player's keys working");
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
