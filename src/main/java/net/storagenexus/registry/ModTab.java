package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.StorageNexus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The mod's three creative tabs: Storage (drawers, barrels, the network, disks, upgrades, item and fluid conduits; the
 * guide comes first), Tools (AIOTs, modules, Construction Builders, Nexium gear and armor, the magnet, the elixir, the
 * Elevator and the Dimensional Anchor) and Farms and Power (every farm, the Farm Assembler, the Freezer, generators,
 * Batteries, the Reactor and its rods, Energy Conduits). An item that isn't named below goes in Storage.
 */
public final class ModTab {
    public enum Group { STORAGE, TOOLS, FARMS_AND_POWER }

    private static final Set<String> TOOLS = Set.of("magnet", "bedrock_breaker_elixir", "elevator", "dimensional_anchor",
            "nexus_link_module", "area_mining_module", "nexium_ingot", "nexium_block", "nexium_sword", "nexium_pickaxe", "nexium_axe",
            "nexium_shovel", "nexium_hoe", "nexium_spear", "nexium_helmet", "nexium_chestplate", "nexium_leggings", "nexium_boots");

    private static final Set<String> POWER = Set.of("freezer", "solar_panel", "thermal_generator", "combustion_generator",
            "iron_battery", "gold_battery", "diamond_battery", "redstone_reactor", "redstone_fuel_rod", "spent_fuel_rod",
            "energy_conduit", "ender_energy_conduit", "farm_assembler");

    /** Which tab an item belongs to. */
    public static Group groupOf(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (net.storagenexus.essentials.registry.ModItems.FARMS.containsValue(item)
                || item == net.storagenexus.essentials.registry.ModItems.IRON_FARM || POWER.contains(path)) return Group.FARMS_AND_POWER;
        if (TOOLS.contains(path) || path.endsWith("_aiot") || path.endsWith("_builder")) return Group.TOOLS;
        return Group.STORAGE;
    }

    /** Every item the tabs show, in the order they were registered (the Nexium items first, then the Essentials items). */
    private static List<Item> items(Group group) {
        List<Item> out = new ArrayList<>();
        for (Item item : ModItems.TAB_ORDER) if (groupOf(item) == group) out.add(item);
        for (Item item : net.storagenexus.essentials.registry.ModItems.TAB_ORDER) if (groupOf(item) == group) out.add(item);
        return out;
    }

    private static CreativeModeTab tab(String id, String title, int column, java.util.function.Supplier<ItemStack> icon, Group group) {
        return Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, StorageNexus.id(id),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, column)
                        .title(Component.translatable(title))
                        .icon(icon)
                        .displayItems((params, output) -> {
                            for (Item item : items(group)) output.accept(item);
                        })
                        .build());
    }

    /** Storage: the original tab, so its id stays {@code storagenexus:main}. */
    public static final CreativeModeTab TAB = tab("main", "itemGroup.storagenexus", 0,
            () -> new ItemStack(ModBlocks.DRAWER_CONTROLLER), Group.STORAGE);
    public static final CreativeModeTab TOOLS_TAB = tab("tools", "itemGroup.storagenexus.tools", 1,
            () -> new ItemStack(ModItems.AIOTS.get(net.storagenexus.aiot.AiotTier.NEXIUM)), Group.TOOLS);
    public static final CreativeModeTab FARMS_TAB = tab("farms_power", "itemGroup.storagenexus.farms_power", 2,
            () -> new ItemStack(ModBlocks.REDSTONE_REACTOR), Group.FARMS_AND_POWER);

    public static void init() {}

    private ModTab() {}
}
