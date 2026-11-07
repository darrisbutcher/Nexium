package net.storagenexus.essentials.ironfarm;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModTags;
import org.jspecify.annotations.Nullable;

/**
 * What the Iron Farm costs at the Farm Assembler: 4 zombies, 12 villagers (spawn eggs or Captured Mobs), 33 torches,
 * 20 slabs, 16 signs, 12 trapdoors and 12 beds, and {@value #FILLER_AMOUNT} smooth stone. Any mix of the kinds in each
 * part's tag counts.
 */
public final class IronFarmRecipe {
    /**
     * The smooth stone it takes: what the double chest it used to be built from held besides the parts, 22 full stacks
     * (54 slots, less the 32 that Captured Mobs, beds and one stack of each other part fill).
     */
    public static final int FILLER_AMOUNT = 22 * 64;

    public enum Part {
        ZOMBIES("zombies", 4, Items.ZOMBIE_SPAWN_EGG),
        VILLAGERS("villagers", 12, Items.VILLAGER_SPAWN_EGG),
        TORCHES("torches", 33, Items.TORCH),
        SLABS("slabs", 20, Items.SMOOTH_STONE_SLAB),
        SIGNS("signs", 16, Items.OAK_SIGN),
        TRAPDOORS("trapdoors", 12, Items.OAK_TRAPDOOR),
        BEDS("beds", 12, Items.BED.red());

        public final String key;
        public final int needed;
        public final Item icon;

        Part(String key, int needed, Item icon) {
            this.key = key;
            this.needed = needed;
            this.icon = icon;
        }

        public Component displayName() {
            return Component.translatable("message.nexusessentials.iron_farm_part." + key);
        }

        /** Zombies and villagers are mobs: a spawn egg or a Captured Mob. */
        public boolean isMob() {
            return this == ZOMBIES || this == VILLAGERS;
        }
    }

    /** Which material a stack counts as, or null if it isn't one. */
    public static @Nullable Part partOf(ItemStack stack) {
        EntityType<?> mob = mobOf(stack);
        if (mob != null) {
            if (mob.builtInRegistryHolder().is(ModTags.IRON_FARM_ZOMBIES)) return Part.ZOMBIES;
            // A Master Librarian is not material for another recipe
            if (mob.builtInRegistryHolder().is(ModTags.IRON_FARM_VILLAGERS)) return net.storagenexus.essentials.librarian.Librarian.isMade(stack) ? null : Part.VILLAGERS;
            return null;
        }
        if (stack.is(ModTags.IRON_FARM_TORCHES)) return Part.TORCHES;
        if (stack.is(ModTags.IRON_FARM_SLABS)) return Part.SLABS;
        if (stack.is(ModTags.IRON_FARM_SIGNS)) return Part.SIGNS;
        if (stack.is(ModTags.IRON_FARM_TRAPDOORS)) return Part.TRAPDOORS;
        if (stack.is(ModTags.IRON_FARM_BEDS)) return Part.BEDS;
        return null;
    }

    /** The smooth stone (the {@code iron_farm_filler} tag). */
    public static boolean isFiller(ItemStack stack) {
        return stack.is(ModTags.IRON_FARM_FILLER);
    }

    /** The mob in a spawn egg or a Captured Mob item, or null for anything else. */
    public static @Nullable EntityType<?> mobOf(ItemStack stack) {
        CapturedMob captured = stack.get(ModComponents.CAPTURED_MOB);
        if (captured != null) return captured.type();
        if (stack.getItem() instanceof SpawnEggItem) return SpawnEggItem.getType(stack);
        return null;
    }

    private IronFarmRecipe() {}
}
