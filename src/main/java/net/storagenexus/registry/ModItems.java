package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.storagenexus.StorageNexus;
import net.storagenexus.conduit.ConduitItem;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.conduit.ConduitWrenchItem;
import net.storagenexus.dimension.DimensionalLinkCardItem;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.tablet.StorageTabletItem;
import net.storagenexus.upgrade.CapacityUpgradeItem;
import net.storagenexus.upgrade.UpgradeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final List<Item> TAB_ORDER = new ArrayList<>();

    public static final CapacityUpgradeItem IRON_UPGRADE = register("iron_capacity_upgrade", p -> new CapacityUpgradeItem(p, 2), new Item.Properties());
    public static final CapacityUpgradeItem GOLD_UPGRADE = register("gold_capacity_upgrade", p -> new CapacityUpgradeItem(p, 4), new Item.Properties());
    public static final CapacityUpgradeItem DIAMOND_UPGRADE = register("diamond_capacity_upgrade", p -> new CapacityUpgradeItem(p, 8), new Item.Properties());
    public static final CapacityUpgradeItem NETHERITE_UPGRADE = register("netherite_capacity_upgrade", p -> new CapacityUpgradeItem(p, 16), new Item.Properties().fireResistant());
    public static final UpgradeItem VOID_UPGRADE = register("void_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.VOID), new Item.Properties());
    public static final UpgradeItem LOCK_UPGRADE = register("lock_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.LOCK), new Item.Properties());
    public static final UpgradeItem TRANSFER_SPEED_UPGRADE = register("transfer_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.TRANSFER_SPEED), new Item.Properties());
    public static final UpgradeItem INTAKE_RANGE_UPGRADE = register("intake_range_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.INTAKE_RANGE), new Item.Properties());
    public static final UpgradeItem CONDUIT_SPEED_UPGRADE = register("conduit_speed_upgrade", p -> new UpgradeItem(p, UpgradeItem.Kind.CONDUIT_SPEED), new Item.Properties());

    public static final StorageTabletItem STORAGE_TABLET = register("storage_tablet", p -> new StorageTabletItem(p, false), new Item.Properties().stacksTo(1));
    public static final StorageTabletItem CRAFTING_TABLET = register("crafting_storage_tablet", p -> new StorageTabletItem(p, true), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DimensionalLinkCardItem LINK_CARD = register("dimensional_link_card", DimensionalLinkCardItem::new, new Item.Properties().stacksTo(1));

    public static final ConduitItem ITEM_CONDUIT = register("item_conduit", p -> new ConduitItem(p, ConduitType.ITEM, 1), new Item.Properties());
    public static final ConduitItem ENDER_ITEM_CONDUIT = register("ender_item_conduit", p -> new ConduitItem(p, ConduitType.ITEM, 2), new Item.Properties());
    public static final ConduitItem FLUID_CONDUIT = register("fluid_conduit", p -> new ConduitItem(p, ConduitType.FLUID, 1), new Item.Properties());
    public static final ConduitItem PRESSURIZED_FLUID_CONDUIT = register("pressurized_fluid_conduit", p -> new ConduitItem(p, ConduitType.FLUID, 2), new Item.Properties());
    public static final ConduitItem ENDER_FLUID_CONDUIT = register("ender_fluid_conduit", p -> new ConduitItem(p, ConduitType.FLUID, 3), new Item.Properties());
    public static final ConduitWrenchItem CONDUIT_WRENCH = register("conduit_wrench", ConduitWrenchItem::new, new Item.Properties().stacksTo(1));
    public static final FilterItem BASIC_FILTER = register("basic_filter", p -> new FilterItem(p, false), new Item.Properties().stacksTo(1));
    public static final FilterItem ADVANCED_FILTER = register("advanced_filter", p -> new FilterItem(p, true), new Item.Properties().stacksTo(1));

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
    }

    public static ConduitItem conduitFor(ConduitType type, int tier) {
        return switch (type) {
            case ITEM -> tier >= 2 ? ENDER_ITEM_CONDUIT : ITEM_CONDUIT;
            case FLUID -> tier >= 3 ? ENDER_FLUID_CONDUIT : tier == 2 ? PRESSURIZED_FLUID_CONDUIT : FLUID_CONDUIT;
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
