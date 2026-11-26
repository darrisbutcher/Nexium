package net.nexium.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.nexium.Nexium;
import net.nexium.aiot.AiotItem;
import net.nexium.aiot.AiotTier;
import net.nexium.aiot.module.ModuleItem;
import net.nexium.aiot.module.RetiredModuleItem;
import net.nexium.aiot.module.ModuleType;
import net.nexium.barrel.BarrelTier;
import net.nexium.barrel.BarrelUpgradeItem;
import net.nexium.conduit.ConduitItem;
import net.nexium.conduit.LegacyConduitItem;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.ConduitWrenchItem;
import net.nexium.dimension.DimensionalLinkCardItem;
import net.nexium.filter.FilterItem;
import net.nexium.guide.GuideBookItem;
import net.nexium.gear.NexiumChestplateItem;
import net.nexium.gear.NexiumMaterials;
import net.nexium.tablet.StorageTabletItem;
import net.nexium.upgrade.CapacityUpgradeItem;
import net.nexium.upgrade.UpgradeItem;

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
    /** The Drawer Key. Its id is still lock_upgrade: the Lock Upgrade of older worlds is this item now. */
    public static final net.nexium.upgrade.DrawerKeyItem DRAWER_KEY = register("lock_upgrade", net.nexium.upgrade.DrawerKeyItem::new, new Item.Properties());
    public static final UpgradeItem TRANSFER_SPEED_UPGRADE = register("transfer_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.TRANSFER_SPEED), new Item.Properties());
    public static final UpgradeItem CONDUIT_SPEED_UPGRADE = register("conduit_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.CONDUIT_SPEED), new Item.Properties());

    public static final StorageTabletItem CRAFTING_TABLET = register("crafting_storage_tablet", p -> new StorageTabletItem(p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DimensionalLinkCardItem LINK_CARD = register("dimensional_link_card", DimensionalLinkCardItem::new, new Item.Properties().stacksTo(1));

    /** The Conduit: one item that carries items, fluids and energy together. */
    public static final ConduitItem CONDUIT = register("conduit", ConduitItem::new, new Item.Properties());
    /** Makes a placed conduit Tier 2. */
    public static final net.nexium.conduit.ConduitUpgradeItem ADVANCED_CONDUIT_UPGRADE = register("advanced_conduit_upgrade",
            net.nexium.conduit.ConduitUpgradeItem::new, new Item.Properties());
    // The six conduits from before the Conduit became one item: not in the creative tab, converted when carried
    public static final LegacyConduitItem ITEM_CONDUIT = legacy("item_conduit", ConduitType.ITEM, 1);
    public static final LegacyConduitItem ENDER_ITEM_CONDUIT = legacy("ender_item_conduit", ConduitType.ITEM, 2);
    public static final LegacyConduitItem FLUID_CONDUIT = legacy("fluid_conduit", ConduitType.FLUID, 1);
    public static final LegacyConduitItem ENDER_FLUID_CONDUIT = legacy("ender_fluid_conduit", ConduitType.FLUID, 2);
    public static final LegacyConduitItem ENERGY_CONDUIT = legacy("energy_conduit", ConduitType.ENERGY, 1);
    public static final LegacyConduitItem ENDER_ENERGY_CONDUIT = legacy("ender_energy_conduit", ConduitType.ENERGY, 2);
    public static final ConduitWrenchItem CONDUIT_WRENCH = register("conduit_wrench", ConduitWrenchItem::new, new Item.Properties().stacksTo(1));
    /** Shows everything on a conduit's network in a window; changes nothing. */
    public static final net.nexium.conduit.ConduitScannerItem CONDUIT_SCANNER = register("conduit_scanner", net.nexium.conduit.ConduitScannerItem::new, new Item.Properties().stacksTo(1));
    public static final FilterItem BASIC_FILTER = register("basic_filter", p -> new FilterItem(p, false), new Item.Properties().stacksTo(1));
    public static final FilterItem ADVANCED_FILTER = register("advanced_filter", p -> new FilterItem(p, true), new Item.Properties().stacksTo(1));

    /** All-in-one tools, one per material: {@code nexium:<tier>_aiot}. */
    public static final Map<AiotTier, AiotItem> AIOTS = new EnumMap<>(AiotTier.class);

    static {
        for (AiotTier tier : AiotTier.values()) {
            AIOTS.put(tier, register(tier.id() + "_aiot", p -> new AiotItem(p, tier), AiotItem.properties(tier)));
        }
    }

    /** AIOT modules, one per type: {@code nexium:<type>_module}. */
    public static final Map<ModuleType, ModuleItem> MODULES = new EnumMap<>(ModuleType.class);

    static {
        for (ModuleType type : ModuleType.values()) {
            MODULES.put(type, register(type.id() + "_module", p -> new ModuleItem(p, type), new Item.Properties().stacksTo(1)));
        }
    }

    // Digital storage: disks for the Disk Drive, made from a casing and a storage part
    public static final Item DISK_CASING = register("disk_casing", Item::new, new Item.Properties());
    public static final Map<net.nexium.disk.DiskTier, Item> STORAGE_PARTS = new EnumMap<>(net.nexium.disk.DiskTier.class);
    public static final Map<net.nexium.disk.DiskTier, Item> FLUID_PARTS = new EnumMap<>(net.nexium.disk.DiskTier.class);
    public static final Map<net.nexium.disk.DiskTier, net.nexium.disk.StorageDiskItem> STORAGE_DISKS =
            new EnumMap<>(net.nexium.disk.DiskTier.class);
    public static final Map<net.nexium.disk.DiskTier, net.nexium.disk.StorageDiskItem> FLUID_DISKS =
            new EnumMap<>(net.nexium.disk.DiskTier.class);

    static {
        for (var tier : net.nexium.disk.DiskTier.values()) {
            Item.Properties props = tier == net.nexium.disk.DiskTier.NEXIUM ? new Item.Properties().fireResistant() : new Item.Properties();
            STORAGE_PARTS.put(tier, register(tier.id() + "_storage_part", Item::new, props));
            FLUID_PARTS.put(tier, register(tier.id() + "_fluid_part", Item::new,
                    tier == net.nexium.disk.DiskTier.NEXIUM ? new Item.Properties().fireResistant() : new Item.Properties()));
        }
        for (var tier : net.nexium.disk.DiskTier.values()) {
            STORAGE_DISKS.put(tier, register(tier.id() + "_storage_disk", p -> new net.nexium.disk.StorageDiskItem(p, tier, false),
                    tier == net.nexium.disk.DiskTier.NEXIUM ? new Item.Properties().stacksTo(1).fireResistant() : new Item.Properties().stacksTo(1)));
        }
        for (var tier : net.nexium.disk.DiskTier.values()) {
            FLUID_DISKS.put(tier, register(tier.id() + "_fluid_disk", p -> new net.nexium.disk.StorageDiskItem(p, tier, true),
                    tier == net.nexium.disk.DiskTier.NEXIUM ? new Item.Properties().stacksTo(1).fireResistant() : new Item.Properties().stacksTo(1)));
        }
    }

    /** Construction Builders, one per tier: {@code nexium:<tier>_builder}. */
    public static final Map<net.nexium.builder.BuilderTier, net.nexium.builder.BuilderItem> BUILDERS =
            new EnumMap<>(net.nexium.builder.BuilderTier.class);

    static {
        for (var tier : net.nexium.builder.BuilderTier.values()) {
            BUILDERS.put(tier, register(tier.id() + "_builder", p -> new net.nexium.builder.BuilderItem(p, tier),
                    net.nexium.builder.BuilderItem.properties(tier)));
        }
    }

    /**
     * What is left of the Vein Mining Module: vein mining needs no module now. Kept under its old id so AIOTs and
     * containers that still hold one load as before; it can't be crafted and isn't in the creative tab.
     */
    public static final RetiredModuleItem RETIRED_VEIN_MINING_MODULE = register("vein_mining_module", p -> new RetiredModuleItem(p, net.minecraft.world.item.Items.LAPIS_LAZULI),
            new Item.Properties().stacksTo(1));

    /**
     * What is left of the 3x3 Mining Module: 3x3 mining is built into every AIOT now. Kept under its old id like the Vein Mining
     * Module; an AIOT that still holds one keeps its 3x3 setting and tunnel length and hands the module back as its materials.
     */
    public static final RetiredModuleItem RETIRED_AREA_MINING_MODULE = register("area_mining_module", p -> new RetiredModuleItem(p, net.minecraft.world.item.Items.REDSTONE),
            new Item.Properties().stacksTo(1));

    static {
        TAB_ORDER.remove(RETIRED_VEIN_MINING_MODULE);
        TAB_ORDER.remove(RETIRED_AREA_MINING_MODULE);
    }

    // Nexium: the tier above netherite (see NexiumMaterials). Everything Nexium survives fire and lava.
    /** Fuel for the Redstone Reactor; it wears down as the reactor runs and leaves a Spent Fuel Rod. */
    public static final Item REDSTONE_FUEL_ROD = register("redstone_fuel_rod", Item::new,
            new Item.Properties().durability(net.nexium.energy.RedstoneReactorBlockEntity.ROD_LIFE));
    public static final Item SPENT_FUEL_ROD = register("spent_fuel_rod", Item::new, new Item.Properties());
    /** Small batteries for the Jetpack and the Crafting Storage Tablet, sized like the Iron, Gold and Diamond Battery blocks. */
    public static final Item IRON_CELL = register("iron_cell", p -> new net.nexium.energy.BatteryCellItem(p, 250_000), new Item.Properties().stacksTo(1));
    public static final Item GOLD_CELL = register("gold_cell", p -> new net.nexium.energy.BatteryCellItem(p, 1_000_000), new Item.Properties().stacksTo(1));
    public static final Item DIAMOND_CELL = register("diamond_cell", p -> new net.nexium.energy.BatteryCellItem(p, 10_000_000), new Item.Properties().stacksTo(1));
    /**
     * Flies on a battery cell kept in its own screen. Worn in the wings slot (or the chest slot): its equipment asset draws it on the back,
     * and it takes the armor enchantments an elytra does.
     */
    public static final Item JETPACK = register("jetpack", net.nexium.jetpack.JetpackItem::new, new Item.Properties().stacksTo(1).enchantable(15)
            .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.CHEST)
                            .setEquipSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON)
                            .setDamageOnHurt(false).build()));
    /** The Fluid Tanks (16, 64 and 256 buckets): blocks, and big buckets in the hand. */
    public static final net.nexium.fluid.FluidTankItem IRON_FLUID_TANK = register("iron_fluid_tank",
            p -> new net.nexium.fluid.FluidTankItem(ModBlocks.IRON_FLUID_TANK, p, 1), new Item.Properties().useBlockDescriptionPrefix());
    public static final net.nexium.fluid.FluidTankItem GOLD_FLUID_TANK = register("gold_fluid_tank",
            p -> new net.nexium.fluid.FluidTankItem(ModBlocks.GOLD_FLUID_TANK, p, 2), new Item.Properties().useBlockDescriptionPrefix());
    public static final net.nexium.fluid.FluidTankItem DIAMOND_FLUID_TANK = register("diamond_fluid_tank",
            p -> new net.nexium.fluid.FluidTankItem(ModBlocks.DIAMOND_FLUID_TANK, p, 3), new Item.Properties().useBlockDescriptionPrefix());
    /** A ninth of a diamond: what worn diamond tools and armor smelt down to. */
    public static final Item DIAMOND_NUGGET = register("diamond_nugget", Item::new, new Item.Properties());
    /** A ninth of a Nexium Ingot: the centre of a Redstone Fuel Rod. */
    public static final Item NEXIUM_NUGGET = register("nexium_nugget", Item::new, new Item.Properties().fireResistant());
    public static final Item NEXIUM_INGOT = register("nexium_ingot", Item::new, new Item.Properties().fireResistant());
    public static final BlockItem NEXIUM_BLOCK = register("nexium_block", p -> new BlockItem(ModBlocks.NEXIUM_BLOCK, p),
            new Item.Properties().useBlockDescriptionPrefix().fireResistant());
    /** Armor for the dog (and only the dog): Nexium's own protection, worn on the body slot like Wolf Armor. */
    public static final Item NEXIUM_DOG_ARMOR = register("nexium_dog_armor", Item::new,
            new Item.Properties().wolfArmor(NexiumMaterials.ARMOR).fireResistant().component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                    net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.BODY)
                            .setEquipSound(NexiumMaterials.ARMOR.equipSound()).setAsset(NexiumMaterials.EQUIPMENT_ASSET)
                            .setAllowedEntities(net.nexium.dog.ModDogs.DOG).setCanBeSheared(true)
                            .setShearingSound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.ARMOR_UNEQUIP_WOLF)).build()));
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
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Nexium.id(e.getKey()));
            BlockItem item = new BlockItem(e.getValue(), new Item.Properties().setId(key).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, key, item);
            blockItems.add(item);
        }
        TAB_ORDER.addAll(0, blockItems);
        TAB_ORDER.remove(GUIDE);
        TAB_ORDER.add(0, GUIDE); // the guide comes first
    }

    private static LegacyConduitItem legacy(String name, ConduitType type, int tier) {
        LegacyConduitItem item = register(name, p -> new LegacyConduitItem(p, type, tier), new Item.Properties());
        TAB_ORDER.remove(item);
        return item;
    }

    /** The old per-type conduit item, for naming a conduit that has only some of the three types. */
    public static LegacyConduitItem conduitFor(ConduitType type, int tier) {
        return switch (type) {
            case ITEM -> tier >= 2 ? ENDER_ITEM_CONDUIT : ITEM_CONDUIT;
            case FLUID -> tier >= 2 ? ENDER_FLUID_CONDUIT : FLUID_CONDUIT;
            case ENERGY -> tier >= 2 ? ENDER_ENERGY_CONDUIT : ENERGY_CONDUIT;
        };
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Nexium.id(name));
        T item = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        TAB_ORDER.add(item);
        return item;
    }

    public static void init() {}

    private ModItems() {}
}
