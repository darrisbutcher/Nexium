package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.nexium.energy.ItemEnergy;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.tablet.StorageTabletItem;
import net.nexium.terminal.TerminalMenu;

/** The Crafting Storage Tablet runs on a battery cell: no cell or too little charge and it will not open; opening costs NE, and so does keeping it open. */
public class TabletPowerGameTest {
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1);

    private static ServerPlayer player(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack tablet(GameTestHelper h, long charge) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        ItemStack tablet = new ItemStack(ModItems.CRAFTING_TABLET);
        tablet.set(ModComponents.TERMINAL_LINK, GlobalPos.of(h.getLevel().dimension(), h.absolutePos(TERMINAL)));
        if (charge >= 0) {
            ItemStack cell = new ItemStack(ModItems.IRON_CELL);
            if (charge > 0) cell.set(ModComponents.BATTERY_CHARGE, charge);
            ItemEnergy.setCell(tablet, cell);
        }
        return tablet;
    }

    @GameTest
    public void withoutACellOrChargeTheTabletDoesNotOpen(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet(h, -1));
        InteractionResult none = player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(none == InteractionResult.FAIL && !(player.containerMenu instanceof TerminalMenu), "no cell: it does not open");
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet(h, StorageTabletItem.OPEN_COST - 1));
        InteractionResult low = player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(low == InteractionResult.FAIL && !(player.containerMenu instanceof TerminalMenu), "too little charge: it does not open");
        h.assertValueEqual(ItemEnergy.stored(player.getMainHandItem()), StorageTabletItem.OPEN_COST - 1, "and nothing is taken");
        h.succeed();
    }

    @GameTest
    public void openingCostsEnergyAndTheOpenScreenCostsMore(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet(h, 1_000));
        InteractionResult opened = player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(opened == InteractionResult.SUCCESS && player.containerMenu instanceof TerminalMenu, "with charge it opens the terminal");
        h.assertValueEqual(ItemEnergy.stored(player.getMainHandItem()), 1_000 - StorageTabletItem.OPEN_COST, "opening cost 100 NE");
        long before = ItemEnergy.stored(player.getMainHandItem());
        player.tickCount += 1;
        h.assertTrue(player.containerMenu.stillValid(player), "it stays open while it can pay");
        player.containerMenu.stillValid(player); // the same tick again: no second payment
        h.assertValueEqual(ItemEnergy.stored(player.getMainHandItem()), before - StorageTabletItem.UPKEEP, "a tick of the open screen costs 2 NE, once a tick");
        // Run it down: the screen closes when it cannot pay
        ItemEnergy.extract(player.getMainHandItem(), ItemEnergy.stored(player.getMainHandItem()) - 1, false);
        player.tickCount += 1;
        h.assertTrue(!player.containerMenu.stillValid(player), "with 1 NE left it can no longer pay and closes");
        h.succeed();
    }

    @GameTest
    public void anotherDimensionCostsMore(GameTestHelper h) {
        GlobalPos here = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(TERMINAL));
        GlobalPos far = GlobalPos.of(net.minecraft.world.level.Level.NETHER, h.absolutePos(TERMINAL));
        h.assertValueEqual(StorageTabletItem.openCost(h.getLevel(), here), StorageTabletItem.OPEN_COST, "the same dimension");
        h.assertValueEqual(StorageTabletItem.openCost(h.getLevel(), far), StorageTabletItem.ACROSS_COST, "another dimension costs ten times as much");
        h.succeed();
    }
}
