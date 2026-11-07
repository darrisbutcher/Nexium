package net.storagenexus.gametest;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerControllerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.upgrade.UpgradeMenu;
import net.storagenexus.util.StorageEvents;

/** Drawer clicks, upgrade slots, controller dumps and connector change detection. */
public class UsabilityGameTest {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static Object lastResult;
    private static String debug = "";

    private static DrawerBlockEntity drawerWithCobble(GameTestHelper h, int count) {
        h.setBlock(POS, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]); // faces north
        DrawerBlockEntity be = h.getBlockEntity(POS, DrawerBlockEntity.class);
        try (Transaction tx = Transaction.openOuter()) {
            be.getItemStorage().insert(ItemVariant.of(Items.COBBLESTONE), count, tx);
            tx.commit();
        }
        return be;
    }

    /** Joining gives the guide; tests want empty hands. */
    private static ServerPlayer emptyHandedPlayer(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        return player;
    }

    /** A real right-click through the server's interaction logic. */
    private static void rightClick(GameTestHelper h, ServerPlayer player, BlockPos rel, Direction face) {
        BlockPos abs = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
        lastResult = player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    private static void attack(GameTestHelper h, ServerPlayer player) {
        AttackBlockCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, h.absolutePos(POS), Direction.NORTH);
    }

    @GameTest
    public void sideClickOpensUpgradesEvenWithOffhandItem(GameTestHelper h) {
        drawerWithCobble(h, 10);
        ServerPlayer player = emptyHandedPlayer(h);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        {
            BlockPos abs = h.absolutePos(POS);
            var st = h.getLevel().getBlockState(abs);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.EAST, 0.5), Direction.EAST, abs, false);
            var r1 = st.useItemOn(ItemStack.EMPTY, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            var r2 = st.useWithoutItem(h.getLevel(), player, hit);
            debug = "mode=" + player.gameMode.getGameModeForPlayer() + " sneak=" + player.isSecondaryUseActive() + " main=" + player.getMainHandItem() + " off=" + player.getOffhandItem() + " useItemOn=" + r1 + " useWithoutItem=" + r2 + " block=" + st + " be=" + h.getLevel().getBlockEntity(abs) + " menuAfterDirect=" + player.containerMenu.getClass().getSimpleName();
            player.closeContainer();
        }
        rightClick(h, player, POS, Direction.EAST);
        h.assertTrue(player.containerMenu instanceof UpgradeMenu, "side click opened the upgrade slots (" + debug + "; result " + lastResult + ", menu " + player.containerMenu.getClass().getSimpleName() + ")");
        h.succeed();
    }

    @GameTest
    public void sneakClickOnFrontOpensUpgrades(GameTestHelper h) {
        drawerWithCobble(h, 10);
        ServerPlayer player = emptyHandedPlayer(h);
        player.setShiftKeyDown(true);
        rightClick(h, player, POS, Direction.NORTH);
        h.assertTrue(player.containerMenu instanceof UpgradeMenu, "sneak + empty hand opened the upgrade slots");
        h.succeed();
    }

    @GameTest
    public void frontClickWithEmptyHandDoesNotOpenUpgrades(GameTestHelper h) {
        drawerWithCobble(h, 10);
        ServerPlayer player = emptyHandedPlayer(h);
        rightClick(h, player, POS, Direction.NORTH);
        h.assertTrue(!(player.containerMenu instanceof UpgradeMenu), "front click stays a drawer click");
        h.succeed();
    }

    @GameTest
    public void doubleClickWithEmptyHandInsertsAll(GameTestHelper h) {
        DrawerBlockEntity be = drawerWithCobble(h, 1);
        ServerPlayer player = emptyHandedPlayer(h);
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 64));
        rightClick(h, player, POS, Direction.NORTH);
        rightClick(h, player, POS, Direction.NORTH);
        h.assertValueEqual(be.slot(0).getAmount(), 65L, "cobble in the drawer after a double-click");
        h.succeed();
    }

    @GameTest
    public void controllerDoubleClickWithEmptyHandDumpsInventory(GameTestHelper h) {
        DrawerBlockEntity be = drawerWithCobble(h, 1);
        BlockPos controller = POS.east();
        h.setBlock(controller, ModBlocks.DRAWER_CONTROLLER);
        ServerPlayer player = emptyHandedPlayer(h);
        player.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 64));
        rightClick(h, player, controller, Direction.UP);
        rightClick(h, player, controller, Direction.UP);
        h.assertValueEqual(be.slot(0).getAmount(), 65L, "cobble sent to the drawer by the controller");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void controllerReachOutlineNeedsSneakRightClick(GameTestHelper h) {
        BlockPos controller = POS.east();
        h.setBlock(controller, ModBlocks.DRAWER_CONTROLLER);
        DrawerControllerBlockEntity be = h.getBlockEntity(controller, DrawerControllerBlockEntity.class);
        ServerPlayer player = emptyHandedPlayer(h);
        rightClick(h, player, controller, Direction.UP);
        h.assertFalse(be.rangeShown(h.getLevel()), "a plain right-click only tells the drawer count");
        h.runAfterDelay(15, () -> {
            player.setShiftKeyDown(true);
            rightClick(h, player, controller, Direction.UP);
            h.assertTrue(be.rangeShown(h.getLevel()), "sneak + right-click shows the reach");
            h.runAfterDelay(15, () -> {
                rightClick(h, player, controller, Direction.UP);
                h.assertFalse(be.rangeShown(h.getLevel()), "again hides it");
                h.succeed();
            });
        });
    }

    @GameTest(maxTicks = 40)
    public void heldAttackTakesOneStackPerClick(GameTestHelper h) {
        DrawerBlockEntity be = drawerWithCobble(h, 640);
        ServerPlayer player = emptyHandedPlayer(h);
        // One click held for 3 ticks (the client re-sends it every tick), then a second click.
        h.runAtTickTime(1, () -> attack(h, player));
        h.runAtTickTime(2, () -> attack(h, player));
        h.runAtTickTime(3, () -> attack(h, player));
        h.runAtTickTime(12, () -> attack(h, player));
        h.runAtTickTime(14, () -> {
            h.assertValueEqual(be.slot(0).getAmount(), 640L - 128, "cobble left after two clicks");
            h.succeed();
        });
    }

    @GameTest
    public void fastClicksEachTakeAStack(GameTestHelper h) {
        DrawerBlockEntity be = drawerWithCobble(h, 640);
        ServerPlayer player = emptyHandedPlayer(h);
        player.setPos(Vec3.atCenterOf(h.absolutePos(POS.north())));
        // Three real clicks arriving in the same tick: nothing is thrown away as a "held" repeat
        for (int i = 0; i < 3; i++) {
            net.storagenexus.drawer.DrawerInteraction.handleClick(player,
                    new net.storagenexus.drawer.DrawerClickPayload(h.absolutePos(POS), 0));
        }
        h.assertValueEqual(be.slot(0).getAmount(), 640L - 3 * 64, "three clicks, three stacks");
        // Out of reach does nothing
        player.setPos(Vec3.atCenterOf(h.absolutePos(POS)).add(0, 30, 0));
        net.storagenexus.drawer.DrawerInteraction.handleClick(player,
                new net.storagenexus.drawer.DrawerClickPayload(h.absolutePos(POS), 0));
        h.assertValueEqual(be.slot(0).getAmount(), 640L - 3 * 64, "too far away");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void sneakAttackTakesOneItem(GameTestHelper h) {
        DrawerBlockEntity be = drawerWithCobble(h, 640);
        ServerPlayer player = emptyHandedPlayer(h);
        player.setShiftKeyDown(true);
        h.runAtTickTime(1, () -> attack(h, player));
        h.runAtTickTime(2, () -> attack(h, player));
        h.runAtTickTime(3, () -> attack(h, player));
        h.runAtTickTime(5, () -> {
            h.assertValueEqual(be.slot(0).getAmount(), 639L, "cobble left after one sneaking click");
            h.succeed();
        });
    }

    /** Runs alone (own environment) because it watches the global storage-change counter. */
    @GameTest(environment = "storagenexus:isolated", maxTicks = 80)
    public void flowingWaterAndChestChangesDoNotRescanNetworks(GameTestHelper h) {
        BlockPos connector = new BlockPos(3, 1, 3);
        BlockPos chest = connector.east();
        h.setBlock(connector, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(chest, Blocks.CHEST);
        long[] before = new long[1];
        h.runAtTickTime(3, () -> {
            before[0] = StorageEvents.storageVersion();
            h.setBlock(connector.west(), Blocks.WATER); // flows around the connector for a while
        });
        for (int t = 4; t < 40; t++) {
            int n = t;
            h.runAtTickTime(t, () -> {
                ChestBlockEntity c = h.getBlockEntity(chest, ChestBlockEntity.class);
                c.setItem(0, new ItemStack(Items.DIRT, n)); // what a hopper does: comparator updates next door
            });
        }
        h.runAtTickTime(40, () -> {
            h.assertValueEqual(StorageEvents.storageVersion() - before[0], 0L, "network rescans caused by water and chest changes");
            h.setBlock(connector.south(), Blocks.BARREL); // a real new inventory
        });
        h.runAtTickTime(42, () -> {
            h.assertTrue(StorageEvents.storageVersion() > before[0], "placing an inventory next to a connector is noticed");
            h.succeed();
        });
    }
}
