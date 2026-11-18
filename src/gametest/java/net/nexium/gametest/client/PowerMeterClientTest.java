package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.nexium.client.screen.PowerMeterScreen;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.EnergyNetwork;
import net.nexium.energy.CombustionGeneratorBlockEntity;
import net.nexium.energy.PowerMeter;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.registry.ModBlocks;

import java.util.List;

/**
 * The Power Meter in a real game: a generator, two conduits, two farms and a battery on a line, the meter against the first
 * conduit. Right-clicking it opens the dashboard; it lists the generator, the farms and the battery; pressing a farm's priority
 * button reaches the conduit side on the server and comes back in the next report; and the off switch stops the farm.
 */
public class PowerMeterClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                BlockPos base = player.blockPosition().offset(-1, 0, 4);
                at[0] = base;
                var level = player.level();
                level.setBlockAndUpdate(base, ModBlocks.COMBUSTION_GENERATOR.defaultBlockState());
                ((CombustionGeneratorBlockEntity) level.getBlockEntity(base)).setItem(0, new ItemStack(Items.COAL, 32));
                for (int i = 1; i <= 2; i++) {
                    BlockPos c = base.east(i);
                    level.setBlockAndUpdate(c, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                    ((ConduitBundleBlockEntity) level.getBlockEntity(c)).addConduit(ConduitType.ENERGY, 1);
                }
                level.setBlockAndUpdate(base.east(3), net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE).defaultBlockState());
                level.setBlockAndUpdate(base.east(2).above(), net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.MONSTER).defaultBlockState());
                level.setBlockAndUpdate(base.east(1).above(), ModBlocks.IRON_BATTERY.defaultBlockState());
                level.setBlockAndUpdate(base.east(1).north(), ModBlocks.POWER_METER.defaultBlockState());
                for (int i = 0; i <= 3; i++) {
                    BlockPos p = base.east(i);
                    level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), Block.UPDATE_ALL);
                }
                for (int i = 1; i <= 2; i++) ((ConduitBundleBlockEntity) level.getBlockEntity(base.east(i))).changed();
                player.snapTo(base.getX() + 1.5, base.getY(), base.getZ() - 2.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(260);
            System.out.println("[nexus] power meter block: " + ctx.takeScreenshot("nexus_power_meter_block"));

            BlockPos meter = at[0].east(1).north();
            world.getServer().runOnServer(server -> PowerMeter.open(server.getPlayerList().getPlayers().get(0), meter));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof PowerMeterScreen), "the meter opens the dashboard");
            List<EnergyNetwork.Endpoint> rows = ctx.computeOnClient(mc -> ((PowerMeterScreen) mc.gui.screen()).rows());
            check(rows.size() == 4, "four blocks on the network: generator, two farms, battery; got " + rows);
            check(rows.getFirst().role() == EnergyNetwork.Role.PRODUCER && rows.getLast().role() == EnergyNetwork.Role.BATTERY,
                    "producers first, batteries last: " + rows);
            System.out.println("[nexus] power meter dashboard: " + ctx.takeScreenshot("nexus_power_meter_screen"));

            // The first consumer's priority button: Normal -> High, reaching the conduit side that feeds it
            int consumer = 0;
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).role() == EnergyNetwork.Role.CONSUMER) { consumer = i; break; }
            final int row = consumer;
            BlockPos machine = rows.get(row).pos();
            ctx.runOnClient(mc -> ((PowerMeterScreen) mc.gui.screen()).pressPriority(row));
            ctx.waitTicks(5);
            int priority = ctx.computeOnClient(mc -> ((PowerMeterScreen) mc.gui.screen()).rows().get(row).priority());
            check(priority == 5, "the new priority comes back in the report, got " + priority);
            ctx.runOnClient(mc -> ((PowerMeterScreen) mc.gui.screen()).pressPower(row));
            ctx.waitTicks(5);
            boolean paused = ctx.computeOnClient(mc -> ((PowerMeterScreen) mc.gui.screen()).rows().get(row).paused());
            check(paused, "the off switch comes back too");
            System.out.println("[nexus] power meter controls: " + ctx.takeScreenshot("nexus_power_meter_controls"));
            // The screen also keeps refreshing by itself
            ctx.waitTicks(45);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof PowerMeterScreen), "it stays open and refreshes");
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
