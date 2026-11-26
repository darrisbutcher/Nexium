package net.nexium.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.nexium.Nexium;
import net.nexium.barrel.BarrelTier;
import net.nexium.barrel.TieredBarrelBlock;
import net.nexium.conduit.ConduitBundleBlock;
import net.nexium.dimension.DimensionalAnchorBlock;
import net.nexium.dimension.DimensionalInputBlock;
import net.nexium.dimension.DimensionalOutputBlock;
import net.nexium.drawer.CompactingDrawerBlock;
import net.nexium.drawer.ControllerSlaveBlock;
import net.nexium.drawer.DrawerBlock;
import net.nexium.drawer.DrawerControllerBlock;
import net.nexium.drawer.DrawerWood;
import net.nexium.elevator.ElevatorBlock;
import net.nexium.fluid.FluidDrawerBlock;
import net.nexium.freezer.FreezerBlock;
import net.nexium.fluid.FluidIntakeBlock;
import net.nexium.fluid.LavaSpringBlock;
import net.nexium.network.FilteredConnectorBlock;
import net.nexium.network.InventoryCableBlock;
import net.nexium.network.LevelEmitterBlock;
import net.nexium.network.NetworkBusBlock;
import net.nexium.network.StorageConnectorBlock;
import net.nexium.network.TerminalBlock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ModBlocks {
    /** Every block that gets a BlockItem, in creative-tab order. */
    public static final Map<String, Block> WITH_ITEMS = new LinkedHashMap<>();

    /** Drawers per wood type: index 0 = 1x1, 1 = 1x2, 2 = 2x2. */
    public static final Map<DrawerWood, DrawerBlock[]> DRAWERS = new EnumMap<>(DrawerWood.class);
    public static final List<DrawerBlock> ALL_DRAWERS = new ArrayList<>();

    static {
        for (DrawerWood wood : DrawerWood.values()) {
            DrawerBlock[] set = new DrawerBlock[3];
            int[] sizes = {1, 2, 4};
            String[] names = {"1x1", "1x2", "2x2"};
            for (int i = 0; i < 3; i++) {
                final int slots = sizes[i];
                set[i] = register(wood.id() + "_drawer_" + names[i], p -> new DrawerBlock(p, slots), wood(), true);
                ALL_DRAWERS.add(set[i]);
            }
            DRAWERS.put(wood, set);
        }
    }

    /** Iron, gold, diamond and netherite barrels. */
    public static final Map<BarrelTier, TieredBarrelBlock> BARRELS = new EnumMap<>(BarrelTier.class);

    static {
        for (BarrelTier tier : BarrelTier.values()) {
            BARRELS.put(tier, register(tier.id() + "_barrel", p -> new TieredBarrelBlock(p, tier),
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true));
        }
    }

    public static final CompactingDrawerBlock COMPACTING_DRAWER = register("compacting_drawer", CompactingDrawerBlock::new, storageStone(), true);
    public static final DrawerControllerBlock DRAWER_CONTROLLER = register("drawer_controller", DrawerControllerBlock::new, stone(), true);
    public static final ControllerSlaveBlock CONTROLLER_SLAVE = register("controller_slave", ControllerSlaveBlock::new, stone(), true);

    public static final FluidDrawerBlock FLUID_DRAWER_1X1 = register("fluid_drawer_1x1", p -> new FluidDrawerBlock(p, 1), glassy(), true);
    /** Fluid Tanks: 16, 64 and 256 buckets of one fluid. Their items are the Fluid Tank items (they can scoop and pour). */
    public static final net.nexium.fluid.FluidTankBlock IRON_FLUID_TANK = register("iron_fluid_tank", p -> new net.nexium.fluid.FluidTankBlock(p, 1),
            glassy().mapColor(MapColor.METAL).requiresCorrectToolForDrops(), false);
    public static final net.nexium.fluid.FluidTankBlock GOLD_FLUID_TANK = register("gold_fluid_tank", p -> new net.nexium.fluid.FluidTankBlock(p, 2),
            glassy().mapColor(MapColor.GOLD).requiresCorrectToolForDrops(), false);
    public static final net.nexium.fluid.FluidTankBlock DIAMOND_FLUID_TANK = register("diamond_fluid_tank", p -> new net.nexium.fluid.FluidTankBlock(p, 3),
            glassy().mapColor(MapColor.DIAMOND).requiresCorrectToolForDrops(), false);
    public static final LavaSpringBlock LAVA_SPRING = register("lava_spring", LavaSpringBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(5.0f, 1200.0f).sound(SoundType.STONE).lightLevel(s -> 15).requiresCorrectToolForDrops(), true);
    public static final FluidIntakeBlock FLUID_INTAKE = register("fluid_intake", FluidIntakeBlock::new, metal(), true);

    public static final StorageConnectorBlock STORAGE_CONNECTOR = register("storage_connector", StorageConnectorBlock::new, metal(), true);
    public static final FilteredConnectorBlock FILTERED_CONNECTOR = register("filtered_connector", FilteredConnectorBlock::new, metal(), true);
    public static final NetworkBusBlock NETWORK_BUS = register("import_export_bus", NetworkBusBlock::new, metal(), true);
    public static final LevelEmitterBlock LEVEL_EMITTER = register("level_emitter", LevelEmitterBlock::new, metal(), true);
    public static final InventoryCableBlock INVENTORY_CABLE = register("inventory_cable", InventoryCableBlock::new, cable(), true);
    public static final TerminalBlock CRAFTING_TERMINAL = register("crafting_terminal", p -> new TerminalBlock(p, true), metal(), true);

    /** Holds 8 storage disks on a storage network. */
    public static final net.nexium.disk.DiskDriveBlock DISK_DRIVE = register("disk_drive", net.nexium.disk.DiskDriveBlock::new, metal(), true);

    public static final DimensionalAnchorBlock DIMENSIONAL_ANCHOR = register("dimensional_anchor", DimensionalAnchorBlock::new, heavy(), true);
    public static final DimensionalInputBlock DIMENSIONAL_INPUT = register("dimensional_input", DimensionalInputBlock::new, heavy(), true);
    public static final DimensionalOutputBlock DIMENSIONAL_OUTPUT = register("dimensional_output", DimensionalOutputBlock::new, heavy(), true);

    /** A quarter-block Nexium platform; elevators in one column take you between floors. */
    public static final ElevatorBlock ELEVATOR = register("elevator", ElevatorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(s -> 6).requiresCorrectToolForDrops(), true);

    /** Makes Nexus Energy from daylight. */
    public static final net.nexium.energy.SolarPanelBlock SOLAR_PANEL = register("solar_panel", net.nexium.energy.SolarPanelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);
    /** Burns lava into Nexus Energy: 80 NE/t at Tier 1, 320 at Tier 2 and 1,280 at Tier 3. */
    public static final net.nexium.energy.ThermalGeneratorBlock THERMAL_GENERATOR = register("thermal_generator", p -> new net.nexium.energy.ThermalGeneratorBlock(p, 1),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.nexium.energy.ThermalGeneratorBlock.LIT) ? 15 : 4).requiresCorrectToolForDrops(), true);
    public static final net.nexium.energy.ThermalGeneratorBlock ADVANCED_THERMAL_GENERATOR = register("advanced_thermal_generator", p -> new net.nexium.energy.ThermalGeneratorBlock(p, 2),
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.nexium.energy.ThermalGeneratorBlock.LIT) ? 15 : 4).requiresCorrectToolForDrops(), true);
    public static final net.nexium.energy.ThermalGeneratorBlock NEXIUM_THERMAL_GENERATOR = register("nexium_thermal_generator", p -> new net.nexium.energy.ThermalGeneratorBlock(p, 3),
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.nexium.energy.ThermalGeneratorBlock.LIT) ? 15 : 4).requiresCorrectToolForDrops(), true);
    /** Shows the power dashboard of the Energy Conduit network it sits against. */
    public static final net.nexium.energy.PowerMeterBlock POWER_METER = register("power_meter", net.nexium.energy.PowerMeterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);
    /** Batteries: 250,000, 1,000,000 and 10,000,000 NE (and the Nexium one below). */
    public static final net.nexium.energy.BatteryBlock IRON_BATTERY = register("iron_battery", p -> new net.nexium.energy.BatteryBlock(p, 250_000),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);
    public static final net.nexium.energy.BatteryBlock GOLD_BATTERY = register("gold_battery", p -> new net.nexium.energy.BatteryBlock(p, 1_000_000),
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);
    public static final net.nexium.energy.BatteryBlock DIAMOND_BATTERY = register("diamond_battery", p -> new net.nexium.energy.BatteryBlock(p, 10_000_000),
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);

    /** The top Battery: 1,000,000,000 NE. */
    public static final net.nexium.energy.BatteryBlock NEXIUM_BATTERY = register("nexium_battery", p -> new net.nexium.energy.BatteryBlock(p, 1_000_000_000),
            BlockBehaviour.Properties.of().mapColor(MapColor.EMERALD).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);

    /** Charges battery cells, and the cells in a Jetpack or Tablet, from Nexus Energy. */
    public static final net.nexium.energy.ChargingStationBlock CHARGING_STATION = register("charging_station", net.nexium.energy.ChargingStationBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops(), true);

    /** Eight smelting slots at once on Nexus Energy. */
    public static final net.nexium.energy.NexusFurnaceBlock NEXUS_FURNACE = register("nexus_furnace", net.nexium.energy.NexusFurnaceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.nexium.energy.NexusFurnaceBlock.LIT) ? 13 : 0).requiresCorrectToolForDrops(), true);

    /** Burns furnace fuel into Nexus Energy. */
    public static final net.nexium.energy.CombustionGeneratorBlock COMBUSTION_GENERATOR = register("combustion_generator",
            net.nexium.energy.CombustionGeneratorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(net.nexium.energy.CombustionGeneratorBlock.LIT) ? 13 : 0).requiresCorrectToolForDrops(), true);

    /** The top generator: nine Redstone Fuel Rods, ice for coolant. */
    public static final net.nexium.energy.RedstoneReactorBlock REDSTONE_REACTOR = register("redstone_reactor",
            net.nexium.energy.RedstoneReactorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(4.0f, 8.0f).sound(SoundType.METAL)
                    .lightLevel(s -> 8).requiresCorrectToolForDrops(), true);

    /** Freezes water into ice, packed ice and blue ice; dyeable. */
    public static final FreezerBlock FREEZER = register("freezer", FreezerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops(), true);

    /** Placed by conduit items, so it has no BlockItem of its own. */
    public static final ConduitBundleBlock CONDUIT_BUNDLE = register("conduit_bundle", ConduitBundleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL)
                    .noOcclusion().dynamicShape(), false);

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD);
    }

    /** Nine Nexium ingots. Like a netherite block: needs a diamond pickaxe, resists explosions, a beacon base. Its item is in ModItems. */
    public static final Block NEXIUM_BLOCK = register("nexium_block", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(50.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops(), false);

    /**
     * Tinted glass that nothing but a player can break: it blocks light like tinted glass, drops itself with any tool, and the
     * Wither, the Ender Dragon and every explosion leave it alone (the {@code dragon_immune} and {@code wither_immune} block tags, and
     * bedrock's blast resistance).
     */
    public static final Block REINFORCED_GLASS = register("reinforced_glass", net.minecraft.world.level.block.TintedGlassBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.3F, 3_600_000.0F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos, box) -> false), true);

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    /** Stone, but drops with any tool: it carries its contents in the dropped item. */
    private static BlockBehaviour.Properties storageStone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties heavy() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 1200f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties glassy() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.GLASS).noOcclusion();
    }

    private static BlockBehaviour.Properties cable() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL).noOcclusion();
    }

    private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
                                                BlockBehaviour.Properties props, boolean withItem) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Nexium.id(name));
        T block = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        if (withItem) WITH_ITEMS.put(name, block);
        return block;
    }

    public static void init() {}

    private ModBlocks() {}
}
