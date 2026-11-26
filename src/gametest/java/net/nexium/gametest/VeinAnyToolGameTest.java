package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.nexium.aiot.module.VeinKey;
import net.nexium.essentials.config.NexusConfig;

/** Vein mining with the Vein Mine key held works with any tool or an empty hand, and the blacklist setting keeps blocks out of it. */
public class VeinAnyToolGameTest {
    private static ServerPlayer miner(GameTestHelper h, ItemStack tool, boolean keyHeld) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        VeinKey.set(player, keyHeld);
        return player;
    }

    private static void line(GameTestHelper h, Block block, int length) {
        for (int x = 1; x <= length; x++) h.setBlock(new BlockPos(x, 1, 1), block);
    }

    private static int left(GameTestHelper h, Block block, int length) {
        int n = 0;
        for (int x = 1; x <= length; x++) if (h.getBlockState(new BlockPos(x, 1, 1)).is(block)) n++;
        return n;
    }

    private static void mine(GameTestHelper h, ServerPlayer player) {
        player.gameMode.destroyBlock(h.absolutePos(new BlockPos(1, 1, 1)));
    }

    @GameTest
    public void aPickaxeVeinMinesWithTheKeyHeld(GameTestHelper h) {
        line(h, Blocks.COAL_ORE, 5);
        mine(h, miner(h, new ItemStack(Items.IRON_PICKAXE), true));
        h.assertValueEqual(left(h, Blocks.COAL_ORE, 5), 0, "the whole coal vein is gone with an iron pickaxe");
        h.succeed();
    }

    @GameTest
    public void blocksWithContentsAreVeinMinedAndDropWhatTheyHold(GameTestHelper h) {
        for (Block block : new Block[]{Blocks.CHEST, Blocks.HOPPER, net.nexium.registry.ModBlocks.CONDUIT_BUNDLE, net.nexium.registry.ModBlocks.IRON_FLUID_TANK}) {
            line(h, block, 4);
            ServerPlayer player = miner(h, new ItemStack(Items.IRON_PICKAXE), true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            if (block != Blocks.CHEST) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            mine(h, player);
            h.assertValueEqual(left(h, block, 4), 0, "the whole line of " + block + " is vein mined");
        }
        // A chest's items drop when it is vein mined
        line(h, Blocks.CHEST, 2);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 1), net.minecraft.world.level.block.entity.ChestBlockEntity.class))
                .setItem(0, new ItemStack(Items.DIAMOND, 3));
        mine(h, miner(h, new ItemStack(Items.IRON_AXE), true));
        int diamonds = 0;
        for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2, 1, 1))).inflate(3))) {
            if (e.getItem().is(Items.DIAMOND)) diamonds += e.getItem().getCount();
        }
        h.assertValueEqual(diamonds, 3, "the chest's three diamonds dropped");
        h.succeed();
    }

    @GameTest
    public void aPickaxeMinesOneBlockWithoutTheKey(GameTestHelper h) {
        line(h, Blocks.COAL_ORE, 5);
        mine(h, miner(h, new ItemStack(Items.IRON_PICKAXE), false));
        h.assertValueEqual(left(h, Blocks.COAL_ORE, 5), 4, "only the block mined is gone");
        h.succeed();
    }

    @GameTest
    public void anEmptyHandVeinMinesDirtButNotStone(GameTestHelper h) {
        line(h, Blocks.DIRT, 5);
        mine(h, miner(h, ItemStack.EMPTY, true));
        h.assertValueEqual(left(h, Blocks.DIRT, 5), 0, "a bare hand takes a whole patch of dirt");
        line(h, Blocks.STONE, 5);
        mine(h, miner(h, ItemStack.EMPTY, true));
        h.assertValueEqual(left(h, Blocks.STONE, 5), 4, "but stone needs a pickaxe, so only the one block went");
        h.succeed();
    }

    @GameTest
    public void aWrongToolDoesNotVeinMineABlockItIsNotMadeFor(GameTestHelper h) {
        line(h, Blocks.COAL_ORE, 5);
        mine(h, miner(h, new ItemStack(Items.IRON_SHOVEL), true));
        h.assertValueEqual(left(h, Blocks.COAL_ORE, 5), 4, "a shovel does not vein mine ore");
        h.succeed();
    }

    @GameTest
    public void anotherToolStopsAtTheConfigLimit(GameTestHelper h) {
        NexusConfig config = NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        try {
            config.aiotVeinMaxBlocks = 3;
            line(h, Blocks.COAL_ORE, 6);
            mine(h, miner(h, new ItemStack(Items.IRON_PICKAXE), true));
            h.assertValueEqual(left(h, Blocks.COAL_ORE, 6), 3, "three blocks mined, the rest left");
        } finally {
            config.aiotVeinMaxBlocks = max;
        }
        h.succeed();
    }

    @GameTest
    public void theBlacklistKeepsBlocksOutOfAVein(GameTestHelper h) {
        NexusConfig config = NexusConfig.get();
        java.util.List<String> saved = new java.util.ArrayList<>(config.veinBlacklist);
        try {
            config.veinBlacklist.clear();
            config.veinBlacklist.add("minecraft:coal_ore");
            line(h, Blocks.COAL_ORE, 5);
            mine(h, miner(h, new ItemStack(Items.IRON_PICKAXE), true));
            h.assertValueEqual(left(h, Blocks.COAL_ORE, 5), 4, "a blacklisted block is mined one at a time");

            config.veinBlacklist.clear();
            config.veinBlacklist.add("#minecraft:logs");
            line(h, Blocks.OAK_LOG, 5);
            mine(h, miner(h, new ItemStack(Items.IRON_AXE), true));
            h.assertValueEqual(left(h, Blocks.OAK_LOG, 5), 4, "a blacklisted tag covers its blocks too");

            config.veinBlacklist.clear();
            line(h, Blocks.OAK_LOG, 5);
            mine(h, miner(h, new ItemStack(Items.IRON_AXE), true));
            h.assertValueEqual(left(h, Blocks.OAK_LOG, 5), 0, "and an empty blacklist leaves vein mining alone");
        } finally {
            config.veinBlacklist.clear();
            config.veinBlacklist.addAll(saved);
        }
        h.succeed();
    }

    @GameTest
    public void aToolThatBreaksPartWayStopsTheVein(GameTestHelper h) {
        line(h, Blocks.DIRT, 6);
        ItemStack shovel = new ItemStack(Items.WOODEN_SHOVEL);
        shovel.setDamageValue(shovel.getMaxDamage() - 1);
        ServerPlayer player = miner(h, shovel, true);
        mine(h, player);
        h.assertTrue(player.getMainHandItem().isEmpty(), "the shovel broke");
        int left = left(h, Blocks.DIRT, 6);
        h.assertTrue(left > 0 && left < 6, "the vein stopped when the shovel broke, it did not go on bare handed: " + left + " of 6 left");
        h.succeed();
    }

    @GameTest
    public void theBlacklistEntryParserAcceptsBlocksAndTagsOnly(GameTestHelper h) {
        h.assertTrue("minecraft:diamond_ore".equals(NexusConfig.blacklistEntry("diamond_ore")), "a bare id gets the minecraft namespace");
        h.assertTrue("minecraft:diamond_ore".equals(NexusConfig.blacklistEntry("  Minecraft:Diamond_Ore ")), "case and spaces are tidied");
        h.assertTrue("#minecraft:logs".equals(NexusConfig.blacklistEntry("#minecraft:logs")), "a tag is kept");
        h.assertTrue(NexusConfig.blacklistEntry("not_a_block") == null, "an unknown block is refused");
        h.assertTrue(NexusConfig.blacklistEntry("") == null && NexusConfig.blacklistEntry("a:b:c") == null, "nonsense is refused");
        h.succeed();
    }
}
