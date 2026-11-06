package net.storagenexus.essentials.librarian;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.storagenexus.essentials.capture.MobCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Master Librarian a "Nexus Librarian" chest makes: a level 5 librarian that sells an enchanted book of every
 * enchantment (curses excepted) at its highest level, Mending and the other treasure enchantments included. Prices
 * follow the villagers' own book formula: 2 emeralds plus up to 5 + 3 per level more, doubled for treasure
 * enchantments, never above 64; each trade also takes a book. It comes out as a Captured Mob item.
 */
public final class Librarian {
    /** Marks the Captured Mob item the chest makes, so it can't be fed back in as one of the villagers a chest wants. */
    private static final String MARKER = "storagenexus_librarian";
    private static final int MAX_USES = 12;
    private static final int XP = 15;

    /** Every trade for the books, in the order of the enchantment registry. */
    public static MerchantOffers offers(ServerLevel level, RandomSource random) {
        MerchantOffers offers = new MerchantOffers();
        List<Holder.Reference<Enchantment>> all = new ArrayList<>(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().toList());
        all.sort((a, b) -> a.key().identifier().compareTo(b.key().identifier()));
        for (Holder.Reference<Enchantment> enchantment : all) {
            if (enchantment.is(EnchantmentTags.CURSE)) continue;
            int max = enchantment.value().getMaxLevel();
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(enchantment, max));
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, price(enchantment, max, random)),
                    Optional.of(new ItemCost(Items.BOOK)), book, MAX_USES, XP, 0.2F));
        }
        return offers;
    }

    /** The villagers' price for a book at this level: 2 + up to 5 + 3 per level more, doubled for treasure, at most 64. */
    public static int price(Holder<Enchantment> enchantment, int level, RandomSource random) {
        int cost = 2 + random.nextInt(5 + level * 3);
        if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) cost *= 2;
        return Math.min(cost, 64);
    }

    /** A Master Librarian with all the trades, as a Captured Mob item. */
    public static ItemStack create(ServerLevel level) {
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (villager == null) return ItemStack.EMPTY;
        VillagerData data = villager.getVillagerData()
                .withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN)
                .withLevel(VillagerData.MAX_VILLAGER_LEVEL);
        villager.setVillagerData(data);
        villager.setVillagerXp(VillagerData.getMinXpPerLevel(VillagerData.MAX_VILLAGER_LEVEL));
        villager.setVillagerDataFinalized(true);
        villager.setOffers(offers(level, level.getRandom()));
        ItemStack stack = MobCapture.capture(villager);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(MARKER, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Whether this item came out of a librarian chest. */
    public static boolean isMade(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(MARKER, false);
    }

    private Librarian() {}
}
