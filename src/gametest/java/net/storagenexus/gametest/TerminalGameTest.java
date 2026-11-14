package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.terminal.TerminalActionPayload;
import net.storagenexus.terminal.TerminalMenu;

/** Terminal actions, recipe-book placement from the network, and fluid drawer capacity. */
public class TerminalGameTest {
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1);
    private static final BlockPos CONNECTOR = new BlockPos(2, 1, 1);
    private static final BlockPos CHEST = new BlockPos(3, 1, 1);

    /** A crafting terminal whose network is one chest holding the given planks. */
    private static TerminalMenu craftingTerminal(GameTestHelper h, ServerPlayer player, int planks) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        for (int i = 0; planks > 0; i++) {
            int n = Math.min(64, planks);
            chest.setItem(i, new ItemStack(Items.OAK_PLANKS, n));
            planks -= n;
        }
        TerminalMenu menu = new TerminalMenu(1, player.getInventory(), true, h.getLevel(), h.absolutePos(TERMINAL), p -> true, null);
        player.containerMenu = menu;
        return menu;
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        return player;
    }

    private static RecipeHolder<?> recipe(GameTestHelper h, String id) {
        return h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).orElseThrow();
    }

    private static int count(ChestBlockEntity chest) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) n += chest.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void recipeBookFillsGridFromNetwork(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 10);
        var action = menu.handlePlacement(false, false, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        h.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.NOTHING, "placement result");
        int placed = 0;
        for (var slot : menu.getInputGridSlots()) if (slot.getItem().is(Items.OAK_PLANKS)) placed += slot.getItem().getCount();
        h.assertValueEqual(placed, 4, "planks placed in the grid");
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 6, "planks left in the network");
        h.assertTrue(menu.getResultSlot().getItem().is(Items.CRAFTING_TABLE), "result shows a crafting table");
        h.succeed();
    }

    @GameTest
    public void recipeBookMaxFillsAsManyAsPossible(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 10);
        menu.handlePlacement(true, false, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        for (var slot : menu.getInputGridSlots()) {
            if (!slot.getItem().isEmpty()) h.assertValueEqual(slot.getItem().getCount(), 2, "planks per grid slot");
        }
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 2, "planks left in the network");
        h.succeed();
    }

    @GameTest
    public void recipeBookShowsGhostWhenIngredientsMissing(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 3);
        var action = menu.handlePlacement(false, false, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        h.assertValueEqual(action, RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "placement result");
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 3, "nothing taken");
        h.succeed();
    }

    @GameTest
    public void shiftCraftingKeepsWhatTheInventoryHasNoRoomFor(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 0);
        // Room for exactly 2 more planks: every slot full of dirt but one, which holds 62 planks
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(10, new ItemStack(Items.OAK_PLANKS, 62));
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_LOG, 1));
        menu.slotsChanged(menu.getInputGridSlots().get(0).container);
        h.assertValueEqual(menu.getResultSlot().getItem().getCount(), 4, "a log makes 4 planks");
        menu.quickMoveStack(player, 0);
        h.assertValueEqual(player.getInventory().getItem(10).getCount(), 64, "2 planks fit in the inventory");
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 2, "the other 2 go into the network, not nowhere");
        h.assertTrue(menu.getInputGridSlots().get(0).getItem().isEmpty(), "the log is used");
        h.succeed();
    }

    @GameTest
    public void shiftCraftingStopsAtTwoStacksAClick(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 1000);
        // Sticks: two planks, one above the other, make 4. The grid refills from the network after every craft
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS, 1));
        menu.getInputGridSlots().get(3).set(new ItemStack(Items.OAK_PLANKS, 1));
        menu.slotsChanged(menu.getInputGridSlots().get(0).container);
        h.assertValueEqual(menu.getResultSlot().getItem().getCount(), 4, "two planks make 4 sticks");
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        h.assertValueEqual(player.getInventory().countItem(Items.STICK), 128, "one shift-click makes two stacks of sticks and no more");
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 1000 - 64, "using 32 crafts' worth of planks from the network");
        h.assertTrue(menu.getResultSlot().getItem().is(Items.STICK), "the next craft waits in the result slot");
        // Another click makes the next two stacks
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        h.assertValueEqual(player.getInventory().countItem(Items.STICK), 256, "a second click makes two more stacks");
        h.succeed();
    }

    @GameTest
    public void shiftCraftingCountsStacksOfWhatIsMade(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 1000);
        // A boat (5 planks) stacks to 1, so two stacks is two boats
        for (int slot : new int[]{3, 5, 6, 7, 8}) menu.getInputGridSlots().get(slot).set(new ItemStack(Items.OAK_PLANKS, 1));
        menu.slotsChanged(menu.getInputGridSlots().get(3).container);
        h.assertTrue(menu.getResultSlot().getItem().is(Items.OAK_BOAT), "five planks make a boat");
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        h.assertValueEqual(player.getInventory().countItem(Items.OAK_BOAT), 2, "a boat stacks to 1, so one shift-click makes two");
        h.succeed();
    }

    @GameTest
    public void aPlainClickOnTheResultStillCraftsOnce(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 1000);
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS, 1));
        menu.getInputGridSlots().get(3).set(new ItemStack(Items.OAK_PLANKS, 1));
        menu.slotsChanged(menu.getInputGridSlots().get(0).container);
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 4, "a plain click takes one craft onto the cursor");
        h.succeed();
    }

    @GameTest
    public void rightClickTakesOneItem(GameTestHelper h) {
        ServerPlayer player = player(h);
        TerminalMenu menu = craftingTerminal(h, player, 10);
        menu.handleAction(player, new TerminalActionPayload(TerminalActionPayload.TAKE_ONE, new ItemStack(Items.OAK_PLANKS), Fluids.EMPTY));
        h.assertValueEqual(menu.getCarried().getCount(), 1, "items on the cursor");
        h.assertValueEqual(count(h.getBlockEntity(CHEST, ChestBlockEntity.class)), 9, "planks left in the network");
        h.succeed();
    }

    @GameTest
    public void fluidDrawerHoldsThirtyTwoBuckets(GameTestHelper h) {
        h.setBlock(CONNECTOR, ModBlocks.FLUID_DRAWER_1X1);
        FluidDrawerBlockEntity drawer = h.getBlockEntity(CONNECTOR, FluidDrawerBlockEntity.class);
        long want = 200 * FluidConstants.BUCKET;
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(drawer.getFluidStorage().insert(FluidVariant.of(Fluids.WATER), want, tx),
                    32 * FluidConstants.BUCKET, "water accepted by a fluid drawer");
        }
        h.succeed();
    }

    @GameTest
    public void terminalDepositsAndPickups(GameTestHelper h) {
        BlockPos t = new BlockPos(1, 1, 1), c = new BlockPos(2, 1, 1), chest = new BlockPos(3, 1, 1);
        h.setBlock(t, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(c, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(chest, Blocks.CHEST);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        TerminalMenu menu = new TerminalMenu(1, player.getInventory(), true, h.getLevel(), h.absolutePos(t), p -> true, null);
        player.containerMenu = menu;
        ChestBlockEntity box = h.getBlockEntity(chest, ChestBlockEntity.class);
        java.util.function.IntSupplier inChest = () -> {
            int n = 0;
            for (int i = 0; i < box.getContainerSize(); i++) n += box.getItem(i).getCount();
            return n;
        };
        // Right-click while holding a stack: one goes in
        menu.setCarried(new ItemStack(Items.OAK_PLANKS, 64));
        menu.handleAction(player, new TerminalActionPayload(TerminalActionPayload.INSERT_ONE, ItemStack.EMPTY, Fluids.EMPTY));
        h.assertValueEqual(inChest.getAsInt(), 1, "after depositing one");
        h.assertValueEqual(menu.getCarried().getCount(), 63, "left on the cursor");
        // Left-click: the whole stack goes in
        menu.handleAction(player, new TerminalActionPayload(TerminalActionPayload.INSERT_CARRIED, ItemStack.EMPTY, Fluids.EMPTY));
        h.assertValueEqual(inChest.getAsInt(), 64, "after depositing the rest");
        h.assertTrue(menu.getCarried().isEmpty(), "cursor empty");
        // Left-click an item: a full stack; right-click: one
        menu.handleAction(player, new TerminalActionPayload(TerminalActionPayload.TAKE_STACK, new ItemStack(Items.OAK_PLANKS), Fluids.EMPTY));
        h.assertValueEqual(menu.getCarried().getCount(), 64, "picked up a full stack");
        menu.setCarried(ItemStack.EMPTY);
        h.setBlock(chest, Blocks.CHEST);
        h.getBlockEntity(chest, ChestBlockEntity.class).setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        menu.handleAction(player, new TerminalActionPayload(TerminalActionPayload.TAKE_ONE, new ItemStack(Items.OAK_PLANKS), Fluids.EMPTY));
        h.assertValueEqual(menu.getCarried().getCount(), 1, "picked up one");
        h.succeed();
    }
}
