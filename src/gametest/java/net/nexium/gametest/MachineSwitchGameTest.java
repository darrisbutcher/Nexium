package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nexium.energy.NexusFurnaceBlockEntity;
import net.nexium.energy.NexusFurnaceMenu;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.essentials.farm.FarmMenu;
import net.nexium.essentials.ironfarm.IronFarmBlockEntity;
import net.nexium.essentials.ironfarm.IronFarmMenu;
import net.nexium.essentials.registry.ModComponents;
import net.nexium.essentials.xpfarm.XpFarmBlockEntity;
import net.nexium.essentials.xpfarm.XpFarmMenu;
import net.nexium.freezer.FreezerBlockEntity;
import net.nexium.freezer.FreezerMenu;

/** The On/Off button of every powered machine (farms, Iron Farm, Experience Farm, Freezer, Nexus Furnace): off, it does nothing and uses no power, and the switch is kept in the item. */
public class MachineSwitchGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    @GameTest
    public void aFarmThatIsOffDoesNoWorkAndUsesNoPower(GameTestHelper h) {
        h.setBlock(POS, net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        FarmBlockEntity f = h.getBlockEntity(POS, FarmBlockEntity.class);
        f.energy().set(f.energy().capacity());
        f.setOff(true);
        long before = f.energy().stored();
        for (int i = 0; i < 20; i++) FarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        h.assertValueEqual(f.progress(), 0, "no progress while it is off");
        h.assertValueEqual(f.energy().stored(), before, "and no energy used");
        f.setOff(false);
        for (int i = 0; i < 20; i++) FarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        h.assertTrue(f.progress() > 0 && f.energy().stored() < before, "switched on, it works and draws power");
        h.succeed();
    }

    @GameTest
    public void theExperienceFarmFreezerAndFurnaceStopWhenOff(GameTestHelper h) {
        BlockPos a = POS, b = POS.east(2), c = POS.south(2);
        h.setBlock(a, net.nexium.essentials.registry.ModBlocks.XP_FARM);
        XpFarmBlockEntity xp = h.getBlockEntity(a, XpFarmBlockEntity.class);
        xp.energy().set(xp.energy().capacity());
        xp.setOff(true);
        XpFarmBlockEntity.serverTick(h.getLevel(), xp.getBlockPos(), xp.getBlockState(), xp);
        h.assertValueEqual(xp.experience(), 0L, "no experience while it is off");
        xp.setOff(false);
        XpFarmBlockEntity.serverTick(h.getLevel(), xp.getBlockPos(), xp.getBlockState(), xp);
        h.assertTrue(xp.experience() > 0, "and some once it is on");

        h.setBlock(b, net.nexium.registry.ModBlocks.FREEZER);
        FreezerBlockEntity freezer = h.getBlockEntity(b, FreezerBlockEntity.class);
        freezer.setWater(FreezerBlockEntity.CAPACITY);
        freezer.energy().set(freezer.energy().capacity());
        freezer.setOff(true);
        long frozen = freezer.energy().stored();
        for (int i = 0; i < 10; i++) FreezerBlockEntity.serverTick(h.getLevel(), freezer.getBlockPos(), freezer.getBlockState(), freezer);
        h.assertValueEqual(freezer.energy().stored(), frozen, "the Freezer uses no energy while it is off");
        freezer.setOff(false);
        for (int i = 0; i < 10; i++) FreezerBlockEntity.serverTick(h.getLevel(), freezer.getBlockPos(), freezer.getBlockState(), freezer);
        h.assertTrue(freezer.energy().stored() < frozen, "and uses some once it is on");

        h.setBlock(c, net.nexium.registry.ModBlocks.NEXUS_FURNACE);
        NexusFurnaceBlockEntity furnace = h.getBlockEntity(c, NexusFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.IRON_ORE, 4));
        furnace.energy().set(furnace.energy().capacity());
        furnace.setOff(true);
        long burnt = furnace.energy().stored();
        for (int i = 0; i < 20; i++) NexusFurnaceBlockEntity.serverTick(h.getLevel(), furnace.getBlockPos(), furnace.getBlockState(), furnace);
        h.assertValueEqual(furnace.energy().stored(), burnt, "the Nexus Furnace uses no energy while it is off");
        furnace.setOff(false);
        for (int i = 0; i < 20; i++) NexusFurnaceBlockEntity.serverTick(h.getLevel(), furnace.getBlockPos(), furnace.getBlockState(), furnace);
        h.assertTrue(furnace.energy().stored() < burnt, "and cooks once it is on");
        h.succeed();
    }

    @GameTest
    public void theChargingStationStopsChargingWhenOff(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, net.nexium.registry.ModBlocks.CHARGING_STATION);
        var station = h.getBlockEntity(at, net.nexium.energy.ChargingStationBlockEntity.class);
        station.slots().setItem(0, new ItemStack(net.nexium.registry.ModItems.IRON_CELL));
        station.energy().set(1_000_000);
        var menu = new net.nexium.energy.ChargingStationMenu(1, player.getInventory(), station);
        h.assertTrue(menu.clickMenuButton(player, net.nexium.energy.ChargingStationMenu.BTN_SWITCH) && station.isOff() && menu.off(), "the button switches the station off");
        long before = station.energy().stored();
        for (int i = 0; i < 10; i++) net.nexium.energy.ChargingStationBlockEntity.serverTick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertValueEqual(station.energy().stored(), before, "no energy used while it is off");
        h.assertValueEqual(net.nexium.energy.ItemEnergy.stored(station.slots().getItem(0)), 0L, "and the cell stays empty");
        // The switch is kept in the dropped item
        h.assertTrue(station.collectComponents().get(ModComponents.MACHINE_OFF) != null, "an off station keeps its switch in the item");
        menu.clickMenuButton(player, net.nexium.energy.ChargingStationMenu.BTN_SWITCH);
        h.assertTrue(!station.isOff() && !menu.off(), "and on again");
        for (int i = 0; i < 10; i++) net.nexium.energy.ChargingStationBlockEntity.serverTick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        h.assertTrue(net.nexium.energy.ItemEnergy.stored(station.slots().getItem(0)) > 0, "it charges the cell once it is on");
        h.succeed();
    }

    @GameTest
    public void everyMenuHasTheSwitchButton(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.setBlock(POS, net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        FarmBlockEntity farm = h.getBlockEntity(POS, FarmBlockEntity.class);
        FarmMenu farmMenu = new FarmMenu(1, player.getInventory(), farm);
        h.assertTrue(farmMenu.clickMenuButton(player, FarmMenu.BTN_SWITCH) && farm.isOff() && farmMenu.off(), "the farm's button switches it off");
        farmMenu.clickMenuButton(player, FarmMenu.BTN_SWITCH);
        h.assertTrue(!farm.isOff(), "and on again");

        BlockPos iron = POS.east(2);
        h.setBlock(iron, net.nexium.essentials.registry.ModBlocks.IRON_FARM);
        IronFarmBlockEntity ironFarm = h.getBlockEntity(iron, IronFarmBlockEntity.class);
        IronFarmMenu ironMenu = new IronFarmMenu(1, player.getInventory(), ironFarm);
        h.assertTrue(ironMenu.clickMenuButton(player, IronFarmMenu.BTN_SWITCH) && ironFarm.isOff() && ironMenu.off(), "the Iron Farm's");

        BlockPos xpPos = POS.south(2);
        h.setBlock(xpPos, net.nexium.essentials.registry.ModBlocks.XP_FARM);
        XpFarmBlockEntity xp = h.getBlockEntity(xpPos, XpFarmBlockEntity.class);
        XpFarmMenu xpMenu = new XpFarmMenu(1, player.getInventory(), xp);
        h.assertTrue(xpMenu.clickMenuButton(player, XpFarmMenu.BTN_SWITCH) && xp.isOff() && xpMenu.off(), "the Experience Farm's");

        BlockPos fz = POS.east(2).south(2);
        h.setBlock(fz, net.nexium.registry.ModBlocks.FREEZER);
        FreezerBlockEntity freezer = h.getBlockEntity(fz, FreezerBlockEntity.class);
        FreezerMenu freezerMenu = new FreezerMenu(1, player.getInventory(), freezer);
        h.assertTrue(freezerMenu.clickMenuButton(player, FreezerMenu.BTN_SWITCH) && freezer.isOff() && freezerMenu.off(), "the Freezer's");

        BlockPos nf = POS.east(4);
        h.setBlock(nf, net.nexium.registry.ModBlocks.NEXUS_FURNACE);
        NexusFurnaceBlockEntity furnace = h.getBlockEntity(nf, NexusFurnaceBlockEntity.class);
        NexusFurnaceMenu furnaceMenu = new NexusFurnaceMenu(1, player.getInventory(), furnace);
        h.assertTrue(furnaceMenu.clickMenuButton(player, NexusFurnaceMenu.BTN_SWITCH) && furnace.isOff() && furnaceMenu.off(), "and the Nexus Furnace's");
        h.succeed();
    }

    /** The switch goes into the dropped item and comes back out of it. */
    @GameTest
    public void theSwitchIsKeptInTheDroppedItem(GameTestHelper h) {
        BlockPos[] spots = {POS, POS.east(2), POS.south(2), POS.east(2).south(2), POS.east(4)};
        net.minecraft.world.level.block.Block[] blocks = {net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.BAMBOO), net.nexium.essentials.registry.ModBlocks.IRON_FARM, net.nexium.essentials.registry.ModBlocks.XP_FARM,
                net.nexium.registry.ModBlocks.FREEZER, net.nexium.registry.ModBlocks.NEXUS_FURNACE};
        for (int i = 0; i < spots.length; i++) {
            h.setBlock(spots[i], blocks[i]);
            BlockEntity be = h.getBlockEntity(spots[i], BlockEntity.class);
            switchOff(be);
            var map = be.collectComponents();
            h.assertTrue(map.has(ModComponents.MACHINE_OFF), blocks[i] + ": the dropped item says it is off");
            BlockPos other = spots[i].north();
            h.setBlock(other, blocks[i]);
            BlockEntity again = h.getBlockEntity(other, BlockEntity.class);
            again.applyComponents(map, DataComponentPatch.EMPTY);
            h.assertTrue(isOff(again), blocks[i] + ": and placed again it is still off");
        }
        h.succeed();
    }

    private static void switchOff(BlockEntity be) {
        if (be instanceof FarmBlockEntity f) f.setOff(true);
        else if (be instanceof IronFarmBlockEntity f) f.setOff(true);
        else if (be instanceof XpFarmBlockEntity f) f.setOff(true);
        else if (be instanceof FreezerBlockEntity f) f.setOff(true);
        else if (be instanceof NexusFurnaceBlockEntity f) f.setOff(true);
        else throw new AssertionError("not a switchable machine: " + be);
    }

    private static boolean isOff(BlockEntity be) {
        if (be instanceof FarmBlockEntity f) return f.isOff();
        if (be instanceof IronFarmBlockEntity f) return f.isOff();
        if (be instanceof XpFarmBlockEntity f) return f.isOff();
        if (be instanceof FreezerBlockEntity f) return f.isOff();
        return ((NexusFurnaceBlockEntity) be).isOff();
    }
}
