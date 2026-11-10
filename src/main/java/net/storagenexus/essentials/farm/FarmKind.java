package net.storagenexus.essentials.farm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.Locale;

/**
 * The one-block farms besides the Iron Farm. Each is made at the Farm Assembler from its recipe
 * ({@link FarmRecipe}), is placed anywhere, and every {@link #cycleTicks()} makes what its kind makes into 27
 * output slots that hoppers can only take from below. A farm with an {@link Input} also has an input slot.
 */
public enum FarmKind {
    /** One baby villager (a Captured Mob) per cycle, fed 3 bread or 12 carrots, potatoes or beetroot. */
    VILLAGER_BREEDER("villager_breeder", Input.FOOD, 1),
    /** Trades 1 gold ingot per cycle for the vanilla piglin barter loot. */
    PIGLIN_BARTER("piglin_barter", Input.GOLD, 2),
    /** Gold nuggets and the rare gold ingot, as if killed with Looting V. */
    ZOMBIFIED_PIGLIN("zombified_piglin_farm", Input.NONE, 2),
    /** Coal, bones and the rare skull. */
    WITHER_SKELETON("wither_skeleton_farm", Input.NONE, 3),
    /** Shulker shells. */
    SHULKER("shulker_farm", Input.NONE, 1),
    /** What a witch drops when a player kills her: glowstone dust, sugar, spider eyes, glass bottles, gunpowder, sticks, redstone. */
    WITCH("witch_farm", Input.NONE, 7),
    /** Sugar cane. */
    SUGAR_CANE("sugar_cane_farm", Input.NONE, 1),
    /** Bamboo. */
    BAMBOO("bamboo_farm", Input.NONE, 1),
    /** Nether wart. */
    NETHER_WART("nether_wart_farm", Input.NONE, 1),
    /** The loot of a zombie, skeleton, spider and creeper, and of the raiders (pillager, vindicator, evoker), all mixed. */
    MONSTER("monster_farm", Input.NONE, 12),
    /** The loot of a blaze, a ghast and a magma cube. */
    NETHER_MOB("nether_mob_farm", Input.NONE, 8),
    /** The loot of a guardian, a drowned and a cod. */
    OCEAN("ocean_farm", Input.NONE, 8),
    /** Slime balls. */
    SLIME("slime_farm", Input.NONE, 1),
    /** The drops of the animals the feed picks: wheat for cows and sheep, seeds for chickens, carrots for pigs and rabbits. */
    LIVESTOCK("livestock_farm", Input.FEED, 6),
    /** Wheat, carrots, potatoes and beetroot with their seeds; bone meal speeds it up. */
    CROP("crop_farm", Input.BONE_MEAL_BOOST, 6),
    /** Melon slices, pumpkins, cactus and kelp, all every cycle. */
    GOURD("gourd_farm", Input.NONE, 1),
    /** Cobblestone, stone, gravel, sand, clay, basalt or obsidian, by its mode. */
    GENERATOR("generator_farm", Input.NONE, 1),
    /** Honeycomb and bottles of honey into the output. */
    BEE("bee_farm", Input.NONE, 1),
    /** Moss and pale moss (blocks, carpet), pale hanging moss, resin clumps and a little azalea; needs bone meal. */
    MOSS("moss_farm", Input.BONE_MEAL, 3);

    /** What the input slot takes: nothing, villagers' food, gold, livestock feed, or bone meal (needed, or only a speed-up). */
    public enum Input { NONE, FOOD, GOLD, FEED, BONE_MEAL, BONE_MEAL_BOOST }

    /** Bone meal lasts this many cycles. */
    public static final int CYCLES_PER_BONE_MEAL = 3;

    /** Bread, or this many carrots, potatoes or beetroot, feed one baby villager. */
    public static final int BREAD_PER_VILLAGER = 3, ROOTS_PER_VILLAGER = 12;

    private final String id;
    private final Input input;
    private final int slotsNeeded;

    FarmKind(String id, Input input, int slotsNeeded) {
        this.id = id;
        this.input = input;
        this.slotsNeeded = slotsNeeded;
    }

    /** The registry name of the block and item. */
    public String id() { return id; }

    public Input input() { return input; }

    public boolean hasInput() { return input != Input.NONE; }

    /** Empty output slots a cycle needs, so its drops always fit. */
    public int slotsNeeded() { return slotsNeeded; }

    public String lang(String suffix) {
        return "farm." + id + "." + suffix;
    }

    /** Ticks per cycle, from the config. */
    public int cycleTicks() {
        NexusConfig c = NexusConfig.get();
        return switch (this) {
            case VILLAGER_BREEDER -> c.villagerBreederTicks;
            case PIGLIN_BARTER -> c.piglinBarterTicks;
            case ZOMBIFIED_PIGLIN -> c.zombifiedPiglinTicks;
            case WITHER_SKELETON -> c.witherSkeletonTicks;
            case SHULKER -> c.shulkerFarmTicks;
            case WITCH -> c.witchFarmTicks;
            case SUGAR_CANE -> c.sugarCaneFarmTicks;
            case BAMBOO -> c.bambooFarmTicks;
            case NETHER_WART -> c.netherWartFarmTicks;
            case MONSTER -> c.monsterFarmTicks;
            case NETHER_MOB -> c.netherMobFarmTicks;
            case OCEAN -> c.oceanFarmTicks;
            case SLIME -> c.slimeFarmTicks;
            case LIVESTOCK -> c.livestockFarmTicks;
            case CROP -> c.cropFarmTicks;
            case GOURD -> c.gourdFarmTicks;
            case GENERATOR -> c.generatorFarmTicks;
            case BEE -> c.beeFarmTicks;
            case MOSS -> c.mossFarmTicks;
        };
    }

    /** Whether this stack goes in the input slot. */
    public boolean acceptsInput(ItemStack stack) {
        return switch (input) {
            case NONE -> false;
            case GOLD -> stack.is(Items.GOLD_INGOT);
            case FOOD -> foodCost(stack) > 0;
            case FEED -> feed(stack) != null;
            case BONE_MEAL, BONE_MEAL_BOOST -> stack.is(Items.BONE_MEAL);
        };
    }

    /** What the input slot shows faded when it is empty. */
    public Item inputIcon() {
        return switch (input) {
            case GOLD -> Items.GOLD_INGOT;
            case FEED -> Items.WHEAT;
            case BONE_MEAL, BONE_MEAL_BOOST -> Items.BONE_MEAL;
            default -> Items.BREAD;
        };
    }

    /** Whether a cycle can't run without its input (all but the Crop Farm's bone meal, which only speeds it up). */
    public boolean inputRequired() {
        return input != Input.NONE && input != Input.BONE_MEAL_BOOST;
    }

    /** How many of this food one baby villager takes, or 0 if it isn't food for villagers. */
    public static int foodCost(ItemStack stack) {
        if (stack.is(Items.BREAD)) return BREAD_PER_VILLAGER;
        if (stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)) return ROOTS_PER_VILLAGER;
        return 0;
    }

    /** The animals a feed item brings to the Livestock Farm, or null if it isn't livestock feed. */
    public static @org.jspecify.annotations.Nullable Feed feed(ItemStack stack) {
        if (stack.is(Items.WHEAT)) return Feed.WHEAT;
        if (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.MELON_SEEDS) || stack.is(Items.PUMPKIN_SEEDS)) return Feed.SEEDS;
        if (stack.is(Items.CARROT)) return Feed.CARROT;
        if (stack.is(Items.POTATO) || stack.is(Items.BEETROOT)) return Feed.ROOT;
        return null;
    }

    /** What each feed brings: wheat, cows and sheep; seeds, chickens; carrots, pigs and rabbits; potatoes and beetroot, pigs. */
    public enum Feed { WHEAT, SEEDS, CARROT, ROOT }

    /** What one unit of input is worth to this kind: the amount of the item that one cycle uses. */
    public int inputPerCycle(ItemStack stack) {
        return switch (input) {
            case NONE -> 0;
            case GOLD, FEED -> 1;
            case FOOD -> foodCost(stack);
            case BONE_MEAL, BONE_MEAL_BOOST -> 1;
        };
    }

    /** The modes a mode button steps through (empty for farms with none), as lang key suffixes. */
    public java.util.List<String> modes() {
        return switch (this) {
            case GENERATOR -> java.util.List.of("cobblestone", "stone", "gravel", "sand", "clay", "basalt", "obsidian");
            default -> java.util.List.of();
        };
    }

    /** Ticks per cycle in this mode: the Generator's obsidian takes twice as long as the rest. */
    public int cycleTicks(int mode) {
        int base = cycleTicks();
        return this == GENERATOR && modes().get(Math.floorMod(mode, modes().size())).equals("obsidian") ? base * 2 : base;
    }

    public static FarmKind byId(String id) {
        for (FarmKind kind : values()) if (kind.id.equals(id.toLowerCase(Locale.ROOT))) return kind;
        throw new IllegalArgumentException(id);
    }
}
