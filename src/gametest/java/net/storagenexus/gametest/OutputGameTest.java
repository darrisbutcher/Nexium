package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.dimension.DimensionalInputBlockEntity;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.dimension.DimensionalOutputBlockEntity;
import net.storagenexus.registry.ModBlocks;

/** Dimensional Outputs push on enabled sides into inventories and conduits. */
public class OutputGameTest {
    private static final BlockPos OUT = new BlockPos(3, 2, 3);

    private static long push(GameTestHelper h, int count) {
        DimensionalOutputBlockEntity out = h.getBlockEntity(OUT, DimensionalOutputBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            long n = out.receive(ItemVariant.of(Items.COBBLESTONE), count, tx);
            tx.commit();
            return n;
        }
    }

    private static int cobble(GameTestHelper h, BlockPos chest) {
        ChestBlockEntity c = h.getBlockEntity(chest, ChestBlockEntity.class);
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(Items.COBBLESTONE)) n += c.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void pushesIntoInventoriesOnAnySide(GameTestHelper h) {
        h.setBlock(OUT, ModBlocks.DIMENSIONAL_OUTPUT); // faces down, at nothing
        h.setBlock(OUT.east(), Blocks.CHEST);
        h.assertValueEqual(push(h, 20), 20L, "items pushed into a chest on the side");
        h.assertValueEqual(cobble(h, OUT.east()), 20, "cobble in the side chest");
        // Fill that chest; the overflow goes to a chest on another side
        ChestBlockEntity east = h.getBlockEntity(OUT.east(), ChestBlockEntity.class);
        for (int i = 0; i < east.getContainerSize(); i++) east.setItem(i, new ItemStack(Items.DIRT, 64));
        h.setBlock(OUT.west(), Blocks.CHEST);
        h.assertValueEqual(push(h, 30), 30L, "items pushed once the first chest is full");
        h.assertValueEqual(cobble(h, OUT.west()), 30, "cobble in the other chest");
        h.succeed();
    }

    @GameTest
    public void disabledSidesAreSkipped(GameTestHelper h) {
        h.setBlock(OUT, ModBlocks.DIMENSIONAL_OUTPUT);
        h.setBlock(OUT.east(), Blocks.CHEST);
        h.setBlock(OUT.west(), Blocks.CHEST);
        DimensionalOutputBlockEntity out = h.getBlockEntity(OUT, DimensionalOutputBlockEntity.class);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        DimensionalMenu menu = new DimensionalMenu(1, player.getInventory(), out);
        menu.clickMenuButton(player, Direction.EAST.get3DDataValue()); // the screen's "E" button
        h.assertTrue(!out.isSideEnabled(Direction.EAST), "east turned off");
        for (int i = 0; i < 6; i++) push(h, 5); // every side gets a turn at going first
        h.assertValueEqual(cobble(h, OUT.east()), 0, "nothing pushed into the disabled side");
        h.assertValueEqual(cobble(h, OUT.west()), 30, "everything went to the enabled side");
        menu.clickMenuButton(player, Direction.EAST.get3DDataValue());
        h.assertTrue(out.isSideEnabled(Direction.EAST), "east back on");
        h.succeed();
    }

    @GameTest
    public void pushesIntoItemConduitsButNotInputs(GameTestHelper h) {
        h.setBlock(OUT, ModBlocks.DIMENSIONAL_OUTPUT);
        BlockPos conduit = OUT.north(), chest = conduit.north();
        h.setBlock(conduit, ModBlocks.CONDUIT_BUNDLE);
        h.getBlockEntity(conduit, ConduitBundleBlockEntity.class).addConduit(ConduitType.ITEM, 1);
        h.setBlock(chest, Blocks.CHEST);
        h.setBlock(OUT.south(), ModBlocks.DIMENSIONAL_INPUT);
        h.assertValueEqual(push(h, 16), 16L, "items pushed into the conduit line");
        h.assertValueEqual(cobble(h, chest), 16, "cobble delivered by the conduit to the chest");
        DimensionalInputBlockEntity input = h.getBlockEntity(OUT.south(), DimensionalInputBlockEntity.class);
        h.assertTrue(input.buffer().isEmpty(), "nothing pushed back into a Dimensional Input");
        h.succeed();
    }
}
