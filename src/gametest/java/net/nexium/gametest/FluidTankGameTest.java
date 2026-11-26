package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.fluid.FluidTankBlockEntity;
import net.nexium.fluid.FluidTankItem;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;

/** The Fluid Tanks: sizes, one fluid, kept when broken, the item as a container, and scooping and pouring like a bucket. */
public class FluidTankGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final FluidVariant WATER = FluidVariant.of(Fluids.WATER), LAVA = FluidVariant.of(Fluids.LAVA);

    private static long insert(FluidTankBlockEntity be, FluidVariant v, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            long n = be.tank().insert(v, amount, tx);
            tx.commit();
            return n;
        }
    }

    @GameTest
    public void tanksHoldTheirBucketsOfOneFluid(GameTestHelper h) {
        var blocks = new net.minecraft.world.level.block.Block[]{ModBlocks.IRON_FLUID_TANK, ModBlocks.GOLD_FLUID_TANK, ModBlocks.DIAMOND_FLUID_TANK};
        long[] buckets = {16, 64, 256};
        for (int t = 0; t < 3; t++) {
            h.setBlock(POS, Blocks.AIR);
            h.setBlock(POS, blocks[t]);
            FluidTankBlockEntity be = h.getBlockEntity(POS, FluidTankBlockEntity.class);
            h.assertValueEqual(be.capacity(), buckets[t] * FluidConstants.BUCKET, "capacity of tier " + (t + 1));
            h.assertValueEqual(insert(be, WATER, 1000 * FluidConstants.BUCKET), buckets[t] * FluidConstants.BUCKET, "fills to the top");
            h.assertValueEqual(insert(be, LAVA, FluidConstants.BUCKET), 0L, "and takes no second fluid");
        }
        h.succeed();
    }

    @GameTest
    public void aBrokenTankKeepsItsFluidInTheItem(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.GOLD_FLUID_TANK);
        FluidTankBlockEntity be = h.getBlockEntity(POS, FluidTankBlockEntity.class);
        insert(be, LAVA, 5 * FluidConstants.BUCKET);
        BlockPos abs = h.absolutePos(POS);
        h.getLevel().destroyBlock(abs, true);
        ItemStack found = ItemStack.EMPTY;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2))) {
            if (e.getItem().is(ModItems.GOLD_FLUID_TANK)) found = e.getItem();
        }
        h.assertFalse(found.isEmpty(), "the tank drops as an item");
        FluidTankBlockEntity.Contents c = FluidTankItem.contents(found);
        h.assertTrue(c.variant().equals(LAVA) && c.amount() == 5 * FluidConstants.BUCKET, "carrying its five buckets of lava: " + c);
        h.succeed();
    }

    @GameTest
    public void aPlacedTankItemRestoresItsFluid(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.IRON_FLUID_TANK);
        FluidTankBlockEntity be = h.getBlockEntity(POS, FluidTankBlockEntity.class);
        ItemStack stack = new ItemStack(ModItems.IRON_FLUID_TANK);
        FluidTankItem.setContents(stack, new FluidTankBlockEntity.Contents(WATER, 3 * FluidConstants.BUCKET));
        be.applyComponentsFromItemStack(stack);
        h.assertValueEqual(be.amount(), 3 * FluidConstants.BUCKET, "the fluid comes back when placed");
        h.assertTrue(be.fluid().equals(WATER), "and it is water");
        h.succeed();
    }

    @GameTest
    public void theItemIsAFluidContainerLikeABucket(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.IRON_FLUID_TANK));
        var slot = PlayerInventoryStorage.of(player).getSlot(0);
        var storage = ContainerItemContext.ofPlayerSlot(player, slot).find(FluidStorage.ITEM);
        h.assertTrue(storage != null, "the tank item exposes fluid storage");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(storage.insert(WATER, 20 * FluidConstants.BUCKET, tx), 16 * FluidConstants.BUCKET, "takes up to its 16 buckets");
            tx.commit();
        }
        FluidTankBlockEntity.Contents c = FluidTankItem.contents(player.getInventory().getItem(0));
        h.assertTrue(c.variant().equals(WATER) && c.amount() == 16 * FluidConstants.BUCKET, "and keeps them in the stack: " + c);
        h.succeed();
    }

    @GameTest
    public void aHeldTankScoopsASourceAndPoursABucket(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.IRON_FLUID_TANK);
        BlockPos water = h.absolutePos(new BlockPos(1, 2, 1));
        h.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(water), Direction.UP, water, false);
        h.assertTrue(FluidTankItem.scoop(h.getLevel(), player, stack, hit), "scoops the water source");
        h.assertTrue(h.getLevel().getBlockState(water).isAir() || !h.getLevel().getBlockState(water).getFluidState().isSource(), "the source is gone");
        h.assertValueEqual(FluidTankItem.contents(stack).amount(), FluidConstants.BUCKET, "a bucket is in the tank");
        // It will not mix fluids
        BlockPos lava = h.absolutePos(new BlockPos(3, 2, 1));
        h.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        h.assertFalse(FluidTankItem.scoop(h.getLevel(), player, stack, new BlockHitResult(Vec3.atCenterOf(lava), Direction.UP, lava, false)), "lava is refused while it holds water");
        // Pour onto a stone block
        BlockPos stone = h.absolutePos(new BlockPos(2, 2, 3));
        h.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        h.assertTrue(FluidTankItem.pour(h.getLevel(), player, stack, new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false)), "pours a bucket");
        h.assertTrue(h.getLevel().getBlockState(stone.above()).getFluidState().isSource(), "a water source appears above the stone");
        h.assertTrue(FluidTankItem.contents(stack).isEmpty(), "and the tank is empty again");
        h.succeed();
    }

    @GameTest
    public void theItemShowsAFillBar(GameTestHelper h) {
        ItemStack stack = new ItemStack(ModItems.GOLD_FLUID_TANK);
        h.assertFalse(stack.isBarVisible(), "no bar while empty");
        FluidTankItem.setContents(stack, new FluidTankBlockEntity.Contents(LAVA, 32 * FluidConstants.BUCKET));
        h.assertTrue(stack.isBarVisible(), "a bar once it holds fluid");
        h.assertValueEqual(stack.getBarWidth(), 7, "half of 64 buckets is about half of 13 pixels");
        FluidTankItem.setContents(stack, new FluidTankBlockEntity.Contents(WATER, 64 * FluidConstants.BUCKET));
        h.assertValueEqual(stack.getBarWidth(), 13, "a full tank fills the bar");
        FluidTankItem.setContents(stack, new FluidTankBlockEntity.Contents(WATER, 1));
        h.assertValueEqual(stack.getBarWidth(), 1, "and a drop still shows one pixel");
        h.succeed();
    }
}
