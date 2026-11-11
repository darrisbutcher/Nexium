package net.storagenexus.essentials.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.storagenexus.essentials.NexusEssentials;

public final class ModTags {
    /** Armor that keeps piglins calm like gold armor (all netherite armor). */
    public static final TagKey<Item> CALMS_PIGLINS = TagKey.create(Registries.ITEM, NexusEssentials.id("calms_piglins"));
    /** Mobs that can't be picked up (the Ender Dragon and the Wither). */
    public static final TagKey<EntityType<?>> CANNOT_BE_PICKED_UP = TagKey.create(Registries.ENTITY_TYPE, NexusEssentials.id("cannot_be_picked_up"));
    /** Boss mobs that cost experience levels to pick up (the Warden, the Elder Guardian and other mods' bosses). */
    public static final TagKey<EntityType<?>> PICK_UP_COSTS_LEVELS = TagKey.create(Registries.ENTITY_TYPE, NexusEssentials.id("pick_up_costs_levels"));
    /** Blocks the Elixir of Bedrock Breaking lets you mine (bedrock). */
    public static final TagKey<Block> BEDROCK_BREAKER_MINEABLE = TagKey.create(Registries.BLOCK, NexusEssentials.id("bedrock_breaker_mineable"));
    /** Damage a dropped Captured Mob item shrugs off, so a pet can't burn up or be blown up. */
    public static final TagKey<DamageType> CAPTURED_MOB_RESISTS = TagKey.create(Registries.DAMAGE_TYPE, NexusEssentials.id("captured_mob_resists"));


    // What the Iron Farm takes at the Farm Assembler. Mobs count as their spawn egg or as a Captured Mob item.
    /** The 4 mobs that scare the villagers: zombie, husk, drowned and zombie villager. */
    public static final TagKey<EntityType<?>> IRON_FARM_ZOMBIES = TagKey.create(Registries.ENTITY_TYPE, NexusEssentials.id("iron_farm_zombies"));
    /** The 12 villagers. */
    public static final TagKey<EntityType<?>> IRON_FARM_VILLAGERS = TagKey.create(Registries.ENTITY_TYPE, NexusEssentials.id("iron_farm_villagers"));
    public static final TagKey<Item> IRON_FARM_TORCHES = item("iron_farm_torches");
    public static final TagKey<Item> IRON_FARM_SLABS = item("iron_farm_slabs");
    public static final TagKey<Item> IRON_FARM_SIGNS = item("iron_farm_signs");
    public static final TagKey<Item> IRON_FARM_TRAPDOORS = item("iron_farm_trapdoors");
    public static final TagKey<Item> IRON_FARM_BEDS = item("iron_farm_beds");
    /** Full stacks of this fill every other slot (smooth stone). */
    public static final TagKey<Item> IRON_FARM_FILLER = item("iron_farm_filler");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, NexusEssentials.id(name));
    }

    private ModTags() {}
}
