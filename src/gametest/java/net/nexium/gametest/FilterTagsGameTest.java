package net.nexium.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.SideConfig;
import net.nexium.filter.FilterData;
import net.nexium.filter.FilterData.Special;
import net.nexium.filter.FilterMenu;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The Advanced Filter's tags and special matches: chosen item tags, "smeltable" and "furnace fuel", each as a whitelist or a blacklist,
 * saved and sent, set through the menu with checks, and working on the insert side of a conduit that feeds a furnace.
 */
public class FilterTagsGameTest {
    private static boolean passes(FilterData filter, Item item) {
        return filter.matches(ItemVariant.of(item));
    }

    private static FilterData advanced() {
        return FilterData.empty(true);
    }

    @GameTest
    public void aChosenTagMatchesItsMembersAsAWhitelistOrABlacklist(GameTestHelper h) {
        FilterData logs = advanced().withTags(List.of(Identifier.parse("minecraft:logs")));
        h.assertTrue(passes(logs, Items.OAK_LOG) && passes(logs, Items.CRIMSON_STEM), "the logs tag lets an oak log and a crimson stem through");
        h.assertTrue(!passes(logs, Items.STONE) && !passes(logs, Items.OAK_PLANKS), "but not stone or planks");
        FilterData not = logs.withWhitelist(false);
        h.assertTrue(!passes(not, Items.OAK_LOG) && passes(not, Items.STONE), "as a blacklist it is the other way round");
        // Several tags: anything in any of them
        FilterData both = advanced().withTags(List.of(Identifier.parse("minecraft:logs"), Identifier.parse("minecraft:coals")));
        h.assertTrue(passes(both, Items.OAK_LOG) && passes(both, Items.COAL) && !passes(both, Items.DIAMOND), "any of several tags");
        h.succeed();
    }

    @GameTest
    public void smeltableAndFuelAreDecidedByTheGame(GameTestHelper h) {
        FilterData smelt = advanced().withSpecial(Special.SMELTABLE);
        h.assertTrue(passes(smelt, Items.RAW_IRON) && passes(smelt, Items.COBBLESTONE) && passes(smelt, Items.BEEF) && passes(smelt, Items.SAND),
                "ore, stone, raw food and sand can be smelted");
        h.assertTrue(!passes(smelt, Items.COAL) && !passes(smelt, Items.DIAMOND) && !passes(smelt, Items.STICK), "coal, a diamond and a stick cannot");
        FilterData fuel = advanced().withSpecial(Special.FUEL);
        h.assertTrue(passes(fuel, Items.COAL) && passes(fuel, Items.STICK) && passes(fuel, Items.LAVA_BUCKET), "coal, a stick and lava burn");
        h.assertTrue(!passes(fuel, Items.RAW_IRON) && !passes(fuel, Items.DIAMOND) && !passes(fuel, Items.COBBLESTONE), "ore, a diamond and cobblestone do not");
        // Something that is both: a log is smelted into charcoal and burns
        h.assertTrue(passes(smelt, Items.OAK_LOG) && passes(fuel, Items.OAK_LOG), "a log is both");
        // Blacklists
        h.assertTrue(!passes(smelt.withWhitelist(false), Items.RAW_IRON) && passes(smelt.withWhitelist(false), Items.DIAMOND), "smeltable as a blacklist");
        h.assertTrue(!passes(fuel.withWhitelist(false), Items.COAL) && passes(fuel.withWhitelist(false), Items.RAW_IRON), "fuel as a blacklist");
        h.succeed();
    }

    @GameTest
    public void entriesTagsAndTheSpecialAddUp(GameTestHelper h) {
        FilterData all = advanced().withEntries(List.of(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY))
                .withTags(List.of(Identifier.parse("minecraft:planks"))).withSpecial(Special.FUEL);
        h.assertTrue(passes(all, Items.DIAMOND) && passes(all, Items.OAK_PLANKS) && passes(all, Items.COAL), "the entry, the tag and the special each let things through");
        h.assertTrue(!passes(all, Items.STONE), "nothing else does");
        // Nothing chosen is the old empty filter: an empty whitelist lets nothing through, an empty blacklist everything
        h.assertTrue(!passes(advanced(), Items.STONE) && passes(advanced().withWhitelist(false), Items.STONE), "an empty filter is as it was");
        h.succeed();
    }

    @GameTest
    public void tagsAndTheSpecialSurviveSavingAndTheNetwork(GameTestHelper h) {
        FilterData data = advanced().withTags(List.of(Identifier.parse("minecraft:logs"), Identifier.parse("c:ingots"))).withSpecial(Special.SMELTABLE).withWhitelist(false);
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        FilterData saved = FilterData.CODEC.parse(ops, FilterData.CODEC.encodeStart(ops, data).getOrThrow()).getOrThrow();
        h.assertTrue(saved.equals(data) && saved.special() == Special.SMELTABLE && saved.tags().size() == 2 && !saved.whitelist(), "a save and load keeps them");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        FilterData.STREAM_CODEC.encode(buf, data);
        h.assertTrue(FilterData.STREAM_CODEC.decode(buf).equals(data), "and so does the network");
        // A filter saved before tags and the special existed has neither
        var json = FilterData.CODEC.encodeStart(ops, advanced()).getOrThrow().getAsJsonObject();
        json.remove("tags");
        json.remove("special");
        FilterData old = FilterData.CODEC.parse(ops, json).getOrThrow();
        h.assertTrue(old.tags().isEmpty() && old.special() == Special.NONE, "an older filter has no tags and no special");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void theMenuKeepsOnlyRealTagsOnceEachUpToTheLimit(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack filter = new ItemStack(ModItems.ADVANCED_FILTER);
        player.setItemInHand(InteractionHand.MAIN_HAND, filter);
        FilterMenu menu = new FilterMenu(1, player.getInventory(), InteractionHand.MAIN_HAND.ordinal());
        h.assertTrue(menu.advanced(), "an Advanced Filter's menu knows it is one");
        menu.setTags(List.of(Identifier.parse("minecraft:logs"), Identifier.parse("minecraft:logs"), Identifier.parse("nexium:not_a_tag"), Identifier.parse("minecraft:planks")));
        List<Identifier> kept = player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER).tags();
        h.assertTrue(kept.equals(List.of(Identifier.parse("minecraft:logs"), Identifier.parse("minecraft:planks"))), "repeats and unknown tags are dropped: " + kept);
        // A cap on how many
        List<Identifier> many = new ArrayList<>();
        for (var tag : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags().toList()) many.add(tag.key().location());
        h.assertTrue(many.size() > FilterData.MAX_TAGS, "there are more tags than a filter holds: " + many.size());
        menu.setTags(many);
        h.assertValueEqual(player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER).tags().size(), FilterData.MAX_TAGS, "no more than the limit are kept");
        // The Match button steps through the special matches
        menu.clickMenuButton(player, FilterMenu.BTN_SPECIAL);
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER).special() == Special.SMELTABLE, "Match goes to smeltable");
        menu.clickMenuButton(player, FilterMenu.BTN_SPECIAL);
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER).special() == Special.FUEL, "then fuel");
        menu.clickMenuButton(player, FilterMenu.BTN_SPECIAL);
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER).special() == Special.NONE, "then back to items and tags");
        // A Basic Filter takes neither
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BASIC_FILTER));
        FilterMenu basic = new FilterMenu(2, player.getInventory(), InteractionHand.MAIN_HAND.ordinal());
        basic.setTags(List.of(Identifier.parse("minecraft:logs")));
        basic.clickMenuButton(player, FilterMenu.BTN_SPECIAL);
        FilterData basicData = player.getItemInHand(InteractionHand.MAIN_HAND).get(ModComponents.FILTER);
        h.assertTrue(basicData == null || (basicData.tags().isEmpty() && basicData.special() == Special.NONE), "a Basic Filter ignores tags and the special match");
        h.succeed();
    }

    @GameTest(maxTicks = 400)
    public void oneChestFeedsAFurnaceItsInputAndItsFuel(GameTestHelper h) {
        // Chest A - bundle 1 - bundle 2 - bundle 3, with the furnace under bundle 1 (its top) and beside bundle 3 (its side)
        BlockPos a = new BlockPos(1, 2, 2), b1 = new BlockPos(2, 2, 2), b2 = new BlockPos(3, 2, 2), b3 = new BlockPos(3, 1, 2), furnace = new BlockPos(2, 1, 2);
        h.setBlock(a, Blocks.CHEST);
        h.setBlock(furnace, Blocks.FURNACE);
        h.setBlock(b1, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(b2, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(b3, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity one = h.getBlockEntity(b1, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity two = h.getBlockEntity(b2, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity three = h.getBlockEntity(b3, ConduitBundleBlockEntity.class);
        for (ConduitBundleBlockEntity be : List.of(one, two, three)) be.addConduit(ConduitType.ITEM, 1);
        // The chest only extracts; the furnace's top takes smeltable items only, its side takes fuel only (the same colour all round)
        SideConfig fromChest = one.config(ConduitType.ITEM, Direction.WEST);
        fromChest.mode = SideConfig.Mode.EXTRACT;
        SideConfig top = one.config(ConduitType.ITEM, Direction.DOWN);
        top.mode = SideConfig.Mode.INSERT;
        top.filter = filterStack(advanced().withSpecial(Special.SMELTABLE));
        SideConfig side = three.config(ConduitType.ITEM, Direction.WEST);
        side.mode = SideConfig.Mode.INSERT;
        side.filter = filterStack(advanced().withSpecial(Special.FUEL));
        one.changed();
        two.changed();
        three.changed();
        ChestBlockEntity chest = h.getBlockEntity(a, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        chest.setItem(1, new ItemStack(Items.COAL, 16));
        AbstractFurnaceBlockEntity f = h.getBlockEntity(furnace, AbstractFurnaceBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(chest.countItem(Items.COBBLESTONE) < 16, "cobblestone left the chest for the furnace");
            h.assertTrue(chest.countItem(Items.COAL) < 16, "coal left the chest for the furnace");
            ItemStack input = f.getItem(0);
            h.assertTrue(input.isEmpty() || input.is(Items.COBBLESTONE), "the input slot holds only what is being smelted, never coal: " + input);
            ItemStack fuel = f.getItem(1);
            h.assertTrue(fuel.isEmpty() || fuel.is(Items.COAL), "the fuel slot holds only fuel: " + fuel);
            h.assertTrue(f.getItem(1).is(Items.COAL) || f.getItem(2).is(Items.STONE) || f.getItem(0).is(Items.COBBLESTONE), "the furnace has what it needs");
        });
    }

    private static ItemStack filterStack(FilterData data) {
        ItemStack stack = new ItemStack(ModItems.ADVANCED_FILTER);
        stack.set(ModComponents.FILTER, data);
        return stack;
    }
}
