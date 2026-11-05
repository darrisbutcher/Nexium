package net.storagenexus.gametest;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/** Breaking and placing storage blocks keeps their contents and upgrades; recipes load. */
public class KeepContentsGameTest {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /** Loot drop for the block at POS, checking it is exactly one stack of the block itself. */
    private static ItemStack singleDrop(GameTestHelper h, Block block) {
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(POS);
        BlockEntity be = level.getBlockEntity(abs);
        List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, be);
        h.assertValueEqual(drops.size(), 1, "number of drops");
        h.assertTrue(drops.get(0).is(block.asItem()), "drop is the block itself");
        return drops.get(0);
    }

    /** Really breaks the block (with drops) and checks only the block item came out. */
    private static void breakAndCheckNoSpill(GameTestHelper h, Block block) {
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(POS);
        level.destroyBlock(abs, true);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3));
        h.assertValueEqual(items.size(), 1, "item entities after breaking");
        h.assertTrue(items.get(0).getItem().is(block.asItem()), "the only item entity is the block");
        items.forEach(ItemEntity::discard);
    }

    private static void placeBack(GameTestHelper h, ItemStack stack) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = stack.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held); // placement reads the held item
        h.placeAt(player, held, POS.below(), Direction.UP);
    }

    @GameTest
    public void drawerKeepsItemsAndUpgrades(GameTestHelper h) {
        Block block = ModBlocks.DRAWERS.get(DrawerWood.OAK)[0];
        h.setBlock(POS, block);
        DrawerBlockEntity be = h.getBlockEntity(POS, DrawerBlockEntity.class);
        h.assertTrue(be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE)), "iron upgrade installs");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(be.getItemStorage().insert(ItemVariant.of(Items.COBBLESTONE), 5000, tx), 5000L, "inserted");
            tx.commit();
        }

        ItemStack drop = singleDrop(h, block);
        h.assertTrue(drop.has(ModComponents.DRAWER_SLOTS), "drop carries drawer slots");
        h.assertTrue(drop.has(ModComponents.UPGRADES), "drop carries upgrades");
        breakAndCheckNoSpill(h, block);

        placeBack(h, drop);
        DrawerBlockEntity placed = h.getBlockEntity(POS, DrawerBlockEntity.class);
        h.assertTrue(placed.slot(0).getResource().equals(ItemVariant.of(Items.COBBLESTONE)), "item restored");
        h.assertValueEqual(placed.slot(0).getAmount(), 5000L, "amount restored");
        h.assertValueEqual(placed.slot(0).capacity(), 54L * 64 * 2, "iron upgrade capacity restored");
        h.succeed();
    }

    @GameTest
    public void emptyDrawerDropsPlainItem(GameTestHelper h) {
        Block block = ModBlocks.DRAWERS.get(DrawerWood.OAK)[2];
        h.setBlock(POS, block);
        ItemStack drop = singleDrop(h, block);
        h.assertTrue(drop.getComponentsPatch().isEmpty(), "empty drawer drop has no extra data, so it stacks");
        h.succeed();
    }

    @GameTest
    public void compactingDrawerKeepsContents(GameTestHelper h) {
        Block block = ModBlocks.COMPACTING_DRAWER;
        h.setBlock(POS, block);
        CompactingDrawerBlockEntity be = h.getBlockEntity(POS, CompactingDrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(be.getItemStorage().insert(ItemVariant.of(Items.IRON_INGOT), 100, tx), 100L, "inserted");
            tx.commit();
        }
        h.assertValueEqual(be.tierCount(), 3, "iron block / ingot / nugget chain");

        ItemStack drop = singleDrop(h, block);
        h.assertTrue(drop.has(ModComponents.COMPACTING_CONTENTS), "drop carries compacting contents");
        breakAndCheckNoSpill(h, block);

        placeBack(h, drop);
        CompactingDrawerBlockEntity placed = h.getBlockEntity(POS, CompactingDrawerBlockEntity.class);
        int ingot = -1;
        for (int i = 0; i < placed.tierCount(); i++) if (placed.tier(i).getItem() == Items.IRON_INGOT) ingot = i;
        h.assertTrue(ingot >= 0, "ingot tier restored");
        h.assertValueEqual(placed.amountOf(ingot), 100L, "ingots restored");
        h.succeed();
    }

    @GameTest
    public void aSimulatedInsertDoesNotLockEmptyCompactingDrawer(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.COMPACTING_DRAWER);
        CompactingDrawerBlockEntity be = h.getBlockEntity(POS, CompactingDrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            be.getItemStorage().insert(ItemVariant.of(Items.GOLD_INGOT), 10, tx);
            // aborted: never committed
        }
        h.assertValueEqual(be.tierCount(), 0, "tiers after aborted insert");
        h.succeed();
    }

    @GameTest
    public void allRecipesLoad(GameTestHelper h) {
        long ours = h.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().identifier().getNamespace().equals("storagenexus")).count();
        // Every file in data/storagenexus/recipe: AIOT (storagenexus:aiot_merge), Nexium and module recipes included
        h.assertValueEqual(ours, 100L, "storagenexus recipes loaded");
        h.succeed();
    }

    @GameTest
    public void fluidDrawerKeepsFluid(GameTestHelper h) {
        Block block = ModBlocks.FLUID_DRAWER_1X1;
        h.setBlock(POS, block);
        FluidDrawerBlockEntity be = h.getBlockEntity(POS, FluidDrawerBlockEntity.class);
        long amount = 10 * FluidConstants.BUCKET;
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(be.getFluidStorage().insert(FluidVariant.of(Fluids.WATER), amount, tx), amount, "inserted");
            tx.commit();
        }

        ItemStack drop = singleDrop(h, block);
        h.assertTrue(drop.has(ModComponents.FLUID_SLOTS), "drop carries fluid");
        breakAndCheckNoSpill(h, block);

        placeBack(h, drop);
        FluidDrawerBlockEntity placed = h.getBlockEntity(POS, FluidDrawerBlockEntity.class);
        h.assertTrue(placed.slot(0).getResource().equals(FluidVariant.of(Fluids.WATER)), "fluid restored");
        h.assertValueEqual(placed.slot(0).getAmount(), amount, "amount restored");
        h.succeed();
    }
}
