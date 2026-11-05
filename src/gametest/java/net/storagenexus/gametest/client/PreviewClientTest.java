package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.client.AiotPreview;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/**
 * Looking at a stone wall with a diamond AIOT whose 3x3 Mining module has a tunnel of 8: the blocks it would mine
 * (8 layers of 3x3, less the one looked at) are outlined, and a screenshot shows the outline. With a tunnel of 1 it
 * is the plain 3x3, and a plain pickaxe outlines nothing.
 */
public class PreviewClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                BlockPos base = player.blockPosition();
                // A stone wall starting three blocks ahead (south), 9 deep, with the player looking at its middle
                for (int x = -4; x <= 4; x++) for (int y = -1; y <= 5; y++) for (int z = 3; z <= 12; z++) {
                    player.level().setBlockAndUpdate(base.offset(x, y, z), Blocks.STONE.defaultBlockState());
                }
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
                ItemStack aiot = new ItemStack(ModItems.AIOTS.get(AiotTier.DIAMOND));
                var slots = AiotModules.slots(aiot);
                ItemStack area = new ItemStack(ModItems.MODULES.get(ModuleType.AREA_MINING));
                area.set(ModComponents.TUNNEL_LENGTH, 8);
                slots.set(0, area);
                AiotModules.setSlots(aiot, slots);
                player.setItemInHand(InteractionHand.MAIN_HAND, aiot);
            });
            ctx.waitTicks(20);
            int tunnel = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(tunnel == 8 * 9 - 1, "a tunnel of 8 outlines 71 blocks, got " + tunnel);
            System.out.println("[nexus] tunnel preview: " + ctx.takeScreenshot("nexus_preview_tunnel"));

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ItemStack aiot = player.getMainHandItem();
                var slots = AiotModules.slots(aiot);
                slots.get(0).remove(ModComponents.TUNNEL_LENGTH);
                AiotModules.setSlots(aiot, slots);
            });
            ctx.waitTicks(10);
            int plain = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(plain == 8, "a plain 3x3 outlines the 8 around the one looked at, got " + plain);
            System.out.println("[nexus] 3x3 preview: " + ctx.takeScreenshot("nexus_preview_3x3"));

            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0)
                    .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE)));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> AiotPreview.outlined().isEmpty()), "a plain pickaxe outlines nothing");
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
