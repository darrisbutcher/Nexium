package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.storagenexus.StorageNexus;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.aiot.module.RetiredModuleItem;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.barrel.BarrelUpgradeItem;
import net.storagenexus.conduit.ConduitItem;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.conduit.ConduitWrenchItem;
import net.storagenexus.dimension.DimensionalLinkCardItem;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.guide.GuideBookItem;
import net.storagenexus.nexium.NexiumChestplateItem;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.tablet.StorageTabletItem;
import net.storagenexus.upgrade.CapacityUpgradeItem;
import net.storagenexus.upgrade.UpgradeItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final List<Item> TAB_ORDER = new ArrayList<>();

    public static final GuideBookItem GUIDE = register("guide", GuideBookItem::new, new Item.Properties().stacksTo(1));

    public static final CapacityUpgradeItem IRON_UPGRADE = register("iron_capacity_upgrade", p -> new CapacityUpgradeItem(p, 2), new Item.Properties());
    public static final CapacityUpgradeItem GOLD_UPGRADE = register("gold_capacity_upgrade", p -> new CapacityUpgradeItem(p, 4), new Item.Properties());
    public static final CapacityUpgradeItem DIAMOND_UPGRADE = register("diamond_capacity_upgrade", p -> new CapacityUpgradeItem(p, 8), new Item.Properties());
    public static final CapacityUpgradeItem EMERALD_UPGRADE = register("emerald_capacity_upgrade", p -> new CapacityUpgradeItem(p, 16), new Item.Properties());
    public static final CapacityUpgradeItem NETHERITE_UPGRADE = register("netherite_capacity_upgrade", p -> new CapacityUpgradeItem(p, 32), new Item.Properties().fireResistant());
    /** Barrel upgrades, one per tier. */
    public static final java.util.Map<BarrelTier, BarrelUpgradeItem> BARREL_UPGRADES = new java.util.EnumMap<>(BarrelTier.class);

    static {
        for (BarrelTier tier : BarrelTier.values()) {
            BARREL_UPGRADES.put(tier, register(tier.id() + "_barrel_upgrade", p -> new BarrelUpgradeItem(p, tier),
                    tier == BarrelTier.NETHERITE ? new Item.Properties().fireResistant() : new Item.Properties()));
        }
    }

    public static final UpgradeItem VOID_UPGRADE = register("void_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.VOID), new Item.Properties());
    public static final UpgradeItem LOCK_UPGRADE = register("lock_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.LOCK), new Item.Properties());
    public static final UpgradeItem TRANSFER_SPEED_UPGRADE = register("transfer_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.TRANSFER_SPEED), new Item.Properties());
    public static final UpgradeItem CONDUIT_SPEED_UPGRADE = register("conduit_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.CONDUIT_SPEED), new Item.Properties());

    public static final StorageTabletItem CRAFTING_TABLET = register("crafting_storage_tablet", p -> new StorageTabletItem(p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DimensionalLinkCardItem LINK_CARD = register("dimensional_link_card", DimensionalLinkCardItem::new, new Item.Properties().stacksTo(1));

    public static final ConduitItem ITEM_CONDUIT = register("item_conduit", p -> new ConduitItem(p, ConduitType.ITEM, 1), new Item.Properties());
    public static final ConduitItem ENDER_ITEM_CONDUIT = register("ender_item_conduit", p -> new ConduitItem(p, ConduitType.ITEM, 2), new Item.Properties());
    public static final ConduitItem FLUID_CONDUIT = register("fluid_conduit", p -> new ConduitItem(p, ConduitType.FLUID, 1), new Item.Properties());
    public static final ConduitItem ENDER_FLUID_CONDUIT = register("ender_fluid_conduit", p -> new ConduitItem(p, ConduitType.FLUID, 2), new Item.Properties());
    public static final ConduitWrenchItem CONDUIT_WRENCH = register("conduit_wrench", ConduitWrenchItem::new, new Item.Properties().stacksTo(1));
    public static final FilterItem BASIC_FILTER = register("basic_filter", p -> new FilterItem(p, false), new Item.Properties().stacksTo(1));
    public static final FilterItem ADVANCED_FILTER = register("advanced_filter", p -> new FilterItem(p, true), new Item.Properties().stacksTo(1));

    /** All-in-one tools, one per material: {@code storagenexus:<tier>_aiot}. */
    public static final Map<AiotTier, AiotItem> AIOTS = new EnumMap<>(AiotTier.class);

    static {
        for (AiotTier tier : AiotTier.values()) {
            AIOTS.put(tier, register(tier.id() + "_aiot", p -> new AiotItem(p, tier), AiotItem.properties(tier)));
        }
    }

    /** AIOT modules, one per type: {@code storagenexus:<type>_module}. */
    public static final Map<ModuleType, ModuleItem> MODULES = new EnumMap<>(ModuleType.class);

    static {
        for (ModuleType type : ModuleType.values()) {
            MODULES.put(type, register(type.id() + "_module", p -> new ModuleItem(p, type), new Item.Properties().stacksTo(1)));
        }
    }

    /** Construction Builders, one per tier: {@code storagenexus:<tier>_builder}. */
    public static final Map<net.storagenexus.builder.BuilderTier, net.storagenexus.builder.BuilderItem> BUILDERS =
            new EnumMap<>(net.storagenexus.builder.BuilderTier.class);

    static {
        for (var tier : net.storagenexus.builder.BuilderTier.values()) {
            BUILDERS.put(tier, register(tier.id() + "_builder", p -> new net.storagenexus.builder.BuilderItem(p, tier),
                    net.storagenexus.builder.BuilderItem.properties(tier)));
        }
    }

    /**
     * What is left of the Vein Mining Module: vein mining needs no module now. Kept under its old id so AIOTs and
     * containers that still hold one load as before; it can't be crafted and isn't in the creative tab.
     */
    public static final RetiredModuleItem RETIRED_VEIN_MINING_MODULE = register("vein_mining_module", RetiredModuleItem::new,
            new Item.Properties().stacksTo(1));

    static {
        TAB_ORDER.remove(RETIRED_VEIN_MINING_MODULE);
    }

    // Nexium: the tier above netherite (see NexiumMaterials). Everything Nexium survives fire and lava.
    public static final Item NEXIUM_INGOT = register("nexium_ingot", Item::new, new Item.Properties().fireResistant());
    public static final BlockItem NEXIUM_BLOCK = register("nexium_block", p -> new BlockItem(ModBlocks.NEXIUM_BLOCK, p),
            new Item.Properties().useBlockDescriptionPrefix().fireResistant());
    public static final Item NEXIUM_SWORD = register("nexium_sword", Item::new,
            new Item.Properties().sword(NexiumMaterials.TOOL, 3.0F, -2.4F).fireResistant());
    public static final Item NEXIUM_PICKAXE = register("nexium_pickaxe", Item::new,
            new Item.Properties().pickaxe(NexiumMaterials.TOOL, 1.0F, -2.8F).fireResistant());
    public static final Item NEXIUM_AXE = register("nexium_axe", Item::new,
            new Item.Properties().axe(NexiumMaterials.TOOL, 5.0F, -3.0F).fireResistant());
    public static final Item NEXIUM_SHOVEL = register("nexium_shovel", Item::new,
            new Item.Properties().shovel(NexiumMaterials.TOOL, 1.5F, -3.0F).fireResistant());
    public static final Item NEXIUM_HOE = register("nexium_hoe", Item::new,
            new Item.Properties().hoe(NexiumMaterials.TOOL, -5.0F, 0.0F).fireResistant());
    /** One step past netherite on every spear stat, as netherite is one step past diamond. */
    public static final Item NEXIUM_SPEAR = register("nexium_spear", Item::new,
            new Item.Properties().spear(NexiumMaterials.TOOL, 1.25F, 1.325F, 0.3F, 2.0F, 8.0F, 4.5F, 5.1F, 7.5F, 4.6F).fireResistant());
    public static final Item NEXIUM_HELMET = register("nexium_helmet", Item::new,
            new Item.Properties().humanoidArmor(NexiumMaterials.ARMOR, ArmorType.HELMET).fireResistant());
    /** Also carries the set bonuses' settings and the Nexus Restock link. */
    public static final Item NEXIUM_CHESTPLATE = register("nexium_chestplate", NexiumChestplateItem::new,
            new Item.Properties().humanoidArmor(NexiumMaterials.ARMOR, ArmorType.CHESTPLATE).fireResistant());
    public static final Item NEXIUM_LEGGINGS = register("nexium_leggings", Item::new,
            new Item.Properties().humanoidArmor(NexiumMaterials.ARMOR, ArmorType.LEGGINGS).fireResistant());
    public static final Item NEXIUM_BOOTS = register("nexium_boots", Item::new,
            new Item.Properties().humanoidArmor(NexiumMaterials.ARMOR, ArmorType.BOOTS).fireResistant());

    static {
        // Block items go after the loose items so the tab starts with drawers.
        List<Item> blockItems = new ArrayList<>();
        for (Map.Entry<String, Block> e : ModBlocks.WITH_ITEMS.entrySet()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, StorageNexus.id(e.getKey()));
            BlockItem item = new BlockItem(e.getValue(), new Item.Properties().setId(key).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, key, item);
            blockItems.add(item);
        }
        TAB_ORDER.addAll(0, blockItems);
        TAB_ORDER.remove(GUIDE);
        TAB_ORDER.add(0, GUIDE); // the guide comes first
    }

    public static ConduitItem conduitFor(ConduitType type, int tier) {
        return switch (type) {
            case ITEM -> tier >= 2 ? ENDER_ITEM_CONDUIT : ITEM_CONDUIT;
            case FLUID -> tier >= 2 ? ENDER_FLUID_CONDUIT : FLUID_CONDUIT;
        };
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, StorageNexus.id(name));
        T item = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        TAB_ORDER.add(item);
        return item;
    }

    public static void init() {}

    private ModItems() {}
}
