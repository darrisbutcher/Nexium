package net.storagenexus.nexium;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.storagenexus.StorageNexus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Nexium: the tier above netherite. Made in a crafting table from a block of diamond, a block of emerald, a block of
 * netherite and a netherite upgrade smithing template (three ingots), and put on netherite gear at a smithing table.
 * <p>
 * Its pickaxe, axe, shovel, hoe and AIOT give Haste II while held (see {@link NexiumHaste}), so with Efficiency V
 * they mine as fast as netherite with Efficiency V under a Haste II beacon.
 */
public final class NexiumMaterials {
    /** What repairs Nexium tools and armor at an anvil: the Nexium ingot. */
    public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, StorageNexus.id("nexium_tool_materials"));
    public static final TagKey<Item> REPAIRS_ARMOR = TagKey.create(Registries.ITEM, StorageNexus.id("repairs_nexium_armor"));
    /** Tools that give Haste II while held in the main hand (unless turned off in the tool menu). */
    public static final TagKey<Item> GRANTS_HASTE = TagKey.create(Registries.ITEM, StorageNexus.id("grants_haste"));

    /** Netherite's mining level and speed, 2,500 uses (netherite 2,031), +5 damage (netherite +4), enchantability 18. */
    public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2500, 9.0F, 5.0F, 18, TOOL_MATERIALS);

    public static final ResourceKey<EquipmentAsset> EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, StorageNexus.id("nexium"));

    /**
     * Netherite plus one armor point per piece (4/9/7/4, 24 in all), 5 toughness per piece (20, the game's cap),
     * 0.15 knockback resistance per piece and durability multiplier 44 (netherite 37).
     */
    public static final ArmorMaterial ARMOR = new ArmorMaterial(44, defense(4, 7, 9, 4, 20), 18,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 5.0F, 0.15F, REPAIRS_ARMOR, EQUIPMENT_ASSET);

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    private NexiumMaterials() {}
}
