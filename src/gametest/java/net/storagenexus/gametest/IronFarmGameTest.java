package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmMenu;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.ironfarm.IronFarmTimers;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IronFarmGameTest {
    private static final BlockPos FARM = new BlockPos(4, 2, 4);

    // ---------------------------------------------------------------- helpers






    private static IronFarmBlockEntity farm(GameTestHelper h) {
        h.setBlock(FARM, ModBlocks.IRON_FARM);
        return h.getBlockEntity(FARM, IronFarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, IronFarmBlockEntity farm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            farm.energy().set(Long.MAX_VALUE); // always powered
            IronFarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
        }
    }

    private static int count(IronFarmBlockEntity farm, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) if (farm.getItem(i).is(item)) n += farm.getItem(i).getCount();
        return n;
    }

    private static int perGolem() {
        return NexusConfig.get().ironFarmGolemTicks;
    }

    // ---------------------------------------------------------------- the farm

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void farmMakesIronAtTheSetRate(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        tick(h, farm, perGolem() - 1);
        h.assertValueEqual(count(farm, Items.IRON_INGOT), 0, "no golem yet");
        tick(h, farm, 1);
        int iron = count(farm, Items.IRON_INGOT);
        h.assertTrue(iron >= 3 && iron <= 5, "one golem drops 3-5 iron, got " + iron);
        h.assertValueEqual(count(farm, Items.POPPY), 0, "and no poppies");
        tick(h, farm, perGolem() * 9);
        int tenGolems = count(farm, Items.IRON_INGOT);
        h.assertTrue(tenGolems >= 30 && tenGolems <= 50, "ten golems drop 30-50 iron, got " + tenGolems);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void outputOnlyLeavesFromBelow(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        h.assertValueEqual(farm.getSlotsForFace(Direction.DOWN).length, IronFarmBlockEntity.OUTPUT_SLOTS, "all output slots below");
        for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            h.assertValueEqual(farm.getSlotsForFace(side).length, 0, "no slots on " + side);
        }
        h.assertTrue(farm.canTakeItemThroughFace(0, iron, Direction.DOWN), "takes from below");
                h.assertTrue(!farm.canPlaceItemThroughFace(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD), Direction.UP), "nothing goes in by hopper");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 400)
    public void hopperBelowPullsIron(GameTestHelper h) {
        h.setBlock(FARM.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        // A hopper above pointing down can't push anything in
        h.setBlock(FARM.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        IronFarmBlockEntity powered = farm(h);
        HopperBlockEntity above = h.getBlockEntity(FARM.above(), HopperBlockEntity.class);
        above.setItem(0, new ItemStack(Items.BREAD, 4));
        h.succeedWhen(() -> {
            powered.energy().set(Long.MAX_VALUE); // kept powered while the world ticks it
            HopperBlockEntity below = h.getBlockEntity(FARM.below(), HopperBlockEntity.class);
            boolean gotIron = false;
            for (int i = 0; i < below.getContainerSize(); i++) gotIron |= below.getItem(i).is(Items.IRON_INGOT);
            h.assertTrue(gotIron, "the hopper below has iron");
            h.assertValueEqual(above.getItem(0).getCount(), 4, "bread stayed in the hopper above");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void theIronFarmNeedsOnlyPower(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        h.assertTrue(!farm.canPlaceItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD)), "no bread goes in");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        IronFarmMenu menu = new IronFarmMenu(1, player.getInventory(), farm);
        h.assertTrue(!menu.getSlot(IronFarmBlockEntity.BREAD_SLOT).mayPlace(new ItemStack(Items.BREAD)), "the slot takes nothing");
        h.assertTrue(!menu.getSlot(IronFarmBlockEntity.BREAD_SLOT).isActive(), "and isn't shown");
        // Bread an older farm held is dropped, and the farm works at its one speed
        farm.setItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD, 2));
        tick(h, farm, perGolem());
        h.assertTrue(farm.getItem(IronFarmBlockEntity.BREAD_SLOT).isEmpty(), "leftover bread is cleared out");
        int iron = count(farm, Items.IRON_INGOT);
        h.assertTrue(iron >= 3 && iron <= 5, "one golem per " + perGolem() + " ticks, got " + iron);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void fullFarmWaitsAndRedstonePausesIt(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) farm.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
        tick(h, farm, perGolem() * 3);
        h.assertTrue(farm.isFull(), "full");
        farm.setItem(0, ItemStack.EMPTY);
        farm.setItem(1, ItemStack.EMPTY);
        tick(h, farm, 1);
        h.assertTrue(!farm.isFull(), "room again");
        farm.clearContent();
        h.setBlock(FARM.north(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(farm.isPaused(), "a redstone signal pauses it");
        tick(h, farm, perGolem() * 2);
        h.assertValueEqual(count(farm, Items.IRON_INGOT), 0, "no golems while powered");
        h.setBlock(FARM.north(), Blocks.AIR);
        h.assertTrue(!farm.isPaused(), "working again without the signal");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 20)
    public void timersStayInTheDroppedItem(GameTestHelper h) {
        IronFarmBlockEntity farm = farm(h);
        farm.setTimers(new IronFarmTimers(40, 0, 0, 0));
        IronFarmTimers kept = farm.collectComponents().get(ModComponents.IRON_FARM_TIMERS);
        h.assertTrue(kept != null && kept.progress() == 40, "the progress goes into the item");

        ItemStack item = new ItemStack(ModItems.IRON_FARM);
        item.set(ModComponents.IRON_FARM_TIMERS, kept);
        h.setBlock(FARM.east(), ModBlocks.IRON_FARM);
        IronFarmBlockEntity placed = h.getBlockEntity(FARM.east(), IronFarmBlockEntity.class);
        placed.applyComponentsFromItemStack(item);
        h.assertValueEqual(placed.timers().progress(), 40, "and comes back when placed");

        h.setBlock(FARM.west(), ModBlocks.IRON_FARM);
        h.assertTrue(h.getBlockEntity(FARM.west(), IronFarmBlockEntity.class).collectComponents().get(ModComponents.IRON_FARM_TIMERS) == null,
                "a fresh farm's item stays plain, so it stacks");
        h.succeed();
    }
}
