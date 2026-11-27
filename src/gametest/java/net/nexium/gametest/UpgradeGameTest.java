package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.drawer.DrawerWood;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.upgrade.UpgradeMenu;
import net.nexium.upgrade.Upgradeable;

import java.util.List;

/** The upgrade slots (four for capacity, one for the Void Upgrade), and what becomes of the old lock slot: the Lock Upgrade is the Drawer Key now. */
public class UpgradeGameTest {

    @GameTest
    public void thereAreFiveUpgradeSlotsAndTheKeyIsNotAnUpgrade(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        h.assertValueEqual(Upgradeable.UPGRADE_SLOTS, 5, "five upgrade slots: four capacity and the Void one");
        h.assertValueEqual(be.getUpgrades().size(), 5, "and a drawer has five");
        h.assertTrue(!be.acceptsUpgrade(new ItemStack(ModItems.DRAWER_KEY)), "the key is not an upgrade");
        h.assertTrue(!be.tryInstall(new ItemStack(ModItems.DRAWER_KEY)), "it does not install");
        h.assertTrue(be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE)), "iron installs");
        h.assertTrue(be.getUpgrades().get(0).is(ModItems.IRON_UPGRADE), "iron in the first slot");
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        UpgradeMenu menu = new UpgradeMenu(1, player.getInventory(), be, be);
        h.assertValueEqual(menu.slots.size(), 5 + 36, "the screen has the five slots and the inventory, no lock slot");
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.DRAWER_KEY)), "no slot takes the key");
        h.assertTrue(menu.getSlot(Upgradeable.VOID_SLOT).mayPlace(new ItemStack(ModItems.VOID_UPGRADE)), "the Void slot takes the void upgrade");
        h.assertTrue(!menu.getSlot(3).mayPlace(new ItemStack(ModItems.VOID_UPGRADE)), "a capacity slot does not");
        h.assertTrue(!menu.getSlot(Upgradeable.VOID_SLOT).mayPlace(new ItemStack(ModItems.GOLD_UPGRADE)), "and the Void slot takes no capacity upgrade");
        h.assertTrue(menu.getSlot(3).mayPlace(new ItemStack(ModItems.GOLD_UPGRADE)), "a capacity slot takes capacity upgrades");
        h.succeed();
    }

    @GameTest
    public void aSavedListHasItsOldLockUpgradesTakenOutAndCounted(GameTestHelper h) {
        NonNullList<ItemStack> upgrades = NonNullList.withSize(Upgradeable.UPGRADE_SLOTS, ItemStack.EMPTY);
        // Newer saves: four general slots, then the lock slot
        int keys = Upgradeable.loadSaved(List.of(new ItemStack(ModItems.IRON_UPGRADE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(ModItems.DRAWER_KEY)), upgrades);
        h.assertValueEqual(keys, 1, "one key in the lock slot");
        h.assertTrue(upgrades.get(0).is(ModItems.IRON_UPGRADE), "the capacity upgrade stays where it was");
        // Older saves: the lock in a general slot
        keys = Upgradeable.loadSaved(List.of(new ItemStack(ModItems.DRAWER_KEY), new ItemStack(ModItems.GOLD_UPGRADE)), upgrades);
        h.assertValueEqual(keys, 1, "one key in a general slot");
        h.assertTrue(upgrades.get(0).isEmpty(), "its slot is freed");
        h.assertTrue(upgrades.get(1).is(ModItems.GOLD_UPGRADE), "and the other upgrade is kept");
        h.assertValueEqual(Upgradeable.loadSaved(List.of(), upgrades), 0, "an empty save has no keys");
        h.assertTrue(upgrades.stream().allMatch(ItemStack::isEmpty), "and empties the slots");
        h.succeed();
    }

    @GameTest(maxTicks = 60)
    public void oldDrawerItemsWithALockUpgradeStayLockedAndDropTheKey(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ItemStack item = new ItemStack(ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        // The lock slot was the fifth: four general slots, then the lock
        NonNullList<ItemStack> old = NonNullList.withSize(5, ItemStack.EMPTY);
        old.set(0, new ItemStack(ModItems.IRON_UPGRADE));
        old.set(4, new ItemStack(ModItems.DRAWER_KEY));
        item.set(ModComponents.UPGRADES, ItemContainerContents.fromItems(old));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
        h.placeAt(player, item, pos.below(), net.minecraft.core.Direction.UP);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        h.assertTrue(be.isLocked(), "the drawer is locked");
        h.assertTrue(be.getUpgrades().get(0).is(ModItems.IRON_UPGRADE), "its capacity upgrade is kept");
        h.assertTrue(be.getUpgrades().stream().noneMatch(s -> s.is(ModItems.DRAWER_KEY)), "the key is no upgrade");
        h.succeedWhen(() -> {
            var keys = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3), e -> e.getItem().is(ModItems.DRAWER_KEY));
            h.assertTrue(keys.size() == 1 && keys.get(0).getItem().getCount() == 1, "the key it held is dropped beside it");
        });
    }

    @GameTest
    public void theVoidUpgradeHasASlotOfItsOwn(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        // Four capacity upgrades fill the four capacity slots, and the Void Upgrade still goes in
        for (int i = 0; i < 4; i++) h.assertTrue(be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE)), "capacity upgrade " + (i + 1) + " installs");
        h.assertTrue(!be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE)), "a fifth capacity upgrade has no slot");
        h.assertTrue(be.tryInstall(new ItemStack(ModItems.VOID_UPGRADE)), "but the Void Upgrade installs");
        h.assertTrue(be.getUpgrades().get(Upgradeable.VOID_SLOT).is(ModItems.VOID_UPGRADE), "in the Void slot");
        h.assertTrue(be.isVoid(), "and the drawer voids");
        h.assertTrue(!be.tryInstall(new ItemStack(ModItems.VOID_UPGRADE)), "only one fits");
        // A Void Upgrade saved in a capacity slot (before there was a Void slot) moves to the Void slot
        NonNullList<ItemStack> upgrades = NonNullList.withSize(Upgradeable.UPGRADE_SLOTS, ItemStack.EMPTY);
        Upgradeable.loadSaved(List.of(new ItemStack(ModItems.VOID_UPGRADE), new ItemStack(ModItems.IRON_UPGRADE)), upgrades);
        h.assertTrue(upgrades.get(Upgradeable.VOID_SLOT).is(ModItems.VOID_UPGRADE), "an old save's Void Upgrade moves to the Void slot");
        h.assertTrue(upgrades.get(0).isEmpty() && upgrades.get(1).is(ModItems.IRON_UPGRADE), "and the capacity slot is freed");
        h.succeed();
    }
}
