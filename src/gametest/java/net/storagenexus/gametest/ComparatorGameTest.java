package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlocks;

/**
 * Comparators read drawers and farms: a drawer by how full it is (0 empty, 1 with anything in it, 15 full, like a
 * chest), a farm by how full its output is, and 15 as soon as it has stopped for lack of room.
 */
public class ComparatorGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static int signal(GameTestHelper h, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        return h.getLevel().getBlockState(abs).getAnalogOutputSignal(h.getLevel(), abs, Direction.NORTH);
    }

    private static void put(DrawerBlockEntity drawer, int slot, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            drawer.slot(slot).insert(ItemVariant.of(Items.COBBLESTONE), amount, tx);
            tx.commit();
        }
    }

    @GameTest
    public void aDrawerReadsHowFullItIs(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity drawer = h.getBlockEntity(POS, DrawerBlockEntity.class);
        h.assertValueEqual(signal(h, POS), 0, "empty");
        put(drawer, 0, 1);
        h.assertValueEqual(signal(h, POS), 1, "one item");
        put(drawer, 0, 54 * 64 / 2 - 1);
        h.assertValueEqual(signal(h, POS), 8, "half full (27 of 54 stacks)");
        put(drawer, 0, 54 * 64 / 2 - 1);
        h.assertValueEqual(signal(h, POS), 14, "one item short of full");
        put(drawer, 0, 1);
        h.assertValueEqual(signal(h, POS), 15, "full");
        // A 2x2 drawer with one of its four sections full is a quarter full
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[2]);
        DrawerBlockEntity four = h.getBlockEntity(POS, DrawerBlockEntity.class);
        put(four, 0, 54 * 64 / 4);
        h.assertValueEqual(signal(h, POS), 4, "a quarter full");
        h.succeed();
    }

    @GameTest
    public void compactingAndFluidDrawersReadTheSameWay(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.COMPACTING_DRAWER);
        CompactingDrawerBlockEntity compacting = h.getBlockEntity(POS, CompactingDrawerBlockEntity.class);
        h.assertValueEqual(signal(h, POS), 0, "empty");
        try (Transaction tx = Transaction.openOuter()) {
            compacting.getItemStorage().insert(ItemVariant.of(Items.IRON_BLOCK), 54 * 64 / 2, tx);
            tx.commit();
        }
        h.assertValueEqual(signal(h, POS), 8, "half full of iron blocks");
        try (Transaction tx = Transaction.openOuter()) {
            compacting.getItemStorage().insert(ItemVariant.of(Items.IRON_BLOCK), 54 * 64 / 2, tx);
            tx.commit();
        }
        h.assertValueEqual(signal(h, POS), 15, "full");

        h.setBlock(POS, ModBlocks.FLUID_DRAWER_1X1);
        FluidDrawerBlockEntity tank = h.getBlockEntity(POS, FluidDrawerBlockEntity.class);
        h.assertValueEqual(signal(h, POS), 0, "empty");
        try (Transaction tx = Transaction.openOuter()) {
            tank.slot(0).insert(FluidVariant.of(Fluids.WATER), 16 * FluidConstants.BUCKET, tx);
            tx.commit();
        }
        h.assertValueEqual(signal(h, POS), 8, "16 of 32 buckets");
        try (Transaction tx = Transaction.openOuter()) {
            tank.slot(0).insert(FluidVariant.of(Fluids.WATER), 16 * FluidConstants.BUCKET, tx);
            tx.commit();
        }
        h.assertValueEqual(signal(h, POS), 15, "full");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void aRealComparatorFollowsTheDrawer(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        BlockPos comparator = POS.south();
        h.setBlock(comparator.below(), Blocks.STONE);
        h.setBlock(comparator, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
        DrawerBlockEntity drawer = h.getBlockEntity(POS, DrawerBlockEntity.class);
        put(drawer, 0, 54 * 64 / 2);
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(h.getBlockEntity(comparator, ComparatorBlockEntity.class).getOutputSignal(), 8, "the comparator's output, half full");
            try (Transaction tx = Transaction.openOuter()) {
                drawer.slot(0).extract(ItemVariant.of(Items.COBBLESTONE), 54 * 64 / 2, tx);
                tx.commit();
            }
            h.runAfterDelay(6, () -> {
                h.assertValueEqual(h.getBlockEntity(comparator, ComparatorBlockEntity.class).getOutputSignal(), 0, "and emptied again");
                h.succeed();
            });
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void aFarmReadsItsOutputAndFifteenWhenStopped(GameTestHelper h) {
        h.setBlock(POS, net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
        FarmBlockEntity farm = h.getBlockEntity(POS, FarmBlockEntity.class);
        h.assertValueEqual(signal(h, POS), 0, "empty");
        farm.cycle(h.getLevel(), FarmKind.SUGAR_CANE, h.getLevel().getRandom());
        h.assertValueEqual(signal(h, POS), 1, "a little sugar cane");
        // Half the slots full
        for (int i = 0; i < 13; i++) farm.setItem(i, new ItemStack(Items.SUGAR_CANE, 64));
        farm.setItem(13, new ItemStack(Items.SUGAR_CANE, 32));
        h.assertValueEqual(signal(h, POS), 8, "half full");
        // No empty slot left: the farm stops, and reads 15 even though the last stacks are not full
        for (int i = 13; i < FarmBlockEntity.OUTPUT_SLOTS; i++) farm.setItem(i, new ItemStack(Items.SUGAR_CANE, 1));
        FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
        h.assertTrue(farm.isFull(), "stopped for lack of room");
        h.assertValueEqual(signal(h, POS), 15, "stopped");
        farm.setItem(26, ItemStack.EMPTY);
        FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
        h.assertTrue(signal(h, POS) < 15, "running again once there is room");

        // The Iron Farm the same
        BlockPos ironPos = POS.east(2);
        h.setBlock(ironPos, net.storagenexus.essentials.registry.ModBlocks.IRON_FARM);
        IronFarmBlockEntity iron = h.getBlockEntity(ironPos, IronFarmBlockEntity.class);
        h.assertValueEqual(signal(h, ironPos), 0, "empty");
        iron.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        h.assertValueEqual(signal(h, ironPos), 1, "one stack of iron");
        for (int i = 0; i < IronFarmBlockEntity.OUTPUT_SLOTS; i++) iron.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
        IronFarmBlockEntity.serverTick(h.getLevel(), iron.getBlockPos(), iron.getBlockState(), iron);
        h.assertValueEqual(signal(h, ironPos), 15, "full");
        h.succeed();
    }
}
