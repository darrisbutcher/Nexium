package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.nexium.dog.DogEntity;
import net.nexium.dog.ModDogs;

/**
 * The dog in a real game: it is drawn with its own model and skin, standing and lying down, seen from the front and the side.
 */
public class DogClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            DogEntity[] holder = new DogEntity[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var level = player.level();
                var base = player.blockPosition();
                // A flat patch of grass to stand on
                for (int x = -3; x <= 3; x++) for (int z = -2; z <= 6; z++) {
                    level.setBlockAndUpdate(base.offset(x, -1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int y = 0; y < 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                player.getInventory().clearContent();
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 12.0F);
                DogEntity dog = ModDogs.DOG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                dog.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 2.6, 180.0F, 0.0F);
                face(dog, 180.0F);
                dog.tame(player);
                dog.setNoAi(true);
                level.addFreshEntity(dog);
            });
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.player.setXRot(12.0F));
            ctx.getInput().pressKey(options -> options.keyToggleGui); // no HUD in the shots
            System.out.println("[nexus] dog stands front: " + ctx.takeScreenshot("nexus_dog_stand_front"));
            // From the side: turn the dog a quarter
            world.getServer().runOnServer(server -> {
                for (var e : server.overworld().getAllEntities()) if (e instanceof DogEntity d) { face(d, 270.0F); }
            });
            ctx.waitTicks(5);
            System.out.println("[nexus] dog stands side: " + ctx.takeScreenshot("nexus_dog_stand_side"));
            // Lying down, side then front
            world.getServer().runOnServer(server -> {
                for (var e : server.overworld().getAllEntities()) if (e instanceof DogEntity d) { d.setOrderedToSit(true); d.setInSittingPose(true); }
            });
            ctx.waitTicks(8);
            System.out.println("[nexus] dog lies side: " + ctx.takeScreenshot("nexus_dog_lie_side"));
            world.getServer().runOnServer(server -> {
                for (var e : server.overworld().getAllEntities()) if (e instanceof DogEntity d) { face(d, 180.0F); }
            });
            ctx.waitTicks(5);
            System.out.println("[nexus] dog lies front: " + ctx.takeScreenshot("nexus_dog_lie_front"));
        }
    }

    private static void face(DogEntity dog, float yaw) {
        dog.snapTo(dog.getX(), dog.getY(), dog.getZ(), yaw, 0.0F);
        dog.setYHeadRot(yaw);
        dog.setYBodyRot(yaw);
        dog.yRotO = yaw;
        dog.yBodyRotO = yaw;
        dog.yHeadRotO = yaw;
    }
}
