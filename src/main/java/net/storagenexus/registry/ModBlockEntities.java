package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.storagenexus.StorageNexus;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.dimension.DimensionalAnchorBlockEntity;
import net.storagenexus.dimension.DimensionalInputBlockEntity;
import net.storagenexus.dimension.DimensionalOutputBlockEntity;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerControllerBlockEntity;
import net.storagenexus.elevator.ElevatorBlockEntity;
import net.storagenexus.freezer.FreezerBlockEntity;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.barrel.TieredBarrelBlockEntity;
import net.storagenexus.fluid.FluidIntakeBlockEntity;
import net.storagenexus.network.LevelEmitterBlockEntity;
import net.storagenexus.network.NetworkBusBlockEntity;
import net.storagenexus.network.ConnectorBlockEntity;

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

    public static final BlockEntityType<FreezerBlockEntity> FREEZER = register("freezer",
            new BlockEntityType<>(FreezerBlockEntity::new, Set.of(ModBlocks.FREEZER)));

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, StorageNexus.id(name), type);
    }

    public static void init() {}

    private ModBlockEntities() {}
}
