package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.nexium.client.screen.CellScreen;
import net.nexium.client.screen.ChargingStationScreen;
import net.nexium.energy.CellMenu;
import net.nexium.energy.CellSource;
import net.nexium.energy.ChargingStationBlockEntity;
import net.nexium.energy.ItemEnergy;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.util.Menus;

/**
 * The Jetpack in a real game: worn on the back, a real jump in the air takes off and the charge goes down, the cell screen, and the
 * Charging Station's screen with cells charging. Takes screenshots.
 */
public class JetpackClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            double[] startY = new double[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                ItemStack jetpack = new ItemStack(ModItems.JETPACK);
                ItemStack cell = new ItemStack(ModItems.GOLD_CELL);
                cell.set(ModComponents.BATTERY_CHARGE, 600_000L);
                ItemEnergy.setCell(jetpack, cell);
                ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpack);
                // A few blocks up in the open, so the jump is in the air
                player.snapTo(player.getX(), player.getY() + 6, player.getZ(), 0.0F, 10.0F);
                startY[0] = player.getY();
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] jetpack worn, front: " + ctx.takeScreenshot("nexus_jetpack_front"));
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            ctx.waitTicks(5);
            System.out.println("[nexus] jetpack worn, back: " + ctx.takeScreenshot("nexus_jetpack_back"));

            // A real jump key held in the air: it takes off, rises, and the cell pays for it
            ctx.getInput().holdKey(options -> options.keyJump);
            ctx.waitTicks(30);
            boolean flying = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getAbilities().flying);
            double y = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getY());
            long charge = world.getServer().computeOnServer(server -> ItemEnergy.stored(ExtraSlots.of(server.getPlayerList().getPlayers().get(0)).wings()));
            System.out.println("[nexus] jetpack flight: " + ctx.takeScreenshot("nexus_jetpack_flying"));
            ctx.getInput().releaseKey(options -> options.keyJump);
            check(flying, "holding jump in the air takes off");
            check(y > startY[0] + 2, "it rises (from " + startY[0] + " to " + y + ")");
            check(charge < 600_000L && charge > 598_000L, "the cell paid about 8 NE a tick: " + charge);
            // Hover on: letting go of jump hovers. Switch hover off: with jump let go, the flight ends and you drop
            ctx.waitTicks(5);
            check(world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getAbilities().flying), "with hover on, letting go of jump keeps hovering");
            world.getServer().runOnServer(server -> net.nexium.jetpack.JetpackFlight.toggle(server.getPlayerList().getPlayers().get(0), net.nexium.jetpack.JetpackFlight.HOVER_SWITCH));
            ctx.waitTicks(10);
            check(!world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getAbilities().flying), "with hover off, letting go of jump ends the flight");
            double dropFrom = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getY());
            ctx.waitTicks(10);
            double droppedTo = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getY());
            check(droppedTo < dropFrom - 0.5, "and the player drops (from " + dropFrom + " to " + droppedTo + ")");
            world.getServer().runOnServer(server -> net.nexium.jetpack.JetpackFlight.toggle(server.getPlayerList().getPlayers().get(0), net.nexium.jetpack.JetpackFlight.HOVER_SWITCH));
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

            // The cell screen
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                Menus.open(player, Component.literal("Jetpack"), CellSource.WINGS_SLOT, (id, inv, p) -> new CellMenu(id, inv, CellSource.WINGS_SLOT));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof CellScreen);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] jetpack cell screen: " + ctx.takeScreenshot("nexus_jetpack_cell_screen"));
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);

            // The Charging Station
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                at[0] = player.blockPosition().offset(2, 0, 2);
                var level = server.overworld();
                level.setBlockAndUpdate(at[0], ModBlocks.CHARGING_STATION.defaultBlockState());
                var station = (ChargingStationBlockEntity) level.getBlockEntity(at[0]);
                station.slots().setItem(0, new ItemStack(ModItems.IRON_CELL));
                ItemStack gold = new ItemStack(ModItems.GOLD_CELL);
                gold.set(ModComponents.BATTERY_CHARGE, 400_000L);
                station.slots().setItem(1, gold);
                ItemStack holder = new ItemStack(ModItems.JETPACK);
                ItemEnergy.setCell(holder, new ItemStack(ModItems.DIAMOND_CELL));
                station.slots().setItem(2, holder);
                station.energy().set(20_000);
                Menus.open(player, Component.literal("Charging Station"), at[0], (id, inv, p) -> new net.nexium.energy.ChargingStationMenu(id, inv, station));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof ChargingStationScreen);
            ctx.waitTicks(15);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] charging station screen: " + ctx.takeScreenshot("nexus_charging_station_screen"));
            long ironAfter = world.getServer().computeOnServer(server ->
                    ItemEnergy.stored(((ChargingStationBlockEntity) server.overworld().getBlockEntity(at[0])).slots().getItem(0)));
            check(ironAfter > 0, "the Charging Station charged the Iron Cell: " + ironAfter);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
