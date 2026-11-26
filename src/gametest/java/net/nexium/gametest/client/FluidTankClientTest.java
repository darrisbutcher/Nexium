package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.nexium.fluid.FluidTankBlockEntity;
import net.nexium.fluid.FluidTankItem;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;

/** The three tanks with water, lava and a half full one, and a held tank scooping and pouring with real right-clicks. */
public class FluidTankClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                var level = server.overworld();
                BlockPos c = player.blockPosition().offset(-3, 0, 5);
                at[0] = c;
                var blocks = new net.minecraft.world.level.block.Block[]{ModBlocks.IRON_FLUID_TANK, ModBlocks.GOLD_FLUID_TANK, ModBlocks.DIAMOND_FLUID_TANK, ModBlocks.IRON_FLUID_TANK};
                var fluids = new net.minecraft.world.level.material.Fluid[]{Fluids.WATER, Fluids.LAVA, Fluids.WATER, Fluids.LAVA};
                double[] fill = {1.0, 0.6, 0.3, 0.9};
                for (int i = 0; i < 4; i++) {
                    level.setBlockAndUpdate(c.east(i * 2), blocks[i].defaultBlockState());
                    var be = (FluidTankBlockEntity) level.getBlockEntity(c.east(i * 2));
                    be.setContents(new FluidTankBlockEntity.Contents(FluidVariant.of(fluids[i]), (long) (be.capacity() * fill[i])));
                }
                player.snapTo(c.getX() + 3.5, c.getY(), c.getZ() - 3.5, 0.0F, 14.0F);
            });
            ctx.waitTicks(40);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] fluid tanks: " + ctx.takeScreenshot("nexus_fluid_tanks"));

            // A held, empty tank scoops a water source and pours it back, with real right-clicks
            BlockPos pond = at[0].south(4).below();
            BlockPos[] pondAt = {pond};
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                level.setBlockAndUpdate(pondAt[0], Blocks.WATER.defaultBlockState());
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), new ItemStack(ModItems.IRON_FLUID_TANK));
                player.snapTo(pondAt[0].getX() + 0.5, pondAt[0].getY() + 1.0, pondAt[0].getZ() - 1.5, 0.0F, 45.0F);
            });
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pondAt[0])));
            ctx.waitTicks(4);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(8);
            long held = world.getServer().computeOnServer(server -> {
                var p = server.getPlayerList().getPlayers().get(0);
                return FluidTankItem.contents(p.getInventory().getItem(p.getInventory().getSelectedSlot())).amount();
            });
            check(held == FluidConstants.BUCKET, "a real right-click on water scoops a bucket into the held tank, got " + held);
            check(world.getServer().computeOnServer(server -> !server.overworld().getBlockState(pondAt[0]).getFluidState().isSource()), "and the pond source is gone");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] fluid tank scooped: " + ctx.takeScreenshot("nexus_fluid_tank_scooped"));
            // ... and a right-click on the ground pours it out
            ctx.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pondAt[0].north(2))));
            ctx.waitTicks(4);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            ctx.waitTicks(8);
            long left = world.getServer().computeOnServer(server -> {
                var p = server.getPlayerList().getPlayers().get(0);
                return FluidTankItem.contents(p.getInventory().getItem(p.getInventory().getSelectedSlot())).amount();
            });
            check(left == 0, "a right-click on the ground pours the bucket out, got " + left + " left");
            check(world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                for (BlockPos p : BlockPos.betweenClosed(pondAt[0].offset(-3, -1, -4), pondAt[0].offset(3, 2, 2))) if (level.getBlockState(p).getFluidState().isSource()) return true;
                return false;
            }), "and water is back in the world");
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
