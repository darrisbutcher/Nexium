package net.storagenexus.essentials.farm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.Locale;

/**
 * The one-block farms besides the Iron Farm. Each is built from a double chest with its own name and recipe
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
    SHULKER("shulker_farm", Input.NONE, 1);

    public enum Input { NONE, FOOD, GOLD }

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
        };
    }

    /** The name a double chest needs to become this farm, from the config. */
    public String chestName() {
        NexusConfig c = NexusConfig.get();
        return switch (this) {
            case VILLAGER_BREEDER -> c.villagerBreederChestName;
            case PIGLIN_BARTER -> c.piglinBarterChestName;
            case ZOMBIFIED_PIGLIN -> c.zombifiedPiglinChestName;
            case WITHER_SKELETON -> c.witherSkeletonChestName;
            case SHULKER -> c.shulkerFarmChestName;
        };
    }

    /** Whether this stack goes in the input slot. */
    public boolean acceptsInput(ItemStack stack) {
        return switch (input) {
            case NONE -> false;
            case GOLD -> stack.is(Items.GOLD_INGOT);
            case FOOD -> foodCost(stack) > 0;
        };
    }

    /** What the input slot shows faded when it is empty. */
    public Item inputIcon() {
        return input == Input.GOLD ? Items.GOLD_INGOT : Items.BREAD;
    }

    /** How many of this food one baby villager takes, or 0 if it isn't food for villagers. */
    public static int foodCost(ItemStack stack) {
        if (stack.is(Items.BREAD)) return BREAD_PER_VILLAGER;
        if (stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT)) return ROOTS_PER_VILLAGER;
        return 0;
    }

    /** What one unit of input is worth to this kind: the amount of the item that one cycle uses. */
    public int inputPerCycle(ItemStack stack) {
        return switch (input) {
            case NONE -> 0;
            case GOLD -> 1;
            case FOOD -> foodCost(stack);
        };
    }

    public static FarmKind byId(String id) {
        for (FarmKind kind : values()) if (kind.id.equals(id.toLowerCase(Locale.ROOT))) return kind;
        throw new IllegalArgumentException(id);
    }
}
