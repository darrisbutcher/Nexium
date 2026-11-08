package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.builder.BuilderItem;
import net.storagenexus.builder.BuilderService;
import net.storagenexus.builder.BuilderSettings;
import net.storagenexus.builder.BuilderTier;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The Construction Builder: Extend, Line and Exchange, the plane lock and size, the tier limit, running out of blocks,
 * blocks from a linked network, Undo (two steps, blocks returned) and its recipes.
 */
public class BuilderGameTest {
    /** The middle of a 3x3 floor at y 1, from x/z 1 to 3. */
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    /** A survival player standing out of the way, holding a builder, with an empty inventory otherwise. */
    @SuppressWarnings("removal")
    private static ServerPlayer builder(GameTestHelper h, BuilderTier tier) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        BlockPos stand = h.absolutePos(new BlockPos(6, 1, 6));
        player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuilderItem.of(tier)));
        return player;
    }

    private static void floor(GameTestHelper h, Block block) {
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 1, z), block);
    }

    private static int build(GameTestHelper h, ServerPlayer player, BlockPos rel, Direction face) {
        return BuilderService.build(h.getLevel(), player, player.getMainHandItem(), h.absolutePos(rel), face);
    }

    private static int count(GameTestHelper h, Block block, int y) {
        int n = 0;
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 5; z++) if (h.getBlockState(new BlockPos(x, y, z)).is(block)) n++;
        return n;
    }

    private static int held(ServerPlayer player, Item item) {
        int n = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(item)) n += inventory.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void extendCoversTheWholeFace(GameTestHelper h) {
        floor(h, Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.getInventory().add(new ItemStack(Items.STONE, 20));
        // Right-click the top of the middle block, through the item, as a player would
        BlockPos abs = h.absolutePos(CENTER);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        h.assertValueEqual(count(h, Blocks.STONE, 2), 9, "a stone on top of every floor block");
        h.assertValueEqual(held(player, Items.STONE), 11, "9 stone used");
        h.assertValueEqual(player.getMainHandItem().getDamageValue(), 9, "1 durability a block");
        h.succeed();
    }

    @GameTest
    public void theOffhandBlockIsPlacedAndTheTierLimits(GameTestHelper h) {
        // A 5x5 floor: 25 blocks, but an Iron builder places 9 at most
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 64));
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 9, "the Iron limit");
        h.assertValueEqual(count(h, Blocks.OAK_PLANKS, 2), 9, "planks from the offhand");
        h.assertValueEqual(player.getOffhandItem().getCount(), 55, "taken from the offhand");
        h.assertBlockPresent(Blocks.OAK_PLANKS, CENTER.above());
        h.succeed();
    }

    @GameTest
    public void planeLockAndSize(GameTestHelper h) {
        // A wall facing north, 3 wide and 3 high, clicked in the middle of its north face
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        BlockPos middle = new BlockPos(2, 2, 3);
        ServerPlayer player = builder(h, BuilderTier.DIAMOND);
        player.getInventory().add(new ItemStack(Items.STONE, 64));
        ItemStack tool = player.getMainHandItem();
        BuilderSettings.cyclePlane(tool); // flat: only sideways
        h.assertValueEqual(build(h, player, middle, Direction.NORTH), 3, "flat: one row across");
        for (int x = 1; x <= 3; x++) h.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, 2));
        clearInFront(h);
        BuilderSettings.cyclePlane(tool); // upright: only up and down
        h.assertValueEqual(build(h, player, middle, Direction.NORTH), 3, "upright: one column");
        for (int y = 1; y <= 3; y++) h.assertBlockPresent(Blocks.STONE, new BlockPos(2, y, 2));
        clearInFront(h);
        // Any way, but only 4 blocks: the nearest ones
        BuilderSettings.cyclePlane(tool);
        h.assertValueEqual(BuilderSettings.plane(tool), BuilderSettings.Plane.ANY, "back to any");
        tool.set(ModComponents.BUILDER_SIZE, 4);
        h.assertValueEqual(BuilderSettings.size(tool), 4, "size 4");
        h.assertValueEqual(build(h, player, middle, Direction.NORTH), 4, "4 of the 9");
        h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 2, 2));
        // Stepping the size: down through the list, back to the tier's limit after 1
        tool.set(ModComponents.BUILDER_SIZE, 27);
        h.assertValueEqual(BuilderSettings.cycleSize(tool), 16, "27, then 16");
        tool.set(ModComponents.BUILDER_SIZE, 1);
        h.assertValueEqual(BuilderSettings.cycleSize(tool), 27, "after 1, the Diamond limit");
        tool.set(ModComponents.BUILDER_SIZE, 500);
        h.assertValueEqual(BuilderSettings.size(tool), 27, "never above the limit");
        h.succeed();
    }

    private static void clearInFront(GameTestHelper h) {
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 2), Blocks.AIR);
    }

    @GameTest
    public void lineFollowsTheLook(GameTestHelper h) {
        floor(h, Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.getInventory().add(new ItemStack(Items.STONE, 64));
        BuilderSettings.cycleMode(player.getMainHandItem()); // Line
        player.setYRot(-90.0F); // looking east, along x
        player.setXRot(30.0F);
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 3, "one row");
        for (int x = 1; x <= 3; x++) h.assertBlockPresent(Blocks.STONE, new BlockPos(x, 2, 2));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
        h.succeed();
    }

    @GameTest
    public void exchangeSwapsTheSurfaceAndGivesTheOldBlocks(GameTestHelper h) {
        floor(h, Blocks.DIRT);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        ItemStack tool = player.getMainHandItem();
        BuilderSettings.cycleMode(tool);
        BuilderSettings.cycleMode(tool);
        h.assertValueEqual(BuilderSettings.mode(tool), BuilderSettings.Mode.EXCHANGE, "Exchange");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 9));
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 9, "the whole floor");
        h.assertValueEqual(count(h, Blocks.STONE, 1), 9, "the floor is stone");
        h.assertValueEqual(held(player, Items.DIRT), 9, "the dirt comes back");
        h.assertTrue(player.getOffhandItem().isEmpty(), "the stone is used");
        // Never bedrock
        floor(h, Blocks.BEDROCK);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 9));
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 0, "bedrock stays");
        h.succeed();
    }

    @GameTest
    public void runningOutStops(GameTestHelper h) {
        floor(h, Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.getInventory().add(new ItemStack(Items.STONE, 4));
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 4, "as many as you have");
        h.assertValueEqual(held(player, Items.STONE), 0, "all used");
        h.assertValueEqual(count(h, Blocks.STONE, 2), 4, "4 placed");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void aLinkedNetworkSuppliesBlocks(GameTestHelper h) {
        BlockPos terminal = new BlockPos(1, 1, 5), connector = new BlockPos(2, 1, 5), chestPos = new BlockPos(3, 1, 5);
        h.setBlock(terminal, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(connector, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = h.getBlockEntity(chestPos, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.STONE, 32));
        floor(h, Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.getInventory().add(new ItemStack(Items.STONE, 2));
        ItemStack link = new ItemStack(ModItems.MODULES.get(ModuleType.NEXUS_LINK));
        link.set(ModComponents.TERMINAL_LINK, GlobalPos.of(h.getLevel().dimension(), h.absolutePos(terminal)));
        BuilderSettings.setModule(player.getMainHandItem(), link);
        h.assertValueEqual(build(h, player, CENTER, Direction.UP), 9, "the whole face");
        h.assertValueEqual(held(player, Items.STONE), 0, "the inventory first");
        h.assertValueEqual(chest.getItem(0).getCount(), 25, "then 7 from the network");
        h.succeed();
    }

    @GameTest
    public void undoGoesBackTwoBuilds(GameTestHelper h) {
        floor(h, Blocks.STONE);
        ServerPlayer player = builder(h, BuilderTier.IRON);
        player.getInventory().add(new ItemStack(Items.STONE, 18));
        h.assertValueEqual(BuilderService.undo(player), -1, "nothing to undo yet");
        build(h, player, CENTER, Direction.UP);
        build(h, player, CENTER.above(), Direction.UP);
        h.assertValueEqual(count(h, Blocks.STONE, 3), 9, "two layers built");
        h.assertValueEqual(BuilderService.undoLeft(player), 2, "two builds to undo");
        // Someone mines one block of the top layer: it is skipped, the rest go
        h.setBlock(new BlockPos(1, 3, 1), Blocks.AIR);
        h.assertValueEqual(BuilderService.undo(player), 8, "the top layer, less the mined block");
        h.assertValueEqual(count(h, Blocks.STONE, 3), 0, "the top layer is gone");
        h.assertValueEqual(BuilderService.undo(player), 9, "the first layer");
        h.assertValueEqual(count(h, Blocks.STONE, 2), 0, "back to the floor");
        h.assertValueEqual(held(player, Items.STONE), 17, "every block still there is returned");
        h.assertValueEqual(BuilderService.undo(player), -1, "only two steps");
        // Only the last two builds are kept (three builds of one block each)
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuilderItem.of(BuilderTier.IRON)));
        for (int i = 0; i < 3; i++) {
            player.getInventory().add(new ItemStack(Items.STONE, 1));
            BlockPos at = new BlockPos(1 + i, 1, 1);
            build(h, player, at, Direction.UP);
        }
        h.assertValueEqual(BuilderService.undoLeft(player), 2, "at most two");
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static ItemStack smith(GameTestHelper h, ItemStack base, ItemStack addition) {
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, addition);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void recipesUpgradeAndKeepSettings(GameTestHelper h) {
        ItemStack paper = new ItemStack(Items.PAPER), lapis = new ItemStack(Items.LAPIS_LAZULI), iron = new ItemStack(Items.IRON_INGOT);
        ItemStack ironBuilder = craft(h, List.of(paper, lapis, paper, iron, new ItemStack(Items.GOLD_NUGGET), iron, iron, ItemStack.EMPTY, iron));
        h.assertTrue(ironBuilder.is(BuilderItem.of(BuilderTier.IRON)), "paper, lapis, gold nugget and iron make an Iron builder");
        BuilderSettings.cycleMode(ironBuilder);
        ItemStack diamond = craft(h, List.of(ironBuilder, new ItemStack(Items.DIAMOND)));
        h.assertTrue(diamond.is(BuilderItem.of(BuilderTier.DIAMOND)), "a diamond upgrades it");
        h.assertValueEqual(BuilderSettings.mode(diamond), BuilderSettings.Mode.LINE, "its mode carries over");
        ItemStack netherite = smith(h, diamond, new ItemStack(Items.NETHERITE_INGOT));
        h.assertTrue(netherite.is(BuilderItem.of(BuilderTier.NETHERITE)), "a netherite ingot at the smithing table");
        ItemStack nexium = smith(h, netherite, new ItemStack(ModItems.NEXIUM_INGOT));
        h.assertTrue(nexium.is(BuilderItem.of(BuilderTier.NEXIUM)), "a Nexium ingot");
        h.assertTrue(!nexium.isDamageableItem(), "the Nexium builder never breaks");
        h.assertValueEqual(BuilderSettings.size(nexium), 128, "and places up to 128");
        h.succeed();
    }
}
