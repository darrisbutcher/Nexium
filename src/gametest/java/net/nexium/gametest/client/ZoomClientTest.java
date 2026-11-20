package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.nexium.essentials.client.FreeCamera;
import net.nexium.essentials.client.Zoom;

import java.util.Arrays;

/**
 * Zoom and the free camera's missing fog, in a real game: the zoom key is on the Key Binds list and unbound at first; held, the view
 * closes in smoothly (the field of view narrows, mouse turning slows down), the wheel changes the level without touching the hotbar, and
 * letting go eases back out. The free camera has no fog while it is away and the fog is back when it is put back on the player.
 */
public class ZoomClientTest implements FabricClientGameTest {
    private static float fogEnd(Minecraft mc) {
        return mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.fogData.renderDistanceEnd;
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(Zoom.KEY), "the zoom key is in the game's key list");
            check(Zoom.KEY.isUnbound(), "and it has no key until the player picks one");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition();
                for (int x = -8; x <= 8; x++) for (int z = -3; z <= 30; z++) {
                    player.level().setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 0; y < 5; y++) player.level().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                // A tower far down the line of sight, so zooming in shows it bigger
                for (int y = 0; y < 3; y++) player.level().setBlockAndUpdate(base.offset(0, y, 24), Blocks.REDSTONE_BLOCK.defaultBlockState());
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
            });
            ctx.waitTicks(15);
            ctx.getInput().pressKey(options -> options.keyToggleGui); // no HUD in the shots

            try {
                Zoom.reset();
                // Not held: nothing happens, whatever key is bound
                ctx.runOnClient(mc -> {
                    Zoom.KEY.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_B));
                    KeyMapping.resetMapping();
                });
                ctx.waitTicks(10);
                float fov0 = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
                check(ctx.computeOnClient(mc -> Zoom.magnification(1.0F)) == 1.0F, "there is no magnification before the key is held");
                check(ctx.computeOnClient(mc -> Zoom.lookScale()) == 1.0, "and the mouse turns at full speed");
                System.out.println("[nexus] zoom off: " + ctx.takeScreenshot("nexus_zoom_off"));

                // Held: the view closes in smoothly to the starting level
                ctx.getInput().holdKey(Zoom.KEY);
                ctx.waitTicks(3);
                float early = ctx.computeOnClient(mc -> Zoom.magnification(1.0F));
                check(early > 1.0F && early < 4.0F, "it eases in rather than jumping: " + early + "x after three ticks");
                ctx.waitTicks(40);
                float magnified = ctx.computeOnClient(mc -> Zoom.magnification(1.0F));
                check(Math.abs(magnified - 4.0F) < 0.05F, "it settles at the starting level of 4x: " + magnified);
                float fovZoomed = ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
                float expected = Zoom.zoomedFov(fov0, 4.0);
                check(Math.abs(fovZoomed - expected) < 0.2F && fovZoomed < fov0 * 0.4F,
                        "the field of view narrows from " + fov0 + " to " + fovZoomed + " (expected about " + expected + ")");
                check(Math.abs(ctx.computeOnClient(mc -> Zoom.lookScale()) - 0.25) < 0.01, "and the mouse turns a quarter as fast");
                System.out.println("[nexus] zoom 4x: " + ctx.takeScreenshot("nexus_zoom_4x"));

                // The wheel changes the level, not the hotbar slot
                int slot = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
                double level0 = ctx.computeOnClient(mc -> Zoom.level());
                ctx.getInput().scroll(3);
                ctx.waitTicks(40);
                double level1 = ctx.computeOnClient(mc -> Zoom.level());
                check(Math.abs(level1 - level0 * Math.pow(1.15, 3)) < 0.01, "three notches up zoom in further: " + level0 + " to " + level1);
                check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == slot, "and the hotbar slot did not change");
                check(Math.abs(ctx.computeOnClient(mc -> Zoom.magnification(1.0F)) - level1) < 0.05, "the view followed it");
                ctx.getInput().scroll(-30);
                ctx.waitTicks(5);
                check(ctx.computeOnClient(mc -> Zoom.level()) == Zoom.MIN, "scrolling down far stops at the weakest zoom");
                ctx.getInput().scroll(60);
                ctx.waitTicks(5);
                check(ctx.computeOnClient(mc -> Zoom.level()) == Zoom.MAX, "and scrolling up far stops at the strongest");
                ctx.getInput().scroll(-60);
                ctx.getInput().scroll(15);
                ctx.waitTicks(5);

                // Let go: back out, and the hotbar scrolls again
                ctx.getInput().releaseKey(Zoom.KEY);
                ctx.waitTicks(50);
                check(ctx.computeOnClient(mc -> Zoom.magnification(1.0F)) == 1.0F, "letting go eases back to no zoom");
                check(Math.abs(ctx.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov()) - fov0) < 0.01F, "with the field of view back where it was");
                int before = ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
                ctx.getInput().scroll(1);
                ctx.waitTicks(3);
                check(ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) != before, "and the wheel changes the hotbar slot again");

                // Fog: on while the camera is on the player, gone while the free camera is away
                ctx.waitTicks(5);
                float fogNear = ctx.computeOnClient(ZoomClientTest::fogEnd);
                check(fogNear > 0 && fogNear < 1.0E6F, "there is fog in normal play: ends at " + fogNear);
                ctx.getInput().pressKey(FreeCamera.TOGGLE);
                ctx.waitTicks(5);
                float fogFree = ctx.computeOnClient(ZoomClientTest::fogEnd);
                check(ctx.computeOnClient(mc -> FreeCamera.active()), "the free camera is on");
                check(fogFree >= 1.0E6F, "and its fog is gone: ends at " + fogFree);
                System.out.println("[nexus] free camera no fog: " + ctx.takeScreenshot("nexus_free_camera_no_fog"));
                // Zoom works from the free camera too
                ctx.getInput().holdKey(Zoom.KEY);
                ctx.waitTicks(40);
                check(ctx.computeOnClient(mc -> Zoom.magnification(1.0F)) > 1.5F, "the zoom works from the free camera too");
                ctx.getInput().releaseKey(Zoom.KEY);
                ctx.waitTicks(40);
                ctx.getInput().pressKey(FreeCamera.TOGGLE);
                ctx.waitTicks(5);
                float fogBack = ctx.computeOnClient(ZoomClientTest::fogEnd);
                check(!ctx.computeOnClient(mc -> FreeCamera.active()) && fogBack < 1.0E6F, "the fog is back with the camera: ends at " + fogBack);
            } finally {
                ctx.runOnClient(mc -> {
                    Zoom.KEY.setKey(InputConstants.UNKNOWN);
                    KeyMapping.resetMapping();
                    Zoom.reset();
                });
            }
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
