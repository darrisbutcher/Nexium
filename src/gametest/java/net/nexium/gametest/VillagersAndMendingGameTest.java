package net.nexium.gametest;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.essentials.trading.TradeCycling;
import net.nexium.util.XpMending;

import java.util.Optional;

/**
 * Mending works on any damaged item in the inventory (held or not, hotbar, main inventory and the extra slots), trades are never
 * out of stock, and the Cycle Trades key gives a villager nobody has traded with a new set of trades but never one that was
 * traded with.
 */
public class VillagersAndMendingGameTest {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(CENTER)));
        return player;
    }

    private static ItemStack damaged(ServerLevel level, net.minecraft.world.item.Item item, boolean mending, int damage) {
        ItemStack stack = new ItemStack(item);
        if (mending) {
            Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            stack.enchant(holder, 1);
        }
        stack.setDamageValue(damage);
        return stack;
    }

    // ---------------------------------------------------------------- mending

    @GameTest
    public void mendingWorksOnAnItemThatIsNotHeld(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack pickaxe = damaged(h.getLevel(), Items.IRON_PICKAXE, true, 100);
        player.getInventory().setItem(20, pickaxe); // main inventory: not held, not worn
        Optional<EnchantedItemInUse> picked = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        h.assertTrue(picked.isPresent() && picked.get().itemStack() == pickaxe, "an orb can pick the Mending pickaxe in the main inventory");
        int left = XpMending.give(player, 20);
        h.assertValueEqual(left, 0, "all 20 points went into the repair");
        h.assertValueEqual(pickaxe.getDamageValue(), 60, "20 points repair 40 durability: 100 damage became 60");
        h.succeed();
    }

    @GameTest
    public void anExperienceOrbMendsAnItemInTheInventory(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack sword = damaged(h.getLevel(), Items.DIAMOND_SWORD, true, 200);
        player.getInventory().setItem(3, sword); // hotbar slot that is not the selected one
        player.getInventory().setSelectedSlot(0);
        player.takeXpDelay = 0;
        ExperienceOrb orb = new ExperienceOrb(h.getLevel(), player.getX(), player.getY(), player.getZ(), 10);
        orb.playerTouch(player);
        h.assertValueEqual(sword.getDamageValue(), 180, "an orb of 10 repaired the sword in the hotbar by 20");
        h.assertValueEqual(player.totalExperience, 0, "and none of it became experience");
        h.succeed();
    }

    @GameTest
    public void mendingReachesTheExtraSlotsToo(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack tool = damaged(h.getLevel(), Items.IRON_SHOVEL, true, 50);
        ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, tool);
        h.assertValueEqual(XpMending.give(player, 10), 0, "the points went into the repair");
        h.assertValueEqual(tool.getDamageValue(), 30, "an item in the trinket slot was mended by 20");
        h.succeed();
    }

    @GameTest
    public void everyMendingItemIsReachableAndOnlyDamagedOnes(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack held = damaged(h.getLevel(), Items.IRON_AXE, true, 10);
        ItemStack inBag = damaged(h.getLevel(), Items.IRON_HOE, true, 10);
        ItemStack whole = damaged(h.getLevel(), Items.GOLDEN_PICKAXE, true, 0);
        ItemStack plain = damaged(h.getLevel(), Items.STONE_PICKAXE, false, 10);
        player.getInventory().setItem(0, held);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(30, inBag);
        player.getInventory().setItem(31, whole);
        player.getInventory().setItem(32, plain);
        Set<ItemStack> seen = new java.util.HashSet<>();
        for (int i = 0; i < 300; i++) {
            Optional<EnchantedItemInUse> picked = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
            h.assertTrue(picked.isPresent(), "there is always something to pick");
            seen.add(picked.get().itemStack());
        }
        h.assertTrue(seen.contains(held), "the held Mending axe is still picked");
        h.assertTrue(seen.contains(inBag), "and the one in the main inventory too");
        h.assertTrue(!seen.contains(whole), "an undamaged Mending item is never picked");
        h.assertTrue(!seen.contains(plain), "and neither is a damaged item without Mending");
        h.succeed();
    }

    // ---------------------------------------------------------------- trades

    @GameTest
    public void aTradeIsNeverOutOfStock(GameTestHelper h) {
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.APPLE), 2, 1, 0.05F);
        for (int i = 0; i < 6; i++) offer.increaseUses();
        h.assertTrue(offer.getUses() > offer.getMaxUses(), "it has been used past its limit of " + offer.getMaxUses());
        h.assertTrue(!offer.isOutOfStock(), "and is not out of stock");
        offer.setToOutOfStock();
        h.assertTrue(!offer.isOutOfStock(), "not even when something tries to lock it up");
        h.succeed();
    }

    @GameTest
    public void usingATradeNeverRaisesItsPrice(GameTestHelper h) {
        // Four emeralds, and a price multiplier that makes demand count for a lot: unchanged, heavy use would push this to the 64 emerald cap
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(Items.BREAD), 12, 5, 0.5F);
        h.assertTrue(offer.getCostA().getCount() == 4, "it starts at its base price: " + offer.getCostA().getCount());
        for (int i = 0; i < 40; i++) offer.increaseUses();
        offer.updateDemand();   // what a villager does when it restocks
        h.assertTrue(offer.getDemand() <= 0, "restocking does not raise the demand: " + offer.getDemand());
        h.assertTrue(offer.getCostA().getCount() == 4, "so a trade used 40 times costs what it did: " + offer.getCostA().getCount());
        // A trade saved with its demand already raised (from before) is priced at its base price too
        MerchantOffer raised = new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), new ItemStack(Items.BREAD), 0, 12, 5, 0.5F, 60);
        h.assertTrue(raised.getDemand() == 60, "it still holds the demand it was saved with: " + raised.getDemand());
        h.assertTrue(raised.getCostA().getCount() == 4, "but is not charged for it: " + raised.getCostA().getCount());
        // A trade with a second item is the same
        MerchantOffer two = new MerchantOffer(new ItemCost(Items.EMERALD, 3), Optional.of(new ItemCost(Items.BOOK, 2)), new ItemStack(Items.BREAD), 0, 12, 5, 0.5F, 60);
        h.assertTrue(two.getCostA().getCount() == 3 && two.getCostB().getCount() == 2, "and so is its second price: " + two.getCostA().getCount() + " and " + two.getCostB().getCount());
        h.succeed();
    }

    private static Villager farmer(GameTestHelper h, ServerPlayer player, boolean profession) {
        Villager villager = h.spawn(EntityTypes.VILLAGER, CENTER.east());
        if (profession) {
            Holder<VillagerProfession> farmer = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER);
            villager.setVillagerData(villager.getVillagerData().withProfession(farmer));
        }
        villager.getOffers();
        player.setPos(villager.position().add(0, 0, 1));
        villager.setTradingPlayer(player);
        player.containerMenu = new MerchantMenu(1, player.getInventory(), villager);
        return villager;
    }

    private static String signature(Villager villager) {
        StringBuilder sb = new StringBuilder();
        for (MerchantOffer offer : villager.getOffers()) {
            sb.append(offer.getCostA()).append('|').append(offer.getCostB()).append("->").append(offer.getResult()).append(';');
        }
        return sb.toString();
    }

    @GameTest
    public void aVillagerNobodyTradedWithCanBeCycled(GameTestHelper h) {
        ServerPlayer player = player(h);
        Villager villager = farmer(h, player, true);
        h.assertTrue(!villager.getOffers().isEmpty(), "a farmer has trades");
        h.assertTrue(TradeCycling.untraded(villager), "nobody has traded with it");
        Set<String> sets = new HashSet<>();
        sets.add(signature(villager));
        for (int i = 0; i < 40; i++) {
            h.assertTrue(TradeCycling.cycle(player) == TradeCycling.Result.CYCLED, "it cycles, time " + i);
            h.assertTrue(!villager.getOffers().isEmpty(), "and still has trades");
            sets.add(signature(villager));
        }
        h.assertTrue(sets.size() > 1, "the trades changed: " + sets.size() + " different sets in 41");
        h.assertValueEqual(villager.getVillagerData().level(), 1, "it is still a Novice");
        h.assertValueEqual(villager.getVillagerXp(), 0, "with no experience");
        h.succeed();
    }

    @GameTest
    public void aVillagerYouTradedWithKeepsItsTrades(GameTestHelper h) {
        ServerPlayer player = player(h);
        Villager villager = farmer(h, player, true);
        String before = signature(villager);
        villager.getOffers().get(0).increaseUses();
        h.assertTrue(!TradeCycling.untraded(villager), "one trade used: it has been traded with");
        h.assertTrue(TradeCycling.cycle(player) == TradeCycling.Result.ALREADY_TRADED, "so it does not cycle");
        h.assertValueEqual(signature(villager), before, "and its trades are as they were");

        ServerPlayer other = player(h);
        Villager second = farmer(h, other, true);
        second.setVillagerXp(5);
        h.assertTrue(TradeCycling.cycle(other) == TradeCycling.Result.ALREADY_TRADED, "a villager with experience does not cycle either");

        ServerPlayer third = player(h);
        Villager levelled = farmer(h, third, true);
        levelled.setVillagerData(levelled.getVillagerData().withLevel(2));
        h.assertTrue(TradeCycling.cycle(third) == TradeCycling.Result.ALREADY_TRADED, "nor one that is past Novice");
        h.succeed();
    }

    @GameTest
    public void cyclingNeedsAnOpenTradeWithTrades(GameTestHelper h) {
        ServerPlayer player = player(h);
        h.assertTrue(TradeCycling.cycle(player) == TradeCycling.Result.NOT_TRADING, "with no trading screen open there is nothing to cycle");
        ServerPlayer other = player(h);
        farmer(h, other, false);
        h.assertTrue(TradeCycling.cycle(other) == TradeCycling.Result.NO_TRADES, "a villager with no job has no trades to cycle");
        h.succeed();
    }
}
