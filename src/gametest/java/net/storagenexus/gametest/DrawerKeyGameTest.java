package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.DrawerKeyItem;
import net.storagenexus.upgrade.Lockable;

/** The Drawer Key: right-click to lock, right-click again to unlock, never used up, on every kind of drawer. */
public class DrawerKeyGameTest {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static long insert(DrawerBlockEntity.DrawerSlot slot, net.minecraft.world.item.Item item, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            long n = slot.insert(ItemVariant.of(item), amount, tx);
            tx.commit();
            return n;
        }
    }

    private static void extract(DrawerBlockEntity.DrawerSlot slot, net.minecraft.world.item.Item item, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            slot.extract(ItemVariant.of(item), amount, tx);
            tx.commit();
        }
    }

    /** A real right-click on this face of the block with the stack in the player's hand. */
    private static InteractionResult click(GameTestHelper h, ServerPlayer player, BlockPos pos, ItemStack stack, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, abs, false);
        return player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    @GameTest
    public void aKeyLocksAnItemDrawerAndUnlocksItAndIsNeverUsedUp(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(POS, DrawerBlockEntity.class);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack key = new ItemStack(ModItems.DRAWER_KEY, 3);
        insert(be.slot(0), Items.DIAMOND, 10);

        h.assertTrue(!be.isLocked(), "it starts unlocked");
        h.assertTrue(click(h, player, POS, key, Direction.UP).consumesAction(), "a right-click on the top is taken by the key");
        h.assertTrue(be.isLocked(), "locked");
        h.assertValueEqual(key.getCount(), 3, "the key is not used up");
        extract(be.slot(0), Items.DIAMOND, 10);
        h.assertTrue(be.slot(0).displayVariant().isOf(Items.DIAMOND), "an empty locked slot is still reserved for diamonds");
        h.assertValueEqual(insert(be.slot(0), Items.EMERALD, 5), 0L, "and takes nothing else");
        h.assertValueEqual(insert(be.slot(0), Items.DIAMOND, 5), 5L, "but takes diamonds");
        h.assertValueEqual(be.slot(0).getAmount(), 5L, "stored");

        // The front too: the key does not go into the drawer as an item
        h.assertTrue(click(h, player, POS, key, be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)).consumesAction(), "a click on the front");
        h.assertTrue(!be.isLocked(), "unlocked again");
        h.assertValueEqual(key.getCount(), 3, "still three keys");
        extract(be.slot(0), Items.DIAMOND, 5);
        h.assertTrue(be.slot(0).displayVariant().isBlank(), "an unlocked empty slot is free");
        h.assertValueEqual(insert(be.slot(0), Items.EMERALD, 5), 5L, "and takes anything");
        h.succeed();
    }

    @GameTest
    public void aSneakingPlayersKeyStillWorksThroughTheItem(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(POS, DrawerBlockEntity.class);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack key = new ItemStack(ModItems.DRAWER_KEY);
        BlockPos abs = h.absolutePos(POS);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        h.assertTrue(ModItems.DRAWER_KEY.useOn(context).consumesAction(), "the item's own use on a drawer");
        h.assertTrue(be.isLocked(), "locks it");
        h.assertTrue(ModItems.DRAWER_KEY.useOn(context).consumesAction(), "and again");
        h.assertTrue(!be.isLocked(), "unlocks it");
        h.setBlock(POS.east(), Blocks.STONE);
        UseOnContext onStone = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs.east()), Direction.UP, abs.east(), false));
        h.assertTrue(!ModItems.DRAWER_KEY.useOn(onStone).consumesAction(), "on anything that is not a drawer it does nothing");
        h.assertTrue(!DrawerKeyItem.toggle(h.getLevel(), abs.east(), player), "toggle says so");
        h.succeed();
    }

    @GameTest
    public void aKeyLocksCompactingAndFluidDrawersToo(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        h.setBlock(POS, ModBlocks.COMPACTING_DRAWER);
        CompactingDrawerBlockEntity compacting = h.getBlockEntity(POS, CompactingDrawerBlockEntity.class);
        h.assertTrue(click(h, player, POS, new ItemStack(ModItems.DRAWER_KEY), Direction.UP).consumesAction(), "the key on a compacting drawer");
        h.assertTrue(compacting.isLocked(), "locks it");
        click(h, player, POS, new ItemStack(ModItems.DRAWER_KEY), Direction.UP);
        h.assertTrue(!compacting.isLocked(), "and unlocks it");

        BlockPos fluidPos = POS.east(2);
        h.setBlock(fluidPos, ModBlocks.FLUID_DRAWER_1X1);
        FluidDrawerBlockEntity fluid = h.getBlockEntity(fluidPos, FluidDrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            fluid.slot(0).insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 2, tx);
            tx.commit();
        }
        h.assertTrue(click(h, player, fluidPos, new ItemStack(ModItems.DRAWER_KEY), Direction.UP).consumesAction(), "the key on a fluid drawer");
        h.assertTrue(fluid.isLocked(), "locks it");
        try (Transaction tx = Transaction.openOuter()) {
            fluid.slot(0).extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 2, tx);
            tx.commit();
        }
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(fluid.slot(0).insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx), 0L, "an empty locked tank takes no other fluid");
        }
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(fluid.slot(0).insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "but takes water");
        }
        click(h, player, fluidPos, new ItemStack(ModItems.DRAWER_KEY), Direction.UP);
        h.assertTrue(!fluid.isLocked(), "unlocked");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(fluid.slot(0).insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "and takes anything when empty again");
        }
        h.succeed();
    }

    @GameTest
    public void aLockedDrawerStaysLockedInTheDroppedItemAndWhenSaved(GameTestHelper h) {
        Lockable[] drawers = new Lockable[3];
        net.minecraft.world.level.block.entity.BlockEntity[] entities = new net.minecraft.world.level.block.entity.BlockEntity[3];
        net.minecraft.world.level.block.Block[] blocks = {ModBlocks.DRAWERS.get(DrawerWood.OAK)[0], ModBlocks.COMPACTING_DRAWER, ModBlocks.FLUID_DRAWER_1X1};
        for (int i = 0; i < 3; i++) {
            BlockPos pos = POS.east(i * 2);
            h.setBlock(pos, blocks[i]);
            entities[i] = h.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
            drawers[i] = (Lockable) entities[i];
            h.assertTrue(entities[i].collectComponents().get(ModComponents.DRAWER_LOCKED) == null, "an unlocked drawer leaves its item plain, so it stacks");
            drawers[i].setLocked(true);
            Boolean kept = entities[i].collectComponents().get(ModComponents.DRAWER_LOCKED);
            h.assertTrue(Boolean.TRUE.equals(kept), "a locked drawer puts the lock in its item (drawer " + i + ")");

            // Saved with the world
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
            entities[i].saveWithFullMetadata(out);
            net.minecraft.world.level.block.entity.BlockEntity loaded = ((net.minecraft.world.level.block.EntityBlock) blocks[i])
                    .newBlockEntity(entities[i].getBlockPos(), entities[i].getBlockState());
            loaded.setLevel(h.getLevel());
            loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
            h.assertTrue(((Lockable) loaded).isLocked(), "the lock is saved with the world (drawer " + i + ")");

            // ... and placed from the item
            ItemStack item = new ItemStack(blocks[i]);
            item.set(ModComponents.DRAWER_LOCKED, true);
            BlockPos next = pos.south(2);
            h.setBlock(next, blocks[i]);
            var placed = h.getBlockEntity(next, net.minecraft.world.level.block.entity.BlockEntity.class);
            placed.applyComponentsFromItemStack(item);
            h.assertTrue(((Lockable) placed).isLocked(), "and comes back when placed (drawer " + i + ")");
        }
        h.succeed();
    }
}
