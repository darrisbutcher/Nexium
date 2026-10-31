package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.Upgradeable;

/** The lock slot and moving old Lock Upgrades into it. */
public class UpgradeGameTest {

    @GameTest
    public void lockGoesInItsOwnSlot(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        h.assertTrue(be.tryInstall(new ItemStack(ModItems.LOCK_UPGRADE)), "lock installs");
        h.assertTrue(be.getUpgrades().get(Upgradeable.LOCK_SLOT).is(ModItems.LOCK_UPGRADE), "lock in the lock slot");
        h.assertTrue(be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE)), "iron installs");
        h.assertTrue(be.getUpgrades().get(0).is(ModItems.IRON_UPGRADE), "iron in the first general slot");
        h.assertTrue(!be.acceptsUpgrade(new ItemStack(ModItems.VOID_UPGRADE), Upgradeable.LOCK_SLOT), "lock slot refuses other upgrades");
        h.assertTrue(!Upgradeable.fitsSlot(new ItemStack(ModItems.LOCK_UPGRADE), 0), "general slots refuse the lock");
        h.succeed();
    }

    @GameTest
    public void oldDrawerItemsMoveTheLockToItsSlot(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        // A drawer item from before 1.0.14: lock in the first of four slots
        ItemStack item = new ItemStack(ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        NonNullList<ItemStack> old = NonNullList.withSize(4, ItemStack.EMPTY);
        old.set(0, new ItemStack(ModItems.LOCK_UPGRADE));
        item.set(ModComponents.UPGRADES, ItemContainerContents.fromItems(old));
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
        h.placeAt(player, item, pos.below(), net.minecraft.core.Direction.UP);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        h.assertTrue(be.getUpgrades().get(0).isEmpty(), "first slot freed");
        h.assertTrue(be.getUpgrades().get(Upgradeable.LOCK_SLOT).is(ModItems.LOCK_UPGRADE), "lock moved to its slot");
        h.succeed();
    }
}
