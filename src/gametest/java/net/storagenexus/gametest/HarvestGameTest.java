package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.aiot.module.VeinKey;

/**
 * Right-click harvest: a fully grown crop drops its harvest and is replanted where it stood; a growing one is left
 * alone; sneaking skips it; and with the vein mining key held the whole connected patch is harvested and replanted.
 */
public class HarvestGameTest {
    private static final BlockPos AT = new BlockPos(3, 2, 3);

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(h.absolutePos(AT.above(2))));
        return player;
    }

    private static void rightClick(GameTestHelper h, ServerPlayer player, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    private static void wheat(GameTestHelper h, BlockPos rel, int age) {
        h.setBlock(rel.below(), Blocks.FARMLAND);
        h.setBlock(rel, ((CropBlock) Blocks.WHEAT).getStateForAge(age));
    }

    private static int dropped(GameTestHelper h, Item item) {
        int n = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(AT)).inflate(6))) {
            if (e.getItem().is(item)) n += e.getItem().getCount();
        }
        return n;
    }

    @GameTest
    public void ripeWheatIsHarvestedAndReplanted(GameTestHelper h) {
        wheat(h, AT, 7);
        ServerPlayer player = player(h);
        rightClick(h, player, AT);
        h.assertBlockProperty(AT, CropBlock.AGE, 0);
        h.assertTrue(dropped(h, Items.WHEAT) >= 1, "wheat dropped");
        h.succeed();
    }

    @GameTest
    public void growingWheatAndSneakingAreLeftAlone(GameTestHelper h) {
        wheat(h, AT, 4);
        ServerPlayer player = player(h);
        rightClick(h, player, AT);
        h.assertBlockProperty(AT, CropBlock.AGE, 4);
        wheat(h, AT, 7);
        player.setShiftKeyDown(true);
        rightClick(h, player, AT);
        h.assertBlockProperty(AT, CropBlock.AGE, 7);
        h.assertValueEqual(dropped(h, Items.WHEAT), 0, "nothing dropped");
        h.succeed();
    }

    @GameTest
    public void netherWartAndCocoaAreReplantedToo(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.SOUL_SAND);
        h.setBlock(AT, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
        BlockPos log = AT.east(2), pod = log.north();
        h.setBlock(log, Blocks.JUNGLE_LOG);
        h.setBlock(pod, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, Direction.SOUTH).setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE));
        ServerPlayer player = player(h);
        rightClick(h, player, AT);
        rightClick(h, player, pod);
        h.assertBlockProperty(AT, NetherWartBlock.AGE, 0);
        h.assertBlockProperty(pod, CocoaBlock.AGE, 0);
        h.assertBlockProperty(pod, CocoaBlock.FACING, Direction.SOUTH);
        h.assertTrue(dropped(h, Items.NETHER_WART) >= 1 && dropped(h, Items.COCOA_BEANS) >= 1, "wart and beans dropped");
        h.succeed();
    }

    @GameTest
    public void theVeinKeyHarvestsTheWholeField(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) wheat(h, AT.offset(dx, 0, dz), 7);
        wheat(h, AT.offset(1, 0, 1), 3); // one still growing
        ServerPlayer player = player(h);
        VeinKey.set(player, true);
        try {
            rightClick(h, player, AT);
        } finally {
            VeinKey.set(player, false);
        }
        int replanted = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (h.getBlockState(AT.offset(dx, 0, dz)).getValue(CropBlock.AGE) == 0) replanted++;
        }
        h.assertValueEqual(replanted, 8, "the 8 ripe ones were harvested and replanted");
        h.assertBlockProperty(AT.offset(1, 0, 1), CropBlock.AGE, 3);
        h.assertTrue(dropped(h, Items.WHEAT) >= 8, "wheat from all 8 dropped");
        h.succeed();
    }
}
