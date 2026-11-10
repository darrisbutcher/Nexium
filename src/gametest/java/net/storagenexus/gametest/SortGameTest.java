package net.storagenexus.gametest;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.sort.InventorySorter;
import net.storagenexus.sort.InventorySorter.Region;
import net.storagenexus.sort.SortOrder;

/** Sorting: the hotbar is never touched, regions follow the cursor, shulker boxes and bundles go bottom right. */
public class SortGameTest {
    private static String name(ItemStack s) {
        return s.isEmpty() ? "-" : s.getItem().toString().replace("minecraft:", "") + "x" + s.getCount();
    }

    @GameTest
    public void alphabeticalMergesAndOrders(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.STONE, 40));
        chest.setItem(5, new ItemStack(Items.APPLE, 3));
        chest.setItem(9, new ItemStack(Items.STONE, 40));
        chest.setItem(20, new ItemStack(Items.BREAD, 2));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        List<Slot> slots = InventorySorter.slotsOf(menu, Region.CONTAINER);
        h.assertValueEqual(slots.size(), 27, "container slots");
        InventorySorter.sort(slots, SortOrder.ALPHABETICAL, player);
        h.assertValueEqual(name(chest.getItem(0)), "applex3", "slot 0");
        h.assertValueEqual(name(chest.getItem(1)), "breadx2", "slot 1");
        h.assertValueEqual(name(chest.getItem(2)), "stonex64", "stone merged to a full stack");
        h.assertValueEqual(name(chest.getItem(3)), "stonex16", "the rest of the stone");
        h.assertTrue(chest.getItem(4).isEmpty(), "empty after the items");
        h.succeed();
    }

    @GameTest
    public void shulkersAndBundlesGoBottomRight(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.SHULKER_BOX));
        chest.setItem(1, new ItemStack(Items.BUNDLE));
        chest.setItem(2, new ItemStack(Items.ACACIA_LOG, 5));
        chest.setItem(3, new ItemStack(Items.ZOMBIE_HEAD));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        InventorySorter.sort(InventorySorter.slotsOf(menu, Region.CONTAINER), SortOrder.ALPHABETICAL, player);
        h.assertValueEqual(name(chest.getItem(0)), "acacia_logx5", "normal items stay at the top");
        h.assertValueEqual(name(chest.getItem(1)), "zombie_headx1", "normal items stay at the top, even Z");
        h.assertValueEqual(name(chest.getItem(25)), "bundlex1", "bundle among the last slots");
        h.assertValueEqual(name(chest.getItem(26)), "shulker_boxx1", "shulker box in the very last slot");
        h.succeed();
    }

    @GameTest
    public void amountPutsBiggestFirst(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.APPLE, 2));
        chest.setItem(1, new ItemStack(Items.DIRT, 30));
        chest.setItem(2, new ItemStack(Items.BREAD, 9));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        InventorySorter.sort(InventorySorter.slotsOf(menu, Region.CONTAINER), SortOrder.AMOUNT, player);
        h.assertValueEqual(name(chest.getItem(0)), "dirtx30", "biggest first");
        h.assertValueEqual(name(chest.getItem(1)), "breadx9", "then bread");
        h.assertValueEqual(name(chest.getItem(2)), "applex2", "then apple");
        h.succeed();
    }

    @GameTest
    public void creativeOrderFollowsTheTabs(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.DIAMOND));
        chest.setItem(1, new ItemStack(Items.STONE));
        chest.setItem(2, new ItemStack(Items.APPLE));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        InventorySorter.sort(InventorySorter.slotsOf(menu, Region.CONTAINER), SortOrder.CREATIVE, player);
        h.assertValueEqual(name(chest.getItem(0)), "stonex1", "building blocks come first");
        h.assertValueEqual(name(chest.getItem(1)), "applex1", "then food");
        h.assertValueEqual(name(chest.getItem(2)), "diamondx1", "then ingredients");
        h.succeed();
    }

    @GameTest
    public void playerSortNeverTouchesTheHotbar(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        player.getInventory().setItem(8, new ItemStack(Items.APPLE, 5));
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 20));
        player.getInventory().setItem(20, new ItemStack(Items.APPLE, 1));
        player.getInventory().setItem(35, new ItemStack(Items.COAL, 4));
        player.getInventory().setItem(36, new ItemStack(Items.IRON_BOOTS));
        InventoryMenu menu = player.inventoryMenu;
        List<Slot> slots = InventorySorter.slotsOf(menu, Region.PLAYER);
        h.assertValueEqual(slots.size(), 27, "only the 27 storage slots");
        InventorySorter.sort(slots, SortOrder.ALPHABETICAL, player);
        h.assertValueEqual(name(player.getInventory().getItem(0)), "stonex5", "hotbar slot 1 untouched");
        h.assertValueEqual(name(player.getInventory().getItem(8)), "applex5", "hotbar slot 9 untouched");
        h.assertValueEqual(name(player.getInventory().getItem(9)), "applex1", "storage sorted");
        h.assertValueEqual(name(player.getInventory().getItem(10)), "coalx4", "storage sorted");
        h.assertValueEqual(name(player.getInventory().getItem(11)), "stonex20", "storage sorted");
        h.assertTrue(player.getInventory().getItem(35).isEmpty(), "the old last slot is empty");
        h.assertTrue(player.getInventory().getItem(36).is(Items.IRON_BOOTS), "armor untouched");
        h.succeed();
    }

    @GameTest
    public void sortingTheContainerLeavesThePlayerAlone(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.getInventory().setItem(9, new ItemStack(Items.STONE));
        player.getInventory().setItem(10, new ItemStack(Items.APPLE));
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(4, new ItemStack(Items.BREAD));
        chest.setItem(7, new ItemStack(Items.APPLE));
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        InventorySorter.sort(InventorySorter.slotsOf(menu, Region.CONTAINER), SortOrder.ALPHABETICAL, player);
        h.assertValueEqual(name(chest.getItem(0)), "applex1", "container sorted");
        h.assertValueEqual(name(player.getInventory().getItem(9)), "stonex1", "player inventory unchanged");
        h.assertValueEqual(name(player.getInventory().getItem(10)), "applex1", "player inventory unchanged");
        h.succeed();
    }

    @GameTest
    public void regionsFollowTheCursor(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), new SimpleContainer(27));
        // A three-row chest: container slots at y 18..71, inventory rows at y 85..138, hotbar at y 143
        h.assertTrue(InventorySorter.regionAt(menu, 8 + 4 * 18 + 17, 30) == Region.CONTAINER, "gap inside the chest area");
        h.assertTrue(InventorySorter.regionAt(menu, 20, 90) == Region.PLAYER, "inventory area");
        h.assertTrue(InventorySorter.regionAt(menu, 20, 145) == null, "the hotbar is not an area");
        h.assertTrue(InventorySorter.regionAt(menu, 20, 5) == null, "the title is not an area");
        h.assertTrue(InventorySorter.regionAt(menu, 170, 30) == null, "outside the slots");
        // The plain inventory has no sortable container
        h.assertTrue(InventorySorter.slotsOf(player.inventoryMenu, Region.CONTAINER).isEmpty(), "no container in the inventory");
        h.assertTrue(InventorySorter.slotsOf(menu, Region.CONTAINER).size() == 27, "chest slots");
        h.succeed();
    }
}
