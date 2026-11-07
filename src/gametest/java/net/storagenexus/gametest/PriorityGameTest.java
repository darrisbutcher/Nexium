package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.network.ConnectorBlockEntity;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlocks;

/**
 * Connector priority: items go to the highest priority inventory that takes them, even when a lower priority one already
 * holds some. Only among inventories of the same priority do the ones already holding the item go first.
 */
public class PriorityGameTest {
    //   chest A - connector A - cable - connector B - chest B
    private static final BlockPos A = new BlockPos(1, 1, 1), CONNECTOR_A = new BlockPos(2, 1, 1), CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos CONNECTOR_B = new BlockPos(4, 1, 1), B = new BlockPos(5, 1, 1);

    private static void layout(GameTestHelper h, int priorityA, int priorityB) {
        h.setBlock(A, Blocks.CHEST);
        h.setBlock(CONNECTOR_A, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CABLE, ModBlocks.INVENTORY_CABLE);
        h.setBlock(CONNECTOR_B, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(B, Blocks.CHEST);
        h.getBlockEntity(CONNECTOR_A, ConnectorBlockEntity.class).setPriority(priorityA);
        h.getBlockEntity(CONNECTOR_B, ConnectorBlockEntity.class).setPriority(priorityB);
    }

    private static void insert(GameTestHelper h, Item item, int count) {
        StorageNetwork.clearCache();
        StorageNetwork net = StorageNetwork.find(h.getLevel(), h.absolutePos(CABLE));
        h.assertTrue(net != null, "the cable is on a network");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual((int) net.insertItem(ItemVariant.of(item), count, tx), count, "all of it went in");
            tx.commit();
        }
    }

    private static int count(GameTestHelper h, BlockPos pos, Item item) {
        ChestBlockEntity chest = h.getBlockEntity(pos, ChestBlockEntity.class);
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void higherPriorityWinsOverALowerOneThatAlreadyHoldsTheItem(GameTestHelper h) {
        layout(h, 5, 0);
        h.getBlockEntity(B, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        insert(h, Items.COBBLESTONE, 10);
        h.assertValueEqual(count(h, A, Items.COBBLESTONE), 10, "the higher priority chest took it");
        h.assertValueEqual(count(h, B, Items.COBBLESTONE), 1, "the lower priority chest got none");
        h.succeed();
    }

    @GameTest
    public void atTheSamePriorityTheOneHoldingTheItemGoesFirst(GameTestHelper h) {
        layout(h, 0, 0);
        h.getBlockEntity(B, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        insert(h, Items.COBBLESTONE, 10);
        h.assertValueEqual(count(h, B, Items.COBBLESTONE), 11, "it joined the cobblestone already there");
        h.assertValueEqual(count(h, A, Items.COBBLESTONE), 0, "the empty chest got none");
        h.succeed();
    }

    @GameTest
    public void aHigherPriorityDrawerControllerBeatsABarrelThatHoldsTheItem(GameTestHelper h) {
        // drawer - controller - connector (5) - cable - connector (0) - barrel holding some cobblestone
        BlockPos drawer = new BlockPos(0, 1, 1);
        h.setBlock(drawer, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        h.setBlock(A, ModBlocks.DRAWER_CONTROLLER);
        h.setBlock(CONNECTOR_A, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CABLE, ModBlocks.INVENTORY_CABLE);
        h.setBlock(CONNECTOR_B, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(B, Blocks.BARREL);
        h.getBlockEntity(CONNECTOR_A, ConnectorBlockEntity.class).setPriority(5);
        DrawerBlockEntity d = h.getBlockEntity(drawer, DrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            d.getItemStorage().insert(ItemVariant.of(Items.COBBLESTONE), 1, tx);
            tx.commit();
        }
        h.getBlockEntity(B, net.minecraft.world.level.block.entity.BarrelBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        insert(h, Items.COBBLESTONE, 20);
        h.assertValueEqual(d.slot(0).getAmount(), 21L, "the drawer behind the controller took it");
        h.assertValueEqual(h.getBlockEntity(B, net.minecraft.world.level.block.entity.BarrelBlockEntity.class).getItem(0).getCount(), 5, "the barrel got none");
        h.succeed();
    }
}
