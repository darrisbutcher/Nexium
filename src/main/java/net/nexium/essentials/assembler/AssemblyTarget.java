package net.nexium.essentials.assembler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.nexium.aiot.AiotTier;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.essentials.farm.FarmRecipe;
import net.nexium.essentials.hostile.HostileMobSwitch;
import net.nexium.essentials.ironfarm.IronFarmRecipe;
import net.nexium.essentials.librarian.Librarian;
import net.nexium.essentials.registry.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the Farm Assembler can make: the one-block farms (the Experience Farm too), the Master Librarian, the Hostile Mob Switch and
 * the Stone to Diamond AIOTs from raw materials, in the order its screen lists them.
 */
public enum AssemblyTarget {
    IRON_FARM((FarmKind) null),
    VILLAGER_BREEDER(FarmKind.VILLAGER_BREEDER),
    PIGLIN_BARTER(FarmKind.PIGLIN_BARTER),
    ZOMBIFIED_PIGLIN(FarmKind.ZOMBIFIED_PIGLIN),
    WITHER_SKELETON(FarmKind.WITHER_SKELETON),
    SHULKER(FarmKind.SHULKER),
    WITCH(FarmKind.WITCH),
    SUGAR_CANE(FarmKind.SUGAR_CANE),
    BAMBOO(FarmKind.BAMBOO),
    NETHER_WART(FarmKind.NETHER_WART),
    MONSTER(FarmKind.MONSTER),
    NETHER_MOB(FarmKind.NETHER_MOB),
    OCEAN(FarmKind.OCEAN),
    SLIME(FarmKind.SLIME),
    LIVESTOCK(FarmKind.LIVESTOCK),
    CROP(FarmKind.CROP),
    GOURD(FarmKind.GOURD),
    GENERATOR(FarmKind.GENERATOR),
    BEE(FarmKind.BEE),
    MOSS(FarmKind.MOSS),
    FROGLIGHT(FarmKind.FROGLIGHT),
    TREE(FarmKind.TREE),
    XP_FARM((FarmKind) null),
    LIBRARIAN((FarmKind) null),
    HOSTILE_SWITCH((FarmKind) null),
    /** An AIOT from what its five tools cost in raw materials: 11 of the tier's material and 9 sticks. */
    STONE_AIOT(AiotTier.STONE),
    COPPER_AIOT(AiotTier.COPPER),
    IRON_AIOT(AiotTier.IRON),
    GOLDEN_AIOT(AiotTier.GOLDEN),
    DIAMOND_AIOT(AiotTier.DIAMOND);

    /** What the five tools of an AIOT cost together: a pickaxe and an axe take 3 of the material, a hoe and a sword 2, a shovel 1. */
    public static final int AIOT_MATERIAL = 3 + 3 + 1 + 2 + 2;
    /** ... and sticks: 2 each for the pickaxe, axe, shovel and hoe, 1 for the sword. */
    public static final int AIOT_STICKS = 2 + 2 + 2 + 2 + 1;

    /** The Experience Farm takes a stack of Bottles o' Enchanting and nothing else. */
    public static final int XP_FARM_BOTTLES = 64;

    /** The most lines any target has (the Iron Farm: seven parts and its smooth stone). */
    public static final int MAX_REQUIREMENTS = 8;

    private final @Nullable FarmKind kind;
    private final @Nullable AiotTier tier;

    AssemblyTarget(@Nullable FarmKind kind) {
        this.kind = kind;
        this.tier = null;
    }

    AssemblyTarget(AiotTier tier) {
        this.kind = null;
        this.tier = tier;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AssemblyTarget byId(String id) {
        for (AssemblyTarget t : values()) if (t.id().equals(id)) return t;
        return IRON_FARM;
    }

    public static AssemblyTarget byIndex(int index) {
        AssemblyTarget[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** What it needs, in the order the screen lists them; the filler block, if any, comes last. */
    public List<Requirement> requirements() {
        List<Requirement> list = new ArrayList<>();
        if (this == IRON_FARM) {
            for (IronFarmRecipe.Part part : IronFarmRecipe.Part.values()) {
                list.add(new Requirement(part.displayName(), part.needed, s -> IronFarmRecipe.partOf(s) == part, part.icon, part.isMob()));
            }
            list.add(new Requirement(Component.translatable("message.nexium.iron_farm_part.smooth_stone"), IronFarmRecipe.FILLER_AMOUNT,
                    IronFarmRecipe::isFiller, Items.SMOOTH_STONE, false));
        } else if (this == XP_FARM) {
            list.add(new Requirement(Component.translatable("item.minecraft.experience_bottle"), XP_FARM_BOTTLES,
                    s -> s.is(Items.EXPERIENCE_BOTTLE), Items.EXPERIENCE_BOTTLE, false));
        } else if (this == HOSTILE_SWITCH) {
            list.add(new Requirement(Component.translatable("message.nexium.hostile_switch_part.mobs"), HostileMobSwitch.MOBS_NEEDED,
                    HostileMobSwitch::isHostileMob, Items.ZOMBIE_SPAWN_EGG, true));
        } else if (tier != null) {
            // The raw materials of the five tools (a stone AIOT takes any of the stone tool materials)
            list.add(new Requirement(aiotMaterialName(), AIOT_MATERIAL, aiotMaterial(), aiotMaterialIcon(), false));
            list.add(new Requirement(Items.STICK.getDefaultInstance().getHoverName(), AIOT_STICKS, s -> s.is(Items.STICK), Items.STICK, false));
        } else if (this == LIBRARIAN) {
            list.add(new Requirement(IronFarmRecipe.Part.VILLAGERS.displayName(), NexusConfig.get().librarianVillagers,
                    s -> IronFarmRecipe.partOf(s) == IronFarmRecipe.Part.VILLAGERS, Items.VILLAGER_SPAWN_EGG, true));
        } else {
            FarmRecipe recipe = FarmRecipe.of(kind);
            for (FarmRecipe.Part part : recipe.parts()) list.add(of(part));
            list.add(of(recipe.filler()));
        }
        return list;
    }

    private java.util.function.Predicate<ItemStack> aiotMaterial() {
        return switch (tier) {
            case STONE -> s -> s.is(ItemTags.STONE_TOOL_MATERIALS);
            default -> s -> s.is(aiotMaterialIcon());
        };
    }

    private net.minecraft.world.item.Item aiotMaterialIcon() {
        return switch (tier) {
            case STONE -> Items.COBBLESTONE;
            case COPPER -> Items.COPPER_INGOT;
            case IRON -> Items.IRON_INGOT;
            case GOLDEN -> Items.GOLD_INGOT;
            case DIAMOND -> Items.DIAMOND;
            default -> throw new IllegalStateException("no raw recipe for " + tier);
        };
    }

    private Component aiotMaterialName() {
        return tier == AiotTier.STONE ? Component.translatable("message.nexium.aiot_part.stone_material") : aiotMaterialIcon().getDefaultInstance().getHoverName();
    }

    private static Requirement of(FarmRecipe.Part part) {
        return new Requirement(part.displayName(), part.needed(), part.matches(), part.icon(), part.mob());
    }

    /** What it makes, as shown in the list and the header. */
    public ItemStack icon() {
        if (this == IRON_FARM) return new ItemStack(ModItems.IRON_FARM);
        if (this == XP_FARM) return new ItemStack(ModItems.XP_FARM);
        if (this == LIBRARIAN) return new ItemStack(Items.VILLAGER_SPAWN_EGG);
        if (this == HOSTILE_SWITCH) return new ItemStack(ModItems.HOSTILE_MOB_SWITCH);
        if (tier != null) return new ItemStack(net.nexium.registry.ModItems.AIOTS.get(tier));
        return new ItemStack(ModItems.FARMS.get(kind));
    }

    public Component displayName() {
        if (this == LIBRARIAN) return Component.translatable("screen.nexium.assembler.librarian");
        return icon().getHoverName();
    }

    /** The shorter name the screen's list shows. */
    public Component shortName() {
        return Component.translatable("screen.nexium.assembler.short." + id());
    }

    /** One line on what it does. */
    public Component makes() {
        if (kind != null) return Component.translatable("tooltip.nexium." + kind.lang("makes"));
        return Component.translatable("screen.nexium.assembler.makes." + id());
    }

    /** Makes the finished thing; empty if it can't be made right now (a Master Librarian needs a villager to spawn). */
    public ItemStack build(ServerLevel level) {
        if (this == LIBRARIAN) return Librarian.create(level);
        return icon();
    }
}
