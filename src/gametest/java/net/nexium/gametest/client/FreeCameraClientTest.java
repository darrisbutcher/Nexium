package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.client.FreeCamera;

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

            // Cave culling: the game draws only the render sections it can reach from the camera through open space, so from below the
            // world (or from inside rock) a cave, or any room, behind solid ground is left undrawn, and which ones are drawn changes as
            // the camera turns. The free camera turns that off. A single stone block floats high above the ground; a camera 90 blocks
            // below it, looking straight up, is blocked by the ground slab: the game's own camera does not draw it, the free camera does.
            int normalDistance = ctx.computeOnClient(mc -> mc.options.renderDistance().get());
            ctx.runOnClient(mc -> mc.options.renderDistance().set(16));
            // The server only sends chunks as far as its own view distance, so it is raised too
            int[] serverDistance = new int[1];
            world.getServer().runOnServer(server -> {
                serverDistance[0] = server.getPlayerList().getViewDistance();
                server.getPlayerList().setViewDistance(16);
            });
            ctx.waitTicks(100);
            world.getServer().runCommand("gamemode creative @a");
            int[] origin = new int[2];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                origin[0] = ((base[0].getX() >> 4) + 2) * 16;
                origin[1] = (base[0].getZ() >> 4) * 16;
                player.level().setBlock(new BlockPos(origin[0] + 8, -40, origin[1] + 8), Blocks.STONE.defaultBlockState(), 2);
            });
            ctx.waitTicks(100);
            BlockPos room = new BlockPos(origin[0], -48, origin[1]);
            // Without the free camera: a camera put there by hand does not draw the room
            ctx.runOnClient(mc -> {
                net.minecraft.world.entity.Marker m = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER, mc.level);
                m.noPhysics = true;
                m.setPos(origin[0] + 8.5, -130, origin[1] + 8.5);
                m.setYRot(0);
                m.setXRot(-90);
                m.setOldPosAndRot();
                mc.setCameraEntity(m);
            });
            ctx.waitTicks(60);
            check(!ctx.computeOnClient(mc -> mc.levelRenderer.visibleSections().stream().anyMatch(sec -> sec.getRenderOrigin().equals(room))),
                    "from below the world the game's own camera does not draw a room behind the ground (cave culling)");
            ctx.runOnClient(mc -> mc.setCameraEntity(mc.player));
            ctx.waitTicks(5);
            // The free camera, flown to the same place, draws it
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> FreeCamera.active()), "the free camera is on again");
            ctx.runOnClient(mc -> {
                var cam = FreeCamera.camera();
                cam.setPos(origin[0] + 8.5, -130, origin[1] + 8.5);
                cam.setYRot(0);
                cam.setXRot(-90);
                cam.setOldPosAndRot();
            });
            ctx.waitTicks(60);
            check(ctx.computeOnClient(mc -> mc.levelRenderer.visibleSections().stream().anyMatch(sec -> sec.getRenderOrigin().equals(room))),
                    "from the same place the free camera draws it: no cave culling");
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> !FreeCamera.active() && mc.getCameraEntity() == mc.player), "and it goes off again");

            // The same from inside rock, which is where a free camera usually ends up: the render section the camera is in and the six
            // next to it are solid stone, and a single block floats 88 blocks ahead in the open. The game's own camera boxed in like
            // that does not draw the far block; the free camera does.
            int[] rock = new int[2];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                rock[0] = ((base[0].getX() >> 4) - 3) * 16;
                rock[1] = (base[0].getZ() >> 4) * 16;
                int y0 = -32;
                int[][] sections = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
                for (int[] s : sections) {
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 16; y++) {
                        player.level().setBlock(new BlockPos(rock[0] + s[0] * 16 + x, y0 + s[1] * 16 + y, rock[1] + s[2] * 16 + z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
                player.level().setBlock(new BlockPos(rock[0] + 8, y0 + 8, rock[1] + 88), Blocks.STONE.defaultBlockState(), 2);
            });
            ctx.waitTicks(100);
            BlockPos far = new BlockPos(rock[0], -32, rock[1] + 80);
            ctx.runOnClient(mc -> {
                net.minecraft.world.entity.Marker m = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER, mc.level);
                m.noPhysics = true;
                m.setPos(rock[0] + 8.5, -32 + 8.5, rock[1] + 8.5);
                m.setYRot(0);
                m.setXRot(0);
                m.setOldPosAndRot();
                mc.setCameraEntity(m);
            });
            ctx.waitTicks(60);
            check(!ctx.computeOnClient(mc -> mc.levelRenderer.visibleSections().stream().anyMatch(sec -> sec.getRenderOrigin().equals(far))),
                    "boxed in by rock the game's own camera does not draw a block far ahead (cave culling)");
            ctx.runOnClient(mc -> mc.setCameraEntity(mc.player));
            ctx.waitTicks(5);
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                var cam = FreeCamera.camera();
                cam.setPos(rock[0] + 8.5, -32 + 8.5, rock[1] + 8.5);
                cam.setYRot(0);
                cam.setXRot(0);
                cam.setOldPosAndRot();
            });
            ctx.waitTicks(60);
            check(ctx.computeOnClient(mc -> mc.levelRenderer.visibleSections().stream().anyMatch(sec -> sec.getRenderOrigin().equals(far))),
                    "the free camera in the same rock draws it: no cave culling");
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> !FreeCamera.active() && mc.getCameraEntity() == mc.player), "and it goes off again");
            ctx.runOnClient(mc -> mc.options.renderDistance().set(normalDistance));
            world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(serverDistance[0]));

            // Night vision: in a sealed room with no light the player sees next to nothing; the free camera, from the same place, sees the
            // room as if it were day. Compared by how bright the screenshots are (the interface is hidden for both).
            world.getServer().runCommand("time set midnight");
            int[] lab = new int[3];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                lab[0] = base[0].getX() + 40;
                lab[1] = base[0].getY() + 12;
                lab[2] = base[0].getZ();
                // Stone walls, floor and roof around a 7 x 4 x 7 room with nothing in it, and a few bright blocks on the far wall
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 4; y++) {
                    boolean inside = Math.abs(x) <= 3 && Math.abs(z) <= 3 && y >= 0 && y <= 3;
                    player.level().setBlock(new BlockPos(lab[0] + x, lab[1] + y, lab[2] + z),
                            inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
                for (int x = -2; x <= 2; x++) {
                    var block = x % 2 == 0 ? Blocks.GOLD_BLOCK : Blocks.EMERALD_BLOCK;
                    player.level().setBlock(new BlockPos(lab[0] + x, lab[1] + 1, lab[2] + 3), block.defaultBlockState(), 2);
                }
            });
            // A command, which moves the player on the client too (setting the server's copy alone leaves the player where they were)
            world.getServer().runCommand("tp @p " + (lab[0] + 0.5) + " " + lab[1] + " " + (lab[2] - 2.5) + " 0 5");
            ctx.waitTicks(40);
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(10);
            var dark = ctx.takeScreenshot("nexus_free_camera_dark_room");
            double darkLevel = brightness(dark);
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(20);
            check(ctx.computeOnClient(mc -> FreeCamera.active()), "the free camera is on in the dark room");
            var lit = ctx.takeScreenshot("nexus_free_camera_night_vision");
            double litLevel = brightness(lit);
            System.out.println("[nexus] free camera night vision: brightness " + darkLevel + " without, " + litLevel + " with");
            check(litLevel > darkLevel + 0.08, "the free camera sees a dark room far brighter than the player does: " + litLevel + " against " + darkLevel);
            ctx.getInput().pressKey(FreeCamera.TOGGLE);
            ctx.waitTicks(20);
            check(ctx.computeOnClient(mc -> !FreeCamera.active()), "and it goes off again");
            double after = brightness(ctx.takeScreenshot("nexus_free_camera_dark_room_again"));
            check(after < litLevel - 0.08, "and the room is dark again for the player: " + after + " against " + litLevel);
            ctx.getInput().pressKey(options -> options.keyToggleGui);
        }
    }

    /** The average brightness of a screenshot, 0 (black) to 1 (white). */
    private static double brightness(java.nio.file.Path file) {
        try {
            var image = javax.imageio.ImageIO.read(file.toFile());
            double sum = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                sum += (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
            }
            return sum / ((double) image.getWidth() * image.getHeight());
        } catch (java.io.IOException e) {
            throw new AssertionError("could not read the screenshot " + file, e);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
