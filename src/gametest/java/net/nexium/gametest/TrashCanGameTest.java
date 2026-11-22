package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.trash.TrashCanMenu;
import net.nexium.network.StorageNetwork;

import java.util.List;

/**
 * The Trash Can destroys items: pipes (hoppers, droppers, conduits, buses) see an inventory that takes everything and keeps nothing, and
 * its screen has a slot that destroys what is dropped on it. It takes nothing else, never gives anything back, and a storage connector
 * never adopts it as network storage.
 */
public class TrashCanGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(POS)));
        return player;
    }

    private static TrashCanMenu menu(GameTestHelper h, ServerPlayer player) {
        return new TrashCanMenu(7, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(POS)));
    }

    private static int count(ServerPlayer player) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void everySideTakesEveryItemAndKeepsNothing(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.TRASH_CAN);
        for (Direction side : Direction.values()) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(POS), side);
            h.assertTrue(storage != null, "the " + side + " side shows an inventory");
            h.assertTrue(storage.supportsInsertion(), "which takes items");
            h.assertFalse(storage.supportsExtraction(), "and gives none back");
            try (Transaction tx = Transaction.openOuter()) {
                h.assertValueEqual(storage.insert(ItemVariant.of(Items.DIAMOND), 64, tx), 64L, "a stack goes in on " + side);
                h.assertValueEqual(storage.insert(ItemVariant.of(Items.DIRT), 100_000, tx), 100_000L, "however many there are");
                h.assertValueEqual(storage.insert(ItemVariant.of(new ItemStack(Items.DIAMOND_SWORD)), 1, tx), 1L, "tools too");
                tx.commit();
            }
            h.assertFalse(storage.iterator().hasNext(), "it holds nothing afterwards");
            try (Transaction tx = Transaction.openOuter()) {
                h.assertValueEqual(storage.extract(ItemVariant.of(Items.DIAMOND), 1, tx), 0L, "and nothing can be taken out");
            }
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 200)
    public void aHopperAboveEmptiesIntoIt(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.TRASH_CAN);
        h.setBlock(POS.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = h.getBlockEntity(POS.above(), HopperBlockEntity.class);
        // A hopper moves one item every 8 ticks, so a few items are enough to see it empty
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        hopper.setItem(1, new ItemStack(Items.DIRT, 2));
        h.succeedWhen(() -> {
            for (int i = 0; i < hopper.getContainerSize(); i++) h.assertTrue(hopper.getItem(i).isEmpty(), "the hopper's slot " + i + " is emptied into the Trash Can");
        });
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aConnectorNextToItNeverAdoptsItAsStorage(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.TRASH_CAN);
        h.setBlock(POS.above(), net.nexium.registry.ModBlocks.STORAGE_CONNECTOR);
        h.assertValueEqual(StorageNetwork.scan(h.getLevel(), h.absolutePos(POS.above()), pos -> true).inventoryCount(), 0,
                "a Trash Can is not joined to a storage network (it would destroy what is on it)");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void droppingAStackOnTheSlotDestroysItAndTheSlotStaysEmpty(GameTestHelper h) {
        ServerPlayer player = player(h);
        TrashCanMenu menu = menu(h, player);
        menu.setCarried(new ItemStack(Items.DIAMOND, 64));
        menu.clicked(TrashCanMenu.TRASH_SLOT, 0, ContainerInput.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty(), "the whole carried stack is gone");
        h.assertTrue(menu.getSlot(TrashCanMenu.TRASH_SLOT).getItem().isEmpty(), "and the slot holds nothing");
        h.assertValueEqual(count(player), 0, "nothing went to the inventory");

        menu.setCarried(new ItemStack(Items.DIRT, 64));
        menu.clicked(TrashCanMenu.TRASH_SLOT, 1, ContainerInput.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 63, "a right click destroys one item");
        menu.clicked(TrashCanMenu.TRASH_SLOT, 0, ContainerInput.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(TrashCanMenu.TRASH_SLOT).getItem().isEmpty(), "and a left click the rest");
        menu.removed(player);
        h.assertValueEqual(count(player), 0, "closing the screen gives nothing back");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aShiftClickMovesNothing(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 10));
        player.getInventory().setItem(20, new ItemStack(Items.GOLD_INGOT, 5));
        TrashCanMenu menu = menu(h, player);
        for (int slot = 1; slot < menu.slots.size(); slot++) menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, player);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 10, "a shift-click does not destroy the hotbar item");
        h.assertValueEqual(player.getInventory().getItem(20).getCount(), 5, "or the one in the inventory");
        h.assertTrue(menu.getSlot(TrashCanMenu.TRASH_SLOT).getItem().isEmpty(), "and the slot stays empty");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aRightClickOpensTheScreenAndAPickaxeMinesIt(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos pos = h.absolutePos(POS);
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.TRASH_CAN.defaultBlockState());
        BlockState state = h.getLevel().getBlockState(pos);
        state.useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        h.assertTrue(player.containerMenu instanceof TrashCanMenu, "a right-click opens the Trash Can's menu, got " + player.containerMenu.getClass().getSimpleName());
        h.assertTrue(player.containerMenu.stillValid(player), "which stays valid while the block is there and the player is near");
        h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        h.assertFalse(player.containerMenu.stillValid(player), "and not once the block is gone");
        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "a pickaxe mines it");
        h.assertTrue(state.requiresCorrectToolForDrops(), "and it needs one to drop");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aCauldronAndAHopperMakeIt(GameTestHelper h) {
        CraftingInput input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.HOPPER), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.CAULDRON), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        boolean makes = false;
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel()) && recipe.assemble(input).is(ModItems.TRASH_CAN)) makes = true;
        }
        h.assertTrue(makes, "a Cauldron and a Hopper, in any place, make a Trash Can");
        CraftingInput onlyHopper = CraftingInput.of(1, 1, List.of(new ItemStack(Items.HOPPER)));
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(onlyHopper, h.getLevel())) {
                h.assertFalse(recipe.assemble(onlyHopper).is(ModItems.TRASH_CAN), "not from a Hopper alone");
            }
        }
        h.succeed();
    }
}
