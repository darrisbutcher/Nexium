package net.nexium.gametest;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.backpack.Backpacks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.sort.InventorySorter;
import net.nexium.sort.InventorySorter.Region;

/**
 * Sorting a Backpack: its 36 storage slots sort A to Z like a chest (modules never move), torches merge to the Backpack's own
 * stack limit, and sorting the inventory under an open Backpack leaves the Backpack where it is.
 */
public class BackpackSortGameTest {
    private static final int FIRST = BackpackModules.SLOTS;

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        return player;
    }

    private static ItemStack backpackWith(BackpackModuleType... types) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        List<ItemStack> modules = new ArrayList<>();
        for (BackpackModuleType type : types) modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(type)));
        while (modules.size() < BackpackModules.SLOTS) modules.add(ItemStack.EMPTY);
        BackpackModules.setSlots(backpack, modules);
        return backpack;
    }

    /** The worn Backpack, opened as the player's current menu so the sort key can reach it. */
    private static BackpackMenu openWorn(ServerPlayer player, ItemStack backpack) {
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        BackpackMenu menu = new BackpackMenu(1, player.getInventory(), Backpacks.EQUIPPED);
        player.containerMenu = menu;
        return menu;
    }

    private static String name(ItemStack s) {
        return s.isEmpty() ? "-" : s.getItem().toString().replace("minecraft:", "") + "x" + s.getCount();
    }

    private static String stored(ItemStack backpack, int slot) {
        return name(BackpackContents.read(backpack).get(slot));
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBackpackSortsAToZAndSavesIt(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        BackpackMenu menu = openWorn(player, backpack);
        menu.getSlot(FIRST).set(new ItemStack(Items.STONE, 40));
        menu.getSlot(FIRST + 5).set(new ItemStack(Items.APPLE, 3));
        menu.getSlot(FIRST + 9).set(new ItemStack(Items.STONE, 40));
        menu.getSlot(FIRST + 20).set(new ItemStack(Items.SHULKER_BOX));
        menu.getSlot(FIRST + 35).set(new ItemStack(Items.BREAD, 2));

        h.assertTrue(InventorySorter.sort(player, 1, false), "something was sorted");
        h.assertValueEqual(name(menu.getSlot(FIRST).getItem()), "applex3", "slot 0 in the open menu");
        h.assertValueEqual(name(menu.getSlot(FIRST + 1).getItem()), "breadx2", "slot 1");
        h.assertValueEqual(name(menu.getSlot(FIRST + 2).getItem()), "stonex80", "stone merged into one stack (the Backpack stacks to 1,024)");
        h.assertTrue(menu.getSlot(FIRST + 3).getItem().isEmpty(), "empty after the items");
        h.assertValueEqual(name(menu.getSlot(FIRST + 35).getItem()), "shulker_boxx1", "the shulker box in the last slot");
        // The same order is what is saved in the Backpack
        h.assertValueEqual(stored(backpack, 0), "applex3", "saved slot 0");
        h.assertValueEqual(stored(backpack, 2), "stonex80", "saved slot 2");
        h.assertValueEqual(stored(backpack, 35), "shulker_boxx1", "saved last slot");
        // The module stayed where it was
        h.assertTrue(menu.getSlot(0).getItem().is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)), "the module is still in its slot");
        h.assertTrue(BackpackModules.find(backpack, BackpackModuleType.FEEDING).is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)), "and still saved");
        // Sorted already: nothing to do
        h.assertTrue(!InventorySorter.sort(player, 1, false), "sorting a sorted Backpack changes nothing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void sortingTheBackpackLeavesTheInventoryAndModulesAlone(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.getInventory().setItem(9, new ItemStack(Items.STONE));
        player.getInventory().setItem(10, new ItemStack(Items.APPLE));
        ItemStack module = new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.TORCH_STACK));
        BackpackMenu menu = openWorn(player, backpackWith(BackpackModuleType.TORCH_STACK));
        menu.getSlot(FIRST + 7).set(new ItemStack(Items.COAL, 2));
        menu.getSlot(FIRST + 3).set(new ItemStack(Items.BREAD));
        InventorySorter.sort(player, 1, false);
        h.assertValueEqual(name(menu.getSlot(FIRST).getItem()), "breadx1", "the Backpack sorted");
        h.assertValueEqual(name(player.getInventory().getItem(9)), "stonex1", "the inventory is unchanged");
        h.assertValueEqual(name(player.getInventory().getItem(10)), "applex1", "the inventory is unchanged");
        h.assertTrue(menu.getSlot(0).getItem().is(module.getItem()), "the module was not sorted away");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void everyStackableItemMergesTo1024(GameTestHelper h) {
        ServerPlayer player = player(h);
        BackpackMenu menu = openWorn(player, backpackWith());
        menu.getSlot(FIRST + 2).set(new ItemStack(Items.TORCH, 600));
        menu.getSlot(FIRST + 14).set(new ItemStack(Items.TORCH, 600));
        menu.getSlot(FIRST + 30).set(new ItemStack(Items.TORCH, 900));
        menu.getSlot(FIRST + 31).set(new ItemStack(Items.ENDER_PEARL, 40));
        menu.getSlot(FIRST + 32).set(new ItemStack(Items.ENDER_PEARL, 40));
        InventorySorter.sort(player, 1, false);
        h.assertValueEqual(name(menu.getSlot(FIRST).getItem()), "ender_pearlx80", "80 ender pearls make one stack, though they stack to 16 outside");
        h.assertValueEqual(name(menu.getSlot(FIRST + 1).getItem()), "torchx1024", "2,100 torches make a stack of 1,024");
        h.assertValueEqual(name(menu.getSlot(FIRST + 2).getItem()), "torchx1024", "another");
        h.assertValueEqual(name(menu.getSlot(FIRST + 3).getItem()), "torchx52", "and the rest");
        h.assertTrue(menu.getSlot(FIRST + 4).getItem().isEmpty(), "nothing else");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBackpackOpenedFromTheInventoryStaysPut(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith();
        player.getInventory().setItem(12, backpack);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 20));
        player.getInventory().setItem(20, new ItemStack(Items.APPLE));
        player.getInventory().setItem(30, new ItemStack(Items.COAL, 4));
        player.getInventory().setItem(2, new ItemStack(Items.DIAMOND, 3));
        BackpackMenu menu = new BackpackMenu(1, player.getInventory(), 12);
        player.containerMenu = menu;

        h.assertTrue(InventorySorter.sort(player, 1, true), "the inventory sorted");
        h.assertTrue(player.getInventory().getItem(12) == backpack, "the Backpack is still in slot 12, the same stack");
        h.assertTrue(menu.stillValid(player), "so the screen stays open");
        h.assertValueEqual(name(player.getInventory().getItem(9)), "applex1", "the items around it sorted (slot 9)");
        h.assertValueEqual(name(player.getInventory().getItem(10)), "coalx4", "slot 10");
        h.assertValueEqual(name(player.getInventory().getItem(11)), "stonex20", "slot 11");
        h.assertTrue(player.getInventory().getItem(13).isEmpty(), "and nothing was put after the Backpack's slot");
        h.assertValueEqual(name(player.getInventory().getItem(2)), "diamondx3", "the hotbar was not touched");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theAreasAreTheStorageSlotsAndTheInventory(GameTestHelper h) {
        ServerPlayer player = player(h);
        BackpackMenu menu = openWorn(player, backpackWith());
        List<Slot> storage = InventorySorter.slotsOf(menu, Region.CONTAINER);
        h.assertValueEqual(storage.size(), BackpackContents.SIZE, "the area is the 36 storage slots");
        for (Slot slot : storage) h.assertTrue(menu.slots.indexOf(slot) >= FIRST, "a module slot is not part of it");
        h.assertValueEqual(InventorySorter.slotsOf(menu, Region.PLAYER).size(), 27, "the inventory is the 27 storage slots under it");
        // Storage rows at x 8..169, y 18..89; the module slots at x 182; the inventory from y 104
        h.assertTrue(InventorySorter.regionAt(menu, 30, 40) == Region.CONTAINER, "over the storage slots");
        h.assertTrue(InventorySorter.regionAt(menu, 8 + 4 * 18 + 17, 30) == Region.CONTAINER, "a gap between its slots");
        h.assertTrue(InventorySorter.regionAt(menu, BackpackMenu.MODULE_X + 4, BackpackMenu.MODULE_Y + 4) == null, "a module slot is not an area");
        h.assertTrue(InventorySorter.regionAt(menu, 30, BackpackMenu.INVENTORY_Y + 20) == Region.PLAYER, "over the inventory");
        h.assertTrue(InventorySorter.regionAt(menu, 30, BackpackMenu.INVENTORY_Y + 62) == null, "the hotbar is not an area");
        h.succeed();
    }
}
