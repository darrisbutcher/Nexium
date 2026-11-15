package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.storagenexus.energy.NexusEnergy;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.xpfarm.ExperienceMath;
import net.storagenexus.essentials.xpfarm.XpFarmBlock;
import net.storagenexus.essentials.xpfarm.XpFarmBlockEntity;

/**
 * The Experience Farm: 40 XP a second on 10 NE/t, banked up to 200 levels, collected all at once or a level or ten at a
 * time, paused by redstone, read by a comparator, and given back as orbs when it is broken.
 */
public class XpFarmGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static XpFarmBlockEntity farm(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.XP_FARM);
        return h.getBlockEntity(POS, XpFarmBlockEntity.class);
    }

    private static void tick(GameTestHelper h, XpFarmBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) XpFarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
    }

    @GameTest
    public void theArithmeticIsVanillas(GameTestHelper h) {
        h.assertValueEqual(ExperienceMath.totalForLevel(1), 7L, "level 1 costs 7");
        h.assertValueEqual(ExperienceMath.totalForLevel(10), 160L, "level 10 is 160 in all");
        h.assertValueEqual(ExperienceMath.totalForLevel(30), 1395L, "level 30 is 1,395 in all");
        h.assertValueEqual(ExperienceMath.totalForLevel(200), 149_720L, "level 200 is 149,720 in all");
        h.assertValueEqual(XpFarmBlockEntity.CAPACITY, 149_720L, "the bank holds 200 levels' worth");
        h.assertValueEqual(ExperienceMath.levelOf(1394), 29, "one short of level 30");
        h.assertValueEqual(ExperienceMath.levelOf(1395), 30, "level 30");
        h.assertValueEqual(ExperienceMath.levelOf(XpFarmBlockEntity.CAPACITY), 200, "a full bank is level 200");
        h.succeed();
    }

    @GameTest
    public void itMakesFortyExperienceASecondOnTenEnergyATick(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        f.energy().set(2_000);
        tick(h, f, 20);
        h.assertValueEqual(f.experience(), 40L, "40 points in a second");
        h.assertValueEqual(f.energy().stored(), 2_000L - 200L, "for 200 NE, 10 a tick");
        h.assertTrue(f.isWorking() && !f.isUnpowered() && !f.isFull(), "working");
        h.succeed();
    }

    @GameTest
    public void itWaitsForPowerAndStopsWhenTheBankIsFull(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        f.energy().set(9);
        tick(h, f, 5);
        h.assertValueEqual(f.experience(), 0L, "nothing without 10 NE");
        h.assertValueEqual(f.energy().stored(), 9L, "and no energy used");
        h.assertTrue(f.isUnpowered() && !f.isWorking(), "it says it needs power");
        f.energy().set(2_000);
        f.addExperience(XpFarmBlockEntity.CAPACITY - 1);
        tick(h, f, 1);
        h.assertValueEqual(f.experience(), XpFarmBlockEntity.CAPACITY, "the last point fits, never past the cap");
        long energy = f.energy().stored();
        tick(h, f, 10);
        h.assertValueEqual(f.experience(), XpFarmBlockEntity.CAPACITY, "full stays full");
        h.assertValueEqual(f.energy().stored(), energy, "and uses no energy");
        h.assertTrue(f.isFull(), "it says it is full");
        h.succeed();
    }

    @GameTest
    public void aRedstoneSignalPausesIt(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.XP_FARM.defaultBlockState().setValue(XpFarmBlock.POWERED, true));
        XpFarmBlockEntity f = h.getBlockEntity(POS, XpFarmBlockEntity.class);
        f.energy().set(2_000);
        tick(h, f, 10);
        h.assertValueEqual(f.experience(), 0L, "paused makes nothing");
        h.assertValueEqual(f.energy().stored(), 2_000L, "and uses nothing");
        h.assertTrue(f.isPaused() && !f.isUnpowered(), "paused, not out of power");
        h.setBlock(POS, ModBlocks.XP_FARM.defaultBlockState().setValue(XpFarmBlock.POWERED, false));
        XpFarmBlockEntity again = h.getBlockEntity(POS, XpFarmBlockEntity.class);
        again.energy().set(2_000);
        tick(h, again, 10);
        h.assertValueEqual(again.experience(), 20L, "and it runs again without the signal");
        h.succeed();
    }

    @GameTest
    public void itTakesEnergyFromConduitsAndGenerators(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        var energy = NexusEnergy.SIDED.find(h.getLevel(), h.absolutePos(POS), Direction.NORTH);
        h.assertTrue(energy != null, "it exposes Nexus Energy");
        h.assertValueEqual(energy.insert(500, false), 500L, "it takes energy in");
        h.assertValueEqual(energy.extract(100, true), 0L, "and gives none out");
        h.assertValueEqual(f.energy().stored(), 500L, "what it was given");
        h.succeed();
    }

    @GameTest
    public void collectAllGivesEveryPointBanked(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        int before = player.totalExperience;
        f.addExperience(1_234);
        h.assertValueEqual(f.takeAll(player), 1_234L, "all of it");
        h.assertValueEqual(player.totalExperience - before, 1_234, "the player has it");
        h.assertValueEqual(f.experience(), 0L, "and the bank is empty");
        h.assertValueEqual(f.takeAll(player), 0L, "nothing left to give");
        h.succeed();
    }

    @GameTest
    public void theLevelButtonsGiveExactlyTheLevels(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.giveExperienceLevels(-player.experienceLevel);
        player.giveExperiencePoints(-player.totalExperience);
        f.addExperience(100_000);
        // From level 0: one level is 7 points, and the player is exactly at level 1
        h.assertValueEqual(f.takeLevels(player, 1), 7L, "+1 level from nothing costs 7");
        h.assertValueEqual(player.experienceLevel, 1, "level 1");
        // Ten more: level 11 starts at 187 points in all, 180 more than level 1
        h.assertValueEqual(f.takeLevels(player, 10), 180L, "+10 levels costs 180 here");
        h.assertValueEqual(player.experienceLevel, 11, "level 11");
        h.assertTrue(player.experienceProgress < 0.001F, "on the level, bar to within float rounding: " + player.experienceProgress);
        h.assertValueEqual(f.experience(), 100_000L - 187L, "the bank paid exactly that");
        // Part way into a level, only the rest of it is needed
        player.giveExperiencePoints(10); // 10 of the 29 points level 11 takes
        long need = ExperienceMath.toGainLevels(player, 1);
        h.assertValueEqual(need, (long) ExperienceMath.toNextLevel(11) - 10, "the rest of the level");
        h.assertValueEqual(f.takeLevels(player, 1), need, "+1 level gives just the rest of it");
        h.assertValueEqual(player.experienceLevel, 12, "level 12");
        // With too little banked it gives what there is
        h.setBlock(POS.east(), ModBlocks.XP_FARM);
        XpFarmBlockEntity small = h.getBlockEntity(POS.east(), XpFarmBlockEntity.class);
        small.addExperience(5);
        h.assertValueEqual(small.takeLevels(player, 10), 5L, "all that is left");
        h.assertValueEqual(small.experience(), 0L, "and the bank is empty");
        h.succeed();
    }

    @GameTest
    public void aComparatorReadsHowFullTheBankIs(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        h.assertValueEqual(f.comparatorSignal(), 0, "empty reads 0");
        f.addExperience(1);
        h.assertValueEqual(f.comparatorSignal(), 1, "any experience reads 1");
        f.addExperience(XpFarmBlockEntity.CAPACITY / 2);
        int half = f.comparatorSignal();
        h.assertTrue(half >= 7 && half <= 9, "half full reads about 8: " + half);
        f.addExperience(XpFarmBlockEntity.CAPACITY);
        h.assertValueEqual(f.comparatorSignal(), 15, "full reads 15");
        h.succeed();
    }

    @GameTest
    public void breakingItReleasesTheBankAsOrbs(GameTestHelper h) {
        // Over and over, so that orbs of equal value merge (they do about one time in forty) and the sums still add up
        AABB area = new AABB(h.absolutePos(POS)).inflate(4);
        for (int round = 1; round <= 40; round++) {
            XpFarmBlockEntity f = farm(h);
            f.addExperience(5_000);
            h.setBlock(POS, Blocks.AIR);
            long total = 0;
            for (ExperienceOrb orb : h.getLevel().getEntitiesOfClass(ExperienceOrb.class, area)) total += worth(orb);
            h.assertValueEqual(total, 5_000L * round, "all the points are in orbs after " + round + " breaks");
        }
        h.succeed();
    }

    /**
     * What an orb is worth in all: vanilla merges orbs of equal value into one that remembers how many it stands for
     * (which ones merge depends on entity ids, so a plain sum of values can come up a few points short).
     */
    private static long worth(ExperienceOrb orb) {
        try {
            var count = ExperienceOrb.class.getDeclaredField("count");
            count.setAccessible(true);
            return (long) orb.getValue() * count.getInt(orb);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot read how many orbs this one stands for", e);
        }
    }

    // ---------------------------------------------------------------- Mending

    private static ItemStack damaged(GameTestHelper h, net.minecraft.world.item.Item item, int damage, boolean mending) {
        ItemStack stack = new ItemStack(item);
        if (mending) stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        stack.setDamageValue(damage);
        return stack;
    }

    private static ServerPlayer blank(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.giveExperienceLevels(-player.experienceLevel);
        player.giveExperiencePoints(-player.totalExperience);
        return player;
    }

    @GameTest
    public void whatYouCollectMendsATool(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack pick = damaged(h, Items.DIAMOND_PICKAXE, 800, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        f.addExperience(100);
        h.assertValueEqual(f.takeAll(player), 100L, "the farm hands over its 100 points");
        h.assertValueEqual(pick.getDamageValue(), 600, "all of them mended: 2 durability a point, 200 in all");
        h.assertValueEqual(player.totalExperience, 0, "so none were left for the player");
        h.assertValueEqual(f.experience(), 0L, "and the bank is empty");
        h.succeed();
    }

    @GameTest
    public void whatMendingDoesNotNeedBecomesLevels(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack pick = damaged(h, Items.DIAMOND_PICKAXE, 800, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        f.addExperience(1_000);
        f.takeAll(player);
        h.assertValueEqual(pick.getDamageValue(), 0, "the pickaxe is fully repaired");
        h.assertValueEqual(player.totalExperience, 600, "1,000 points less the 400 that 800 durability costs (2 a point): 600 are the player's");
        h.succeed();
    }

    @GameTest
    public void aToolWithoutMendingIsLeftAlone(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack plain = damaged(h, Items.DIAMOND_PICKAXE, 800, false);
        ItemStack undamaged = damaged(h, Items.DIAMOND_SWORD, 0, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        player.setItemInHand(InteractionHand.OFF_HAND, undamaged);
        f.addExperience(500);
        f.takeAll(player);
        h.assertValueEqual(plain.getDamageValue(), 800, "a damaged tool without Mending stays damaged");
        h.assertValueEqual(player.totalExperience, 500, "and the player gets every point");
        h.succeed();
    }

    @GameTest
    public void mendingReachesArmorAndTheWingsSlot(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack elytra = damaged(h, Items.ELYTRA, 300, true);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, elytra);
        f.addExperience(50);
        f.takeAll(player);
        h.assertValueEqual(elytra.getDamageValue(), 200, "an elytra in the wings slot is mended: 100 durability for 50 points");
        h.assertValueEqual(player.totalExperience, 0, "all of it");

        ItemStack boots = damaged(h, Items.DIAMOND_BOOTS, 100, true);
        ExtraSlots.of(player).clearContent();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
        f.addExperience(20);
        f.takeAll(player);
        h.assertValueEqual(boots.getDamageValue(), 60, "worn armor is mended too");
        h.succeed();
    }

    @GameTest
    public void aSpillOverMendsAnotherDamagedItem(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack pick = damaged(h, Items.DIAMOND_PICKAXE, 40, true);
        ItemStack sword = damaged(h, Items.DIAMOND_SWORD, 40, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        player.setItemInHand(InteractionHand.OFF_HAND, sword);
        f.addExperience(100);
        f.takeAll(player);
        // One is picked at random first; either way each needs only 20 points for its 40 durability, and the 60 left are the player's
        h.assertValueEqual(pick.getDamageValue(), 0, "the pickaxe is fully mended");
        h.assertValueEqual(sword.getDamageValue(), 0, "and so is the sword, from what the first did not use");
        h.assertValueEqual(player.totalExperience, 60, "and the player gets the 60 points that neither needed");
        h.succeed();
    }

    @GameTest
    public void theLevelButtonsMendFirstToo(GameTestHelper h) {
        XpFarmBlockEntity f = farm(h);
        ServerPlayer player = blank(h);
        ItemStack pick = damaged(h, Items.DIAMOND_PICKAXE, 800, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        f.addExperience(100_000);
        h.assertValueEqual(f.takeLevels(player, 1), 7L, "+1 level still takes the 7 points it always does");
        h.assertValueEqual(pick.getDamageValue(), 786, "they mended 14 durability");
        h.assertValueEqual(player.experienceLevel, 0, "so the player is still on level 0");
        h.assertValueEqual(f.experience(), 100_000L - 7L, "and the bank paid for them");
        h.succeed();
    }
}
