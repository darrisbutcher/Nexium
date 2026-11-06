package net.storagenexus.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.storagenexus.StorageNexus;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.barrel.TieredBarrelBlock;
import net.storagenexus.conduit.ConduitBundleBlock;
import net.storagenexus.dimension.DimensionalAnchorBlock;
import net.storagenexus.dimension.DimensionalInputBlock;
import net.storagenexus.dimension.DimensionalOutputBlock;
import net.storagenexus.drawer.CompactingDrawerBlock;
import net.storagenexus.drawer.ControllerSlaveBlock;
import net.storagenexus.drawer.DrawerBlock;
import net.storagenexus.drawer.DrawerControllerBlock;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.elevator.ElevatorBlock;
import net.storagenexus.fluid.FluidDrawerBlock;
import net.storagenexus.freezer.FreezerBlock;
import net.storagenexus.fluid.FluidIntakeBlock;
import net.storagenexus.fluid.LavaSpringBlock;
import net.storagenexus.network.FilteredConnectorBlock;
import net.storagenexus.network.InventoryCableBlock;
import net.storagenexus.network.LevelEmitterBlock;
import net.storagenexus.network.NetworkBusBlock;
import net.storagenexus.network.StorageConnectorBlock;
import net.storagenexus.network.TerminalBlock;

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
    public static final LavaSpringBlock LAVA_SPRING = register("lava_spring", LavaSpringBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(5.0f, 1200.0f).sound(SoundType.STONE).lightLevel(s -> 15).requiresCorrectToolForDrops(), true);
    public static final FluidIntakeBlock FLUID_INTAKE = register("fluid_intake", FluidIntakeBlock::new, metal(), true);

    public static final StorageConnectorBlock STORAGE_CONNECTOR = register("storage_connector", StorageConnectorBlock::new, metal(), true);
    public static final FilteredConnectorBlock FILTERED_CONNECTOR = register("filtered_connector", FilteredConnectorBlock::new, metal(), true);
    public static final NetworkBusBlock NETWORK_BUS = register("import_export_bus", NetworkBusBlock::new, metal(), true);
    public static final LevelEmitterBlock LEVEL_EMITTER = register("level_emitter", LevelEmitterBlock::new, metal(), true);
    public static final InventoryCableBlock INVENTORY_CABLE = register("inventory_cable", InventoryCableBlock::new, cable(), true);
    public static final TerminalBlock CRAFTING_TERMINAL = register("crafting_terminal", p -> new TerminalBlock(p, true), metal(), true);

    public static final DimensionalAnchorBlock DIMENSIONAL_ANCHOR = register("dimensional_anchor", DimensionalAnchorBlock::new, heavy(), true);
    public static final DimensionalInputBlock DIMENSIONAL_INPUT = register("dimensional_input", DimensionalInputBlock::new, heavy(), true);
    public static final DimensionalOutputBlock DIMENSIONAL_OUTPUT = register("dimensional_output", DimensionalOutputBlock::new, heavy(), true);

    /** A quarter-block Nexium platform; elevators in one column take you between floors. */
    public static final ElevatorBlock ELEVATOR = register("elevator", ElevatorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(s -> 6).requiresCorrectToolForDrops(), true);

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
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, StorageNexus.id(name));
        T block = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        if (withItem) WITH_ITEMS.put(name, block);
        return block;
    }

    public static void init() {}

    private ModBlocks() {}
}
