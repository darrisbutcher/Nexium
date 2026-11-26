package net.nexium.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.nexium.Nexium;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.dimension.DimensionalAnchorBlockEntity;
import net.nexium.dimension.DimensionalInputBlockEntity;
import net.nexium.dimension.DimensionalOutputBlockEntity;
import net.nexium.drawer.CompactingDrawerBlockEntity;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.drawer.DrawerControllerBlockEntity;
import net.nexium.elevator.ElevatorBlockEntity;
import net.nexium.freezer.FreezerBlockEntity;
import net.nexium.fluid.FluidDrawerBlockEntity;
import net.nexium.barrel.TieredBarrelBlockEntity;
import net.nexium.fluid.FluidIntakeBlockEntity;
import net.nexium.network.LevelEmitterBlockEntity;
import net.nexium.network.NetworkBusBlockEntity;
import net.nexium.network.ConnectorBlockEntity;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<DrawerBlockEntity> DRAWER = register("drawer",
            new BlockEntityType<>(DrawerBlockEntity::new, Set.copyOf(ModBlocks.ALL_DRAWERS)));
    public static final BlockEntityType<CompactingDrawerBlockEntity> COMPACTING_DRAWER = register("compacting_drawer",
            new BlockEntityType<>(CompactingDrawerBlockEntity::new, Set.of(ModBlocks.COMPACTING_DRAWER)));
    public static final BlockEntityType<DrawerControllerBlockEntity> DRAWER_CONTROLLER = register("drawer_controller",
            new BlockEntityType<>(DrawerControllerBlockEntity::new, Set.of(ModBlocks.DRAWER_CONTROLLER)));
    public static final BlockEntityType<FluidDrawerBlockEntity> FLUID_DRAWER = register("fluid_drawer",
            new BlockEntityType<>(FluidDrawerBlockEntity::new, Set.<Block>of(ModBlocks.FLUID_DRAWER_1X1)));
    public static final BlockEntityType<NetworkBusBlockEntity> NETWORK_BUS = register("network_bus",
            new BlockEntityType<>(NetworkBusBlockEntity::new, Set.of(ModBlocks.NETWORK_BUS)));
    public static final BlockEntityType<LevelEmitterBlockEntity> LEVEL_EMITTER = register("level_emitter",
            new BlockEntityType<>(LevelEmitterBlockEntity::new, Set.of(ModBlocks.LEVEL_EMITTER)));
    public static final BlockEntityType<ElevatorBlockEntity> ELEVATOR = register("elevator",
            new BlockEntityType<>(ElevatorBlockEntity::new, Set.of(ModBlocks.ELEVATOR)));
    public static final BlockEntityType<TieredBarrelBlockEntity> TIERED_BARREL = register("tiered_barrel",
            new BlockEntityType<>(TieredBarrelBlockEntity::new, new java.util.HashSet<Block>(ModBlocks.BARRELS.values())));
    public static final BlockEntityType<FluidIntakeBlockEntity> FLUID_INTAKE = register("fluid_intake",
            new BlockEntityType<>(FluidIntakeBlockEntity::new, Set.of(ModBlocks.FLUID_INTAKE)));
    public static final BlockEntityType<ConnectorBlockEntity> CONNECTOR = register("connector",
            new BlockEntityType<>(ConnectorBlockEntity::new, Set.<Block>of(ModBlocks.STORAGE_CONNECTOR, ModBlocks.FILTERED_CONNECTOR)));
    public static final BlockEntityType<DimensionalAnchorBlockEntity> DIMENSIONAL_ANCHOR = register("dimensional_anchor",
            new BlockEntityType<>(DimensionalAnchorBlockEntity::new, Set.of(ModBlocks.DIMENSIONAL_ANCHOR)));
    public static final BlockEntityType<DimensionalInputBlockEntity> DIMENSIONAL_INPUT = register("dimensional_input",
            new BlockEntityType<>(DimensionalInputBlockEntity::new, Set.of(ModBlocks.DIMENSIONAL_INPUT)));
    public static final BlockEntityType<DimensionalOutputBlockEntity> DIMENSIONAL_OUTPUT = register("dimensional_output",
            new BlockEntityType<>(DimensionalOutputBlockEntity::new, Set.of(ModBlocks.DIMENSIONAL_OUTPUT)));
    public static final BlockEntityType<ConduitBundleBlockEntity> CONDUIT_BUNDLE = register("conduit_bundle",
            new BlockEntityType<>(ConduitBundleBlockEntity::new, Set.of(ModBlocks.CONDUIT_BUNDLE)));

    public static final BlockEntityType<net.nexium.disk.DiskDriveBlockEntity> DISK_DRIVE = register("disk_drive",
            new BlockEntityType<>(net.nexium.disk.DiskDriveBlockEntity::new, Set.of(ModBlocks.DISK_DRIVE)));

    public static final BlockEntityType<net.nexium.energy.SolarPanelBlockEntity> SOLAR_PANEL = register("solar_panel",
            new BlockEntityType<>(net.nexium.energy.SolarPanelBlockEntity::new, Set.of(ModBlocks.SOLAR_PANEL)));
    public static final BlockEntityType<net.nexium.fluid.FluidTankBlockEntity> FLUID_TANK = register("fluid_tank",
            new BlockEntityType<>(net.nexium.fluid.FluidTankBlockEntity::new,
                    Set.of(ModBlocks.IRON_FLUID_TANK, ModBlocks.GOLD_FLUID_TANK, ModBlocks.DIAMOND_FLUID_TANK)));
    public static final BlockEntityType<net.nexium.energy.ThermalGeneratorBlockEntity> THERMAL_GENERATOR = register("thermal_generator",
            new BlockEntityType<>(net.nexium.energy.ThermalGeneratorBlockEntity::new,
                    Set.of(ModBlocks.THERMAL_GENERATOR, ModBlocks.ADVANCED_THERMAL_GENERATOR, ModBlocks.NEXIUM_THERMAL_GENERATOR)));
    public static final BlockEntityType<net.nexium.energy.ChargingStationBlockEntity> CHARGING_STATION = register("charging_station",
            new BlockEntityType<>(net.nexium.energy.ChargingStationBlockEntity::new, Set.of(ModBlocks.CHARGING_STATION)));
    public static final BlockEntityType<net.nexium.energy.BatteryBlockEntity> BATTERY = register("battery",
            new BlockEntityType<>(net.nexium.energy.BatteryBlockEntity::new, Set.of(ModBlocks.IRON_BATTERY, ModBlocks.GOLD_BATTERY, ModBlocks.DIAMOND_BATTERY)));
    public static final BlockEntityType<net.nexium.energy.RedstoneReactorBlockEntity> REDSTONE_REACTOR = register("redstone_reactor",
            new BlockEntityType<>(net.nexium.energy.RedstoneReactorBlockEntity::new, Set.of(ModBlocks.REDSTONE_REACTOR)));
    public static final BlockEntityType<net.nexium.energy.NexusFurnaceBlockEntity> NEXUS_FURNACE = register("nexus_furnace",
            new BlockEntityType<>(net.nexium.energy.NexusFurnaceBlockEntity::new, Set.of(ModBlocks.NEXUS_FURNACE)));
    public static final BlockEntityType<net.nexium.energy.CombustionGeneratorBlockEntity> COMBUSTION_GENERATOR = register("combustion_generator",
            new BlockEntityType<>(net.nexium.energy.CombustionGeneratorBlockEntity::new, Set.of(ModBlocks.COMBUSTION_GENERATOR)));

    public static final BlockEntityType<FreezerBlockEntity> FREEZER = register("freezer",
            new BlockEntityType<>(FreezerBlockEntity::new, Set.of(ModBlocks.FREEZER)));

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Nexium.id(name), type);
    }

    public static void init() {}

    private ModBlockEntities() {}
}
