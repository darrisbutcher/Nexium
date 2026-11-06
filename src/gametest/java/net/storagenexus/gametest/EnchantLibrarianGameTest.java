package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.ironfarm.IronFarmChest;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.librarian.Librarian;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Higher enchantment levels and fewer exclusions, and the "Nexus Librarian" chest that makes a Master Librarian. */
public class EnchantLibrarianGameTest {
    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static boolean compatible(GameTestHelper h, ResourceKey<Enchantment> a, ResourceKey<Enchantment> b) {
        return Enchantment.areCompatible(enchantment(h, a), enchantment(h, b));
    }

    @GameTest
    public void theseEnchantmentsGoToLevelFive(GameTestHelper h) {
        List<ResourceKey<Enchantment>> five = List.of(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION, Enchantments.BLAST_PROTECTION,
                Enchantments.PROJECTILE_PROTECTION, Enchantments.UNBREAKING, Enchantments.LOOTING, Enchantments.FORTUNE, Enchantments.RESPIRATION);
        for (ResourceKey<Enchantment> key : five) {
            h.assertValueEqual(enchantment(h, key).value().getMaxLevel(), 5, key.identifier() + " max level");
        }
        h.assertValueEqual(enchantment(h, Enchantments.MENDING).value().getMaxLevel(), 1, "mending is still one level");
        h.succeed();
    }

    @GameTest
    public void exclusionsAreRelaxedWhereAsked(GameTestHelper h) {
        h.assertTrue(compatible(h, Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION), "protection with fire protection");
        h.assertTrue(compatible(h, Enchantments.BLAST_PROTECTION, Enchantments.PROJECTILE_PROTECTION), "blast with projectile");
        h.assertTrue(compatible(h, Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION), "protection with projectile");
        h.assertTrue(compatible(h, Enchantments.INFINITY, Enchantments.MENDING), "infinity with mending");
        h.assertTrue(compatible(h, Enchantments.SHARPNESS, Enchantments.SMITE), "sharpness with smite");
        h.assertFalse(compatible(h, Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS), "sharpness still excludes bane of arthropods");
        h.assertFalse(compatible(h, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS), "smite still excludes bane of arthropods");
        h.assertFalse(compatible(h, Enchantments.SHARPNESS, Enchantments.IMPALING), "sharpness still excludes impaling");
        h.assertFalse(compatible(h, Enchantments.FORTUNE, Enchantments.SILK_TOUCH), "fortune still excludes silk touch");
        h.succeed();
    }

    @GameTest
    public void aBootCanHoldAllFourProtectionsAtLevelFive(GameTestHelper h) {
        ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
        EnchantmentHelper.updateEnchantments(boots, e -> {
            e.set(enchantment(h, Enchantments.PROTECTION), 5);
            e.set(enchantment(h, Enchantments.FIRE_PROTECTION), 5);
            e.set(enchantment(h, Enchantments.BLAST_PROTECTION), 5);
            e.set(enchantment(h, Enchantments.PROJECTILE_PROTECTION), 5);
        });
        h.assertValueEqual(EnchantmentHelper.getEnchantmentsForCrafting(boots).size(), 4, "four protections on one piece");
        h.succeed();
    }

    // ---------------------------------------------------------------- the librarian

    @GameTest
    public void theLibrarianSellsEveryBookAtItsHighestLevel(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MerchantOffers offers = Librarian.offers(level, level.getRandom());
        int expected = (int) level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().filter(e -> !e.is(EnchantmentTags.CURSE)).count();
        h.assertValueEqual(offers.size(), expected, "one trade per enchantment but the curses");
        boolean mending = false, protection5 = false, curse = false;
        for (MerchantOffer offer : offers) {
            h.assertTrue(offer.getResult().is(Items.ENCHANTED_BOOK), "sells enchanted books");
            var stored = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
            h.assertValueEqual(stored.size(), 1, "one enchantment per book");
            for (var entry : stored.entrySet()) {
                Holder<Enchantment> e = entry.getKey();
                h.assertValueEqual(entry.getIntValue(), e.value().getMaxLevel(), "highest level");
                if (e.is(Enchantments.MENDING)) mending = true;
                if (e.is(Enchantments.PROTECTION) && entry.getIntValue() == 5) protection5 = true;
                if (e.is(EnchantmentTags.CURSE)) curse = true;
            }
            int price = offer.getBaseCostA().getCount();
            h.assertTrue(price >= 2 && price <= 64, "price " + price + " within the villagers' range");
            h.assertTrue(offer.getCostB().getItem().equals(Items.BOOK), "a book goes in as well");
        }
        h.assertTrue(mending, "mending is on sale");
        h.assertTrue(protection5, "protection V is on sale");
        h.assertFalse(curse, "no curses");
        h.succeed();
    }

    @GameTest
    public void theLibrarianComesOutAsAMarkedMasterVillager(GameTestHelper h) {
        ItemStack stack = Librarian.create(h.getLevel());
        CapturedMob mob = stack.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(mob != null && mob.type() == EntityTypes.VILLAGER, "a captured villager");
        h.assertTrue(Librarian.isMade(stack), "marked as made by a librarian chest");
        String data = mob.data().toString();
        h.assertTrue(data.contains("librarian"), "a librarian: " + data.substring(0, Math.min(200, data.length())));
        h.assertTrue(data.contains("minecraft:enchanted_book"), "with the books");
        h.assertTrue(IronFarmRecipe.partOf(stack) == null, "it does not count as a villager for another recipe");
        h.succeed();
    }

    private static ItemStack villager() {
        return MobCapture.toItem(new CapturedMob(EntityTypes.VILLAGER, new CompoundTag(), Optional.empty(), 20.0F, 20.0F, false));
    }

    private static ChestBlockEntity chest(GameTestHelper h, String name) {
        h.setBlock(CHEST, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE));
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        ItemStack named = new ItemStack(Items.CHEST);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        chest.applyComponentsFromItemStack(named);
        return chest;
    }

    private static void fill(ChestBlockEntity chest, List<ItemStack> stacks) {
        for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        chest.setChanged();
    }

    @GameTest
    public void aNamedChestWithTwelveVillagersMakesAMasterLibrarian(GameTestHelper h) {
        ChestBlockEntity chest = chest(h, "Nexus Librarian");
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 12; i++) stacks.add(villager());
        fill(chest, stacks);
        IronFarmChest.process(h.getLevel(), h.absolutePos(CHEST), null);
        h.runAfterDelay(2, () -> {
            h.assertBlockNotPresent(Blocks.CHEST, CHEST);
            List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CHEST)).inflate(3));
            boolean found = items.stream().anyMatch(e -> Librarian.isMade(e.getItem()));
            h.assertTrue(found, "the librarian dropped where the chest was");
            h.succeed();
        });
    }

    @GameTest
    public void aChestWithTheWrongContentsKeepsEverythingAndTellsThePlayer(GameTestHelper h) {
        ChestBlockEntity chest = chest(h, "Nexus Librarian");
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 11; i++) stacks.add(villager());
        fill(chest, stacks);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        IronFarmChest.process(h.getLevel(), h.absolutePos(CHEST), player);
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.CHEST, CHEST);
            // Twelve villagers plus a stray item is not ready either
            stacks.add(villager());
            stacks.add(new ItemStack(Items.DIRT));
            fill(chest, stacks);
            IronFarmChest.process(h.getLevel(), h.absolutePos(CHEST), player);
            h.runAfterDelay(2, () -> {
                h.assertBlockPresent(Blocks.CHEST, CHEST);
                // A librarian made by a chest is not one of the twelve
                List<ItemStack> again = new ArrayList<>();
                for (int i = 0; i < 11; i++) again.add(villager());
                again.add(Librarian.create(h.getLevel()));
                fill(chest, again);
                IronFarmChest.process(h.getLevel(), h.absolutePos(CHEST), player);
                h.runAfterDelay(2, () -> {
                    h.assertBlockPresent(Blocks.CHEST, CHEST);
                    h.succeed();
                });
            });
        });
    }
}
