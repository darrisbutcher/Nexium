package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.nexium.energy.ChargingStationBlockEntity;
import net.nexium.energy.ItemEnergy;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;

/** Battery cells hold charge in the item up to their size, and the Charging Station charges the cells in its slots from its energy. */
public class BatteryCellGameTest {
    @GameTest
    public void cellsHoldTheirSizeOfEnergyAndGiveItBack(GameTestHelper h) {
        ItemStack iron = new ItemStack(ModItems.IRON_CELL);
        h.assertValueEqual(ItemEnergy.capacity(iron), 250_000L, "an Iron Cell holds 250,000 NE");
        h.assertValueEqual(ItemEnergy.capacity(new ItemStack(ModItems.GOLD_CELL)), 1_000_000L, "a Gold Cell 1,000,000");
        h.assertValueEqual(ItemEnergy.capacity(new ItemStack(ModItems.DIAMOND_CELL)), 10_000_000L, "a Diamond Cell 10,000,000");
        h.assertValueEqual(ItemEnergy.capacity(new ItemStack(ModItems.NEXIUM_CELL)), 100_000_000L, "a Nexium Cell 100,000,000");
        h.assertValueEqual(ItemEnergy.insert(iron, 300_000, false), 250_000L, "it takes no more than its size");
        h.assertValueEqual(ItemEnergy.insert(iron, 1, false), 0L, "and nothing once full");
        h.assertValueEqual(ItemEnergy.extract(iron, 100_000, true), 100_000L, "a simulated draw changes nothing");
        h.assertValueEqual(ItemEnergy.stored(iron), 250_000L, "still full after it");
        h.assertValueEqual(ItemEnergy.extract(iron, 100_000, false), 100_000L, "a draw takes it out");
        h.assertValueEqual(ItemEnergy.extract(iron, 1_000_000, false), 150_000L, "no more than is there");
        h.assertTrue(iron.get(ModComponents.BATTERY_CHARGE) == null, "an empty cell keeps no charge component");
        h.succeed();
    }

    @GameTest
    public void aCellInsideAHolderIsChargedThroughIt(GameTestHelper h) {
        ItemStack holder = new ItemStack(net.minecraft.world.item.Items.STICK);
        ItemEnergy.setCell(holder, new ItemStack(ModItems.GOLD_CELL));
        h.assertTrue(ItemEnergy.chargeable(holder), "a holder with a cell can be charged");
        h.assertValueEqual(ItemEnergy.insert(holder, 5_000, false), 5_000L, "charge goes into the cell inside");
        h.assertValueEqual(ItemEnergy.stored(holder), 5_000L, "and shows on the holder");
        h.assertValueEqual(ItemEnergy.extract(holder, 2_000, false), 2_000L, "and comes back out of it");
        h.assertValueEqual(ItemEnergy.stored(holder), 3_000L, "leaving the rest");
        h.assertTrue(!ItemEnergy.chargeable(new ItemStack(net.minecraft.world.item.Items.STICK)), "a plain item cannot be charged");
        h.succeed();
    }

    @GameTest
    public void theChargingStationSharesItsTotalAmongTheCellsThatAreCharging(GameTestHelper h) {
        h.assertValueEqual(ChargingStationBlockEntity.TOTAL, 40_000L, "40,000 NE a tick in all");
        h.assertValueEqual(ChargingStationBlockEntity.RATE, 10_000L, "which is 10,000 a slot with all four in use");
        h.assertValueEqual(ChargingStationBlockEntity.BUFFER, 10_000_000L, "and it holds 10,000,000 NE");
        long[] expected = {40_000, 20_000, 13_333, 10_000};
        for (int cells = 1; cells <= 4; cells++) {
            BlockPos pos = new BlockPos(cells, 1, 1);
            h.setBlock(pos, ModBlocks.CHARGING_STATION);
            ChargingStationBlockEntity station = h.getBlockEntity(pos, ChargingStationBlockEntity.class);
            for (int i = 0; i < cells; i++) station.slots().setItem(i, new ItemStack(ModItems.DIAMOND_CELL));
            station.energy().set(ChargingStationBlockEntity.BUFFER);
            h.assertValueEqual(station.charging(), cells, cells + " cells are charging");
            station.charge();
            for (int i = 0; i < cells; i++) {
                h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(i)), expected[cells - 1], cells + " cell(s): each takes its share");
            }
        }
        // A full cell is not charging, so it does not take a share
        BlockPos pos = new BlockPos(5, 1, 1);
        h.setBlock(pos, ModBlocks.CHARGING_STATION);
        ChargingStationBlockEntity station = h.getBlockEntity(pos, ChargingStationBlockEntity.class);
        ItemStack full = new ItemStack(ModItems.IRON_CELL);
        ItemEnergy.insert(full, 1_000_000, false);
        station.slots().setItem(0, full);
        station.slots().setItem(1, new ItemStack(ModItems.IRON_CELL));
        station.energy().set(ChargingStationBlockEntity.BUFFER);
        h.assertValueEqual(station.charging(), 1, "a full cell does not count");
        station.charge();
        h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(1)), 40_000L, "so the other one gets all 40,000");
        h.succeed();
    }

    @GameTest
    public void theChargingStationChargesEachSlotFromWhatItHolds(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CHARGING_STATION);
        ChargingStationBlockEntity station = h.getBlockEntity(pos, ChargingStationBlockEntity.class);
        station.slots().setItem(0, new ItemStack(ModItems.IRON_CELL));
        station.slots().setItem(1, new ItemStack(ModItems.GOLD_CELL));
        station.slots().setItem(2, new ItemStack(net.minecraft.world.item.Items.STICK));
        station.energy().insert(100_000, false);
        station.charge();
        long half = ChargingStationBlockEntity.TOTAL / 2;
        h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(0)), half, "with two cells charging, each takes half the total in a tick");
        h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(1)), half, "so does the second");
        h.assertValueEqual(station.energy().stored(), 100_000L - 2 * half, "and the station's energy pays for both");
        station.energy().set(500);
        station.charge();
        h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(0)), half + 500, "with little left, the first slot takes it all");
        h.assertValueEqual(ItemEnergy.stored(station.slots().getItem(1)), half, "and the second gets nothing more");
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, java.util.List<ItemStack> items) {
        java.util.List<ItemStack> grid = new java.util.ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        net.minecraft.world.item.crafting.CraftingInput input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void upgradingACellOrABatteryKeepsItsCharge(GameTestHelper h) {
        ItemStack redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE_BLOCK), diamond = new ItemStack(net.minecraft.world.item.Items.DIAMOND),
                nexium = new ItemStack(ModItems.NEXIUM_INGOT), empty = ItemStack.EMPTY;
        // Gold Cell to Diamond Cell: the pattern is  D / DCD / DRD
        ItemStack gold = new ItemStack(ModItems.GOLD_CELL);
        gold.set(net.nexium.registry.ModComponents.BATTERY_CHARGE, 750_000L);
        ItemStack diamondCell = craft(h, java.util.List.of(empty, diamond, empty, diamond, gold, diamond, diamond, redstone, diamond));
        h.assertTrue(diamondCell.is(ModItems.DIAMOND_CELL), "a Gold Cell, diamonds and a redstone block make a Diamond Cell");
        h.assertValueEqual(ItemEnergy.stored(diamondCell), 750_000L, "and the Diamond Cell keeps the 750,000 NE the Gold Cell held");
        // Diamond Cell to Nexium Cell, past what a cell took in before
        diamondCell.set(net.nexium.registry.ModComponents.BATTERY_CHARGE, 9_000_000L);
        ItemStack nexiumCell = craft(h, java.util.List.of(empty, nexium, empty, nexium, diamondCell, nexium, nexium, redstone, nexium));
        h.assertTrue(nexiumCell.is(ModItems.NEXIUM_CELL), "a Diamond Cell, Nexium ingots and a redstone block make a Nexium Cell");
        h.assertValueEqual(ItemEnergy.stored(nexiumCell), 9_000_000L, "with its 9,000,000 NE");
        // An empty one gives an empty one
        ItemStack fresh = craft(h, java.util.List.of(empty, diamond, empty, diamond, new ItemStack(ModItems.GOLD_CELL), diamond, diamond, redstone, diamond));
        h.assertValueEqual(ItemEnergy.stored(fresh), 0L, "an empty Gold Cell gives an empty Diamond Cell");
        // Batteries: Gold to Diamond  DRD / RBR / DRD
        ItemStack goldBattery = new ItemStack(ModBlocks.GOLD_BATTERY);
        goldBattery.set(net.nexium.registry.ModComponents.BATTERY_CHARGE, 900_000L);
        ItemStack diamondBattery = craft(h, java.util.List.of(diamond, redstone, diamond, redstone, goldBattery, redstone, diamond, redstone, diamond));
        h.assertTrue(diamondBattery.is(ModBlocks.DIAMOND_BATTERY.asItem()), "a Gold Battery makes a Diamond Battery");
        h.assertValueEqual(diamondBattery.get(net.nexium.registry.ModComponents.BATTERY_CHARGE), 900_000L, "keeping its 900,000 NE");
        h.succeed();
    }
}
