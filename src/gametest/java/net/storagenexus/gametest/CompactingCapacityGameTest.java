package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.registry.ModBlocks;

/** Compacting drawer capacity. */
public class CompactingCapacityGameTest {
    @GameTest
    public void holds54StacksOfIronBlocks(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.COMPACTING_DRAWER);
        CompactingDrawerBlockEntity be = h.getBlockEntity(pos, CompactingDrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            long in = be.getItemStorage().insert(ItemVariant.of(Items.IRON_BLOCK), 54 * 64 + 10, tx);
            h.assertValueEqual(in, 54L * 64, "iron blocks accepted");
            tx.commit();
        }
        int nugget = -1, block = -1;
        for (int i = 0; i < be.tierCount(); i++) {
            if (be.tier(i).getItem() == Items.IRON_NUGGET) nugget = i;
            if (be.tier(i).getItem() == Items.IRON_BLOCK) block = i;
        }
        h.assertValueEqual(be.amountOf(block), 54L * 64, "iron blocks stored");
        h.assertValueEqual(be.amountOf(nugget), 54L * 64 * 81, "same amount in nuggets");
        h.assertTrue(be.fillFraction() == 1.0, "a full pool shows a full fill bar");
        h.succeed();
    }

    @GameTest
    public void fillFractionCountsEveryForm(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.COMPACTING_DRAWER);
        CompactingDrawerBlockEntity be = h.getBlockEntity(pos, CompactingDrawerBlockEntity.class);
        h.assertTrue(be.fillFraction() == 0, "empty");
        try (Transaction tx = Transaction.openOuter()) {
            be.getItemStorage().insert(ItemVariant.of(Items.IRON_BLOCK), 27 * 64, tx);
            be.getItemStorage().insert(ItemVariant.of(Items.IRON_INGOT), 9 * 64, tx); // 64 blocks' worth
            tx.commit();
        }
        double expected = (27.0 * 64 + 64) / (54 * 64);
        h.assertTrue(Math.abs(be.fillFraction() - expected) < 1e-9, "half full plus a stack of blocks in ingots: " + be.fillFraction());
        h.succeed();
    }
}
