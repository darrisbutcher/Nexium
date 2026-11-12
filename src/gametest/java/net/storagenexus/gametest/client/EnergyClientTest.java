package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.storagenexus.client.screen.CombustionGeneratorScreen;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.energy.CombustionGeneratorBlockEntity;
import net.storagenexus.essentials.client.FarmScreen;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.registry.ModBlocks;

/**
 * Nexus Energy in a real game: a Combustion Generator burning coal, two Energy Conduits and a Sugar Cane Farm that runs
 * on it; the generator's screen; and an unpowered farm's screen saying what it needs.
 */
public class EnergyClientTest implements FabricClientGameTest {
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
                BlockPos base = player.blockPosition().offset(-1, 0, 3);
                at[0] = base;
                var level = player.level();
                level.setBlockAndUpdate(base, ModBlocks.COMBUSTION_GENERATOR.defaultBlockState());
                ((CombustionGeneratorBlockEntity) level.getBlockEntity(base)).setItem(0, new ItemStack(Items.COAL, 16));
                for (int i = 1; i <= 2; i++) {
                    BlockPos c = base.east(i);
                    level.setBlockAndUpdate(c, ModBlocks.CONDUIT_BUNDLE.defaultBlockState());
                    ((ConduitBundleBlockEntity) level.getBlockEntity(c)).addConduit(ConduitType.ENERGY, i);
                }
                level.setBlockAndUpdate(base.east(3), net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE).defaultBlockState());
                // A farm with no power, off to the side
                level.setBlockAndUpdate(base.offset(1, 0, 3), net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.MONSTER).defaultBlockState());
                for (int i = 0; i <= 3; i++) {
                    BlockPos p = base.east(i);
                    level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), Block.UPDATE_ALL);
                }
                for (int i = 1; i <= 2; i++) ((ConduitBundleBlockEntity) level.getBlockEntity(base.east(i))).changed();
                player.snapTo(base.getX() + 2.0, base.getY(), base.getZ() - 2.5, 0.0F, 20.0F);
            });
            // Stage 2 blocks in a row beside the line: Solar Panel, Thermal Generator, the three Batteries
            world.getServer().runOnServer(server -> {
                var level = server.getPlayerList().getPlayers().get(0).level();
                BlockPos row = at[0].offset(-1, 0, 5);
                level.setBlockAndUpdate(row, ModBlocks.SOLAR_PANEL.defaultBlockState());
                level.setBlockAndUpdate(row.east(2), ModBlocks.THERMAL_GENERATOR.defaultBlockState());
                var thermal = (net.storagenexus.energy.ThermalGeneratorBlockEntity) level.getBlockEntity(row.east(2));
                thermal.tank().variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA);
                thermal.tank().amount = 3 * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
                net.storagenexus.energy.BatteryBlock[] batteries = {ModBlocks.IRON_BATTERY, ModBlocks.GOLD_BATTERY, ModBlocks.DIAMOND_BATTERY};
                double[] fill = {0.3, 0.6, 0.95};
                for (int i = 0; i < 3; i++) {
                    BlockPos p = row.east(4 + i * 2);
                    level.setBlockAndUpdate(p, batteries[i].defaultBlockState());
                    var battery = (net.storagenexus.energy.BatteryBlockEntity) level.getBlockEntity(p);
                    battery.energy().set((long) (battery.energy().capacity() * fill[i]));
                }
            });
            ctx.waitTicks(60);
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(20.0F);
            });
            long farmProgress = world.getServer().computeOnServer(server ->
                    (long) ((FarmBlockEntity) server.overworld().getBlockEntity(at[0].east(3))).progress());
            check(farmProgress > 0, "the farm runs on the generator through the conduits, progress " + farmProgress);
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(10);
            System.out.println("[nexus] energy line: " + ctx.takeScreenshot("nexus_energy_line"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            // The stage 2 row, from the front
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos row = at[0].offset(-1, 0, 5);
                player.teleportTo(player.level(), row.getX() + 5.0, row.getY(), row.getZ() - 4.0, java.util.Set.of(), 0.0F, 15.0F, false);
            });
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> {
                mc.player.setYRot(0.0F);
                mc.player.setXRot(15.0F);
            });
            ctx.getInput().pressKey(options -> options.keyToggleGui);
            ctx.waitTicks(10);
            System.out.println("[nexus] power row: " + ctx.takeScreenshot("nexus_power_row"));
            ctx.getInput().pressKey(options -> options.keyToggleGui);

            for (int which = 0; which < 2; which++) {
                final int w = which;
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    BlockPos row = at[0].offset(-1, 0, 5);
                    player.teleportTo(player.level(), row.getX() + 3.0, row.getY(), row.getZ() - 1.5, java.util.Set.of(), 0.0F, 15.0F, false);
                });
                ctx.waitTicks(20); // a teleport closes any screen that opens in the same moment
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    BlockPos row = at[0].offset(-1, 0, 5);
                    var be = player.level().getBlockEntity(w == 0 ? row.east(2) : row.east(8));
                    player.openMenu((net.minecraft.world.MenuProvider) be);
                });
                ctx.waitTicks(10);
                String shown = ctx.computeOnClient(mc -> String.valueOf(mc.gui.screen()));
                System.out.println("[nexus] screen for " + (w == 0 ? "thermal" : "battery") + ": " + shown);
                check(shown.contains(w == 0 ? "ThermalGeneratorScreen" : "BatteryScreen"), "the " + (w == 0 ? "Thermal Generator" : "Battery") + " screen opens, got " + shown);
                if (w == 1) {
                    // The numbers are bigger than a 16-bit screen value holds: they must arrive whole
                    int[] numbers = ctx.computeOnClient(mc -> {
                        var menu = ((net.storagenexus.client.screen.BatteryScreen) mc.gui.screen()).getMenu();
                        return new int[]{menu.stored(), menu.capacity()};
                    });
                    check(numbers[0] == 9_500_000 && numbers[1] == 10_000_000, "the Battery screen shows 9,500,000 of 10,000,000, got " + numbers[0] + " of " + numbers[1]);
                }
                ctx.runOnClient(mc -> mc.gui.toastManager().clear());
                ctx.getInput().setCursorPos(0, 0);
                ctx.waitTicks(3);
                System.out.println("[nexus] " + (w == 0 ? "thermal screen: " : "battery screen: ") + ctx.takeScreenshot(w == 0 ? "nexus_thermal_screen" : "nexus_battery_screen"));
                ctx.runOnClient(mc -> mc.player.closeContainer());
                ctx.waitTicks(5);
            }

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.openMenu((CombustionGeneratorBlockEntity) player.level().getBlockEntity(at[0]));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof CombustionGeneratorScreen), "the generator's screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] generator screen: " + ctx.takeScreenshot("nexus_generator_screen"));
            ctx.runOnClient(mc -> mc.player.closeContainer());
            ctx.waitTicks(5);

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var level = player.level();
                BlockPos spot = at[0].offset(-1, 0, 8);
                level.setBlockAndUpdate(spot, ModBlocks.REDSTONE_REACTOR.defaultBlockState());
                var reactor = (net.storagenexus.energy.RedstoneReactorBlockEntity) level.getBlockEntity(spot);
                for (int i = 0; i < 6; i++) reactor.setItem(i, new net.minecraft.world.item.ItemStack(net.storagenexus.registry.ModItems.REDSTONE_FUEL_ROD));
                reactor.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_ICE, 32));
                reactor.setItem(10, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PACKED_ICE, 16));
                reactor.setHeat(0.7);
                reactor.energy().set(61_000);
                player.openMenu(reactor);
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof net.storagenexus.client.screen.RedstoneReactorScreen), "the reactor's screen opens");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] reactor screen: " + ctx.takeScreenshot("nexus_reactor_screen"));
            // The Insert button takes the rods one step deeper
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, net.storagenexus.energy.RedstoneReactorMenu.BTN_INSERT));
            ctx.waitTicks(5);
            int depth = world.getServer().computeOnServer(server -> ((net.storagenexus.energy.RedstoneReactorBlockEntity)
                    server.overworld().getBlockEntity(at[0].offset(-1, 0, 8))).depth());
            check(depth == 3, "Insert moves the rods to Inserted 1, got " + depth);
            ctx.waitTicks(3);
            System.out.println("[nexus] reactor screen deeper: " + ctx.takeScreenshot("nexus_reactor_screen_inserted"));
            ctx.runOnClient(mc -> mc.player.closeContainer());
            ctx.waitTicks(5);

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.openMenu((FarmBlockEntity) player.level().getBlockEntity(at[0].offset(1, 0, 3)));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmScreen), "the unpowered farm's screen opens");
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] unpowered farm: " + ctx.takeScreenshot("nexus_unpowered_farm"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
