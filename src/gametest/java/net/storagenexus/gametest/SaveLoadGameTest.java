package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.barrel.TieredBarrelBlockEntity;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.conduit.SideConfig;
import net.storagenexus.dimension.DimensionalAnchorBlockEntity;
import net.storagenexus.dimension.DimensionalInputBlockEntity;
import net.storagenexus.dimension.DimensionalOutputBlockEntity;
import net.storagenexus.dimension.LinkRegistry;
import net.storagenexus.dimension.LinkedBlockEntity;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmTimers;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.fluid.FluidIntakeBlockEntity;
import net.storagenexus.network.ConnectorBlockEntity;
import net.storagenexus.network.LevelEmitterBlockEntity;
import net.storagenexus.network.NetworkBusBlockEntity;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModItems;
import net.storagenexus.upgrade.Upgradeable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Every block entity in the mod, saved and loaded back the way a chunk save and reload (or reopening the world) does.
 * <p>
 * For each thing a block entity keeps, the test changes just that one thing on a freshly placed block and checks that
 * <ol>
 *   <li>what gets saved changes, so it really is saved (a barrel's items once were not, and vanished on relog), and</li>
 *   <li>loading that save into a new block entity and saving again gives exactly the same data, so it is loaded back.</li>
 * </ol>
 * A Nexium or Essentials block entity type with no case here fails the test, so a new one can't be missed.
 */
public class SaveLoadGameTest {
    private static final BlockPos POS = new BlockPos(3, 1, 3);

    /** One thing a block entity keeps. {@code prepare} runs before the "before" save, {@code change} between the two saves. */
    private record Part(String what, Consumer<BlockEntity> prepare, Consumer<BlockEntity> change) {}

    private record Case(String name, Block block, List<Part> parts) {}

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> Part part(Class<T> type, String what, Consumer<T> prepare, Consumer<T> change) {
        return new Part(what, be -> prepare.accept((T) be), be -> change.accept((T) be));
    }

    private static <T extends BlockEntity> Part part(Class<T> type, String what, Consumer<T> change) {
        return part(type, what, be -> {}, change);
    }

    @GameTest(maxTicks = 20)
    public void everyBlockEntitySavesAndLoadsWhatItKeeps(GameTestHelper h) {
        HolderLookup.Provider registries = h.getLevel().registryAccess();
        List<String> problems = new ArrayList<>();
        Set<BlockEntityType<?>> covered = new java.util.HashSet<>();
        int checked = 0;

        for (Case c : cases(h)) {
            List<Part> parts = c.parts().isEmpty() ? List.of(new Part("nothing (it keeps no state)", be -> {}, be -> {})) : c.parts();
            for (Part p : parts) {
                String label = c.name() + " / " + p.what();
                h.setBlock(POS, Blocks.AIR);
                h.setBlock(POS, c.block());
                BlockEntity be = h.getLevel().getBlockEntity(h.absolutePos(POS));
                if (be == null) {
                    problems.add(label + ": no block entity was placed");
                    continue;
                }
                covered.add(be.getType());
                try {
                    p.prepare().accept(be);
                    CompoundTag before = be.saveWithFullMetadata(registries);
                    p.change().accept(be);
                    CompoundTag after = be.saveWithFullMetadata(registries);
                    if (!c.parts().isEmpty() && after.equals(before)) {
                        problems.add(label + ": changing it doesn't change what is saved, so it is lost on relog");
                    }
                    BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(POS), h.getBlockState(POS), after, registries);
                    if (loaded == null) {
                        problems.add(label + ": the save doesn't load back into a block entity");
                    } else {
                        CompoundTag again = loaded.saveWithFullMetadata(registries);
                        if (!again.equals(after)) problems.add(label + ": loading changes it\n  saved:  " + after + "\n  loaded: " + again);
                    }
                    checked++;
                } catch (RuntimeException e) {
                    problems.add(label + ": " + e);
                }
                // Clear the spot (an anchor drops its chunk ticket) and anything that spilled
                h.setBlock(POS, Blocks.AIR);
                h.setBlock(POS.east(), Blocks.AIR);
                h.killAllEntitiesOfClass(ItemEntity.class);
            }
        }

        Set<String> missing = new TreeSet<>();
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            String namespace = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace();
            if ((namespace.equals("storagenexus") || namespace.equals("nexusessentials")) && !covered.contains(type)) {
                missing.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
            }
        }
        if (!missing.isEmpty()) problems.add("no save/load case for " + missing + ": add one to SaveLoadGameTest.cases");

        h.assertTrue(problems.isEmpty(), problems.size() + " save/load problem(s):\n" + String.join("\n", problems));
        h.assertTrue(checked > 0, "nothing was checked");
        h.succeed();
    }

    private static List<Case> cases(GameTestHelper h) {
        ItemStack filter = new ItemStack(ModItems.BASIC_FILTER);
        Block drawer1x1 = ModBlocks.DRAWERS.get(DrawerWood.OAK)[0];
        Block drawer2x2 = ModBlocks.DRAWERS.get(DrawerWood.OAK)[2];
        LinkRegistry links = LinkRegistry.get(h.getLevel().getServer());
        List<Case> cases = new ArrayList<>();

        cases.add(new Case("Drawer 1x1", drawer1x1, List.of(
                part(DrawerBlockEntity.class, "items", be -> insert(be.slot(0), Items.DIAMOND, 10)),
                part(DrawerBlockEntity.class, "capacity upgrade", be -> upgrade(be, 0, ModItems.IRON_UPGRADE)),
                // The locked item is only set when the lock goes on with something inside; empty it again so only the lock differs
                part(DrawerBlockEntity.class, "locked item", be -> upgrade(be, Upgradeable.LOCK_SLOT, ModItems.LOCK_UPGRADE), be -> {
                    upgrade(be, Upgradeable.LOCK_SLOT, null);
                    insert(be.slot(0), Items.DIAMOND, 10);
                    upgrade(be, Upgradeable.LOCK_SLOT, ModItems.LOCK_UPGRADE);
                    extract(be.slot(0), Items.DIAMOND, 10);
                }))));
        cases.add(new Case("Drawer 2x2", drawer2x2, List.of(
                part(DrawerBlockEntity.class, "items in the last section", be -> insert(be.slot(be.slotCount() - 1), Items.EMERALD, 7)))));

        cases.add(new Case("Elevator", ModBlocks.ELEVATOR, List.of(
                part(net.storagenexus.elevator.ElevatorBlockEntity.class, "floor name", be -> be.setFloorName("Roof")))));

        cases.add(new Case("Compacting Drawer", ModBlocks.COMPACTING_DRAWER, List.of(
                part(CompactingDrawerBlockEntity.class, "contents", be -> {
                    try (Transaction tx = Transaction.openOuter()) {
                        be.getItemStorage().insert(ItemVariant.of(Items.IRON_INGOT), 9, tx);
                        tx.commit();
                    }
                }),
                part(CompactingDrawerBlockEntity.class, "capacity upgrade", be -> {
                    be.getUpgrades().set(0, new ItemStack(ModItems.IRON_UPGRADE));
                    be.onUpgradesChanged();
                }))));

        cases.add(new Case("Fluid Drawer", ModBlocks.FLUID_DRAWER_1X1, List.of(
                part(FluidDrawerBlockEntity.class, "fluid", be -> insertFluid(be.slot(0), FluidConstants.BUCKET)),
                part(FluidDrawerBlockEntity.class, "capacity upgrade", be -> {
                    be.getUpgrades().set(0, new ItemStack(ModItems.IRON_UPGRADE));
                    be.onUpgradesChanged();
                }),
                part(FluidDrawerBlockEntity.class, "locked fluid", be -> fluidLock(be, true), be -> {
                    fluidLock(be, false);
                    insertFluid(be.slot(0), FluidConstants.BUCKET);
                    fluidLock(be, true);
                    try (Transaction tx = Transaction.openOuter()) {
                        be.slot(0).extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
                        tx.commit();
                    }
                }))));

        cases.add(new Case("Iron Barrel", ModBlocks.BARRELS.get(BarrelTier.IRON), barrelParts()));
        cases.add(new Case("Netherite Barrel", ModBlocks.BARRELS.get(BarrelTier.NETHERITE), barrelParts()));

        // Its cluster is rebuilt from the drawers around it when it loads, so it keeps nothing of its own
        cases.add(new Case("Drawer Controller", ModBlocks.DRAWER_CONTROLLER, List.of()));

        cases.add(new Case("Import/Export Bus", ModBlocks.NETWORK_BUS, List.of(
                part(NetworkBusBlockEntity.class, "filter", be -> be.filterContainer().setItem(0, filter.copy())))));

        cases.add(new Case("Level Emitter", ModBlocks.LEVEL_EMITTER, List.of(
                part(LevelEmitterBlockEntity.class, "item", be -> be.setItem(new ItemStack(Items.DIAMOND))),
                part(LevelEmitterBlockEntity.class, "amount", be -> be.setAmount(42)),
                part(LevelEmitterBlockEntity.class, "at least / fewer than", LevelEmitterBlockEntity::toggleBelow))));

        cases.add(new Case("Fluid Intake", ModBlocks.FLUID_INTAKE, List.of(
                part(FluidIntakeBlockEntity.class, "filter", be -> be.setFilter(FluidVariant.of(Fluids.WATER))),
                part(FluidIntakeBlockEntity.class, "buffered fluid", be -> {
                    h.setBlock(POS.east(), Blocks.WATER);
                    be.pullNow(h.getLevel());
                }))));

        cases.add(new Case("Storage Connector", ModBlocks.STORAGE_CONNECTOR, List.of(
                part(ConnectorBlockEntity.class, "priority", be -> be.setPriority(5)))));
        cases.add(new Case("Filtered Connector", ModBlocks.FILTERED_CONNECTOR, List.of(
                part(ConnectorBlockEntity.class, "priority", be -> be.setPriority(-3)),
                part(ConnectorBlockEntity.class, "filter", be -> be.filterContainer().setItem(0, filter.copy())))));

        cases.add(new Case("Disk Drive", ModBlocks.DISK_DRIVE, List.of(
                part(net.storagenexus.disk.DiskDriveBlockEntity.class, "priority", be -> be.setPriority(-7)),
                part(net.storagenexus.disk.DiskDriveBlockEntity.class, "disk", be -> be.disks().setItem(3,
                        new ItemStack(ModItems.STORAGE_DISKS.get(net.storagenexus.disk.DiskTier.K64)))),
                part(net.storagenexus.disk.DiskDriveBlockEntity.class, "filter", be -> be.filterContainer().setItem(0, filter.copy())))));

        cases.add(new Case("Dimensional Anchor", ModBlocks.DIMENSIONAL_ANCHOR, List.of(
                part(DimensionalAnchorBlockEntity.class, "active", DimensionalAnchorBlockEntity::activate))));

        List<Part> input = new ArrayList<>(linkParts(DimensionalInputBlockEntity.class, links));
        input.add(part(DimensionalInputBlockEntity.class, "buffered items", be -> be.buffer().setItem(0, new ItemStack(Items.DIAMOND, 12))));
        input.add(part(DimensionalInputBlockEntity.class, "speed upgrade", be -> be.upgradeSlot().setItem(0, new ItemStack(ModItems.TRANSFER_SPEED_UPGRADE))));
        input.add(part(DimensionalInputBlockEntity.class, "buffered fluid", be -> {
            try (Transaction tx = Transaction.openOuter()) {
                be.getFluidStorage().insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
                tx.commit();
            }
        }));
        cases.add(new Case("Dimensional Input", ModBlocks.DIMENSIONAL_INPUT, input));

        List<Part> output = new ArrayList<>(linkParts(DimensionalOutputBlockEntity.class, links));
        output.add(part(DimensionalOutputBlockEntity.class, "filter", be -> be.filterSlot().setItem(0, filter.copy())));
        output.add(part(DimensionalOutputBlockEntity.class, "sides", be -> be.toggleSide(Direction.UP)));
        cases.add(new Case("Dimensional Output", ModBlocks.DIMENSIONAL_OUTPUT, output));

        cases.add(new Case("Conduit Bundle", ModBlocks.CONDUIT_BUNDLE, List.of(
                part(ConduitBundleBlockEntity.class, "item conduit", be -> be.addConduit(ConduitType.ITEM, 1)),
                part(ConduitBundleBlockEntity.class, "fluid conduit", be -> be.addConduit(ConduitType.FLUID, 2)),
                part(ConduitBundleBlockEntity.class, "facade", be -> be.setFacade(Blocks.STONE.defaultBlockState())),
                side("side mode", cfg -> cfg.mode = SideConfig.Mode.EXTRACT),
                side("insert channel", cfg -> cfg.channel = 3),
                side("extract channel", cfg -> cfg.extractChannel = 5),
                side("redstone control", cfg -> cfg.redstone = SideConfig.RedstoneControl.WITH_SIGNAL),
                side("priority", cfg -> cfg.priority = 4),
                side("round-robin", cfg -> cfg.roundRobin = true),
                side("filter", cfg -> cfg.filter = filter.copy()),
                side("speed upgrades", cfg -> cfg.upgrades = new ItemStack(ModItems.CONDUIT_SPEED_UPGRADE, 2)))));

        cases.add(new Case("Iron Farm", net.storagenexus.essentials.registry.ModBlocks.IRON_FARM, List.of(
                part(IronFarmBlockEntity.class, "output", be -> be.setItem(0, new ItemStack(Items.IRON_INGOT, 5))),
                part(IronFarmBlockEntity.class, "bread", be -> be.setItem(IronFarmBlockEntity.BREAD_SLOT, new ItemStack(Items.BREAD, 3))),
                part(IronFarmBlockEntity.class, "timers", be -> be.setTimers(new IronFarmTimers(10, 20, 30, 40))))));
        cases.add(new Case("Farm Assembler", net.storagenexus.essentials.registry.ModBlocks.FARM_ASSEMBLER, List.of(
                part(net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity.class, "target", be -> be.select(net.storagenexus.essentials.assembler.AssemblyTarget.SHULKER, null)),
                part(net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity.class, "materials", be -> be.accept(new ItemStack(Items.TORCH, 5))),
                part(net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity.class, "finished farm", be -> be.output().setItem(0,
                        new ItemStack(net.storagenexus.essentials.registry.ModItems.IRON_FARM))))));
        cases.add(new Case("Combustion Generator", ModBlocks.COMBUSTION_GENERATOR, List.of(
                part(net.storagenexus.energy.CombustionGeneratorBlockEntity.class, "fuel", be -> be.setItem(0, new ItemStack(Items.COAL, 7))),
                part(net.storagenexus.energy.CombustionGeneratorBlockEntity.class, "energy", be -> be.energy().set(1234)))));
        cases.add(new Case("Solar Panel", ModBlocks.SOLAR_PANEL, List.of(
                part(net.storagenexus.energy.SolarPanelBlockEntity.class, "energy", be -> be.energy().set(321)))));
        cases.add(new Case("Thermal Generator", ModBlocks.THERMAL_GENERATOR, List.of(
                part(net.storagenexus.energy.ThermalGeneratorBlockEntity.class, "energy", be -> be.energy().set(4321)),
                part(net.storagenexus.energy.ThermalGeneratorBlockEntity.class, "lava", be -> {
                    be.tank().variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA);
                    be.tank().amount = 2 * FluidConstants.BUCKET;
                }))));
        cases.add(new Case("Redstone Reactor", ModBlocks.REDSTONE_REACTOR, List.of(
                part(net.storagenexus.energy.RedstoneReactorBlockEntity.class, "rods", be -> be.setItem(3, new ItemStack(net.storagenexus.registry.ModItems.REDSTONE_FUEL_ROD))),
                part(net.storagenexus.energy.RedstoneReactorBlockEntity.class, "coolant", be -> be.setItem(10, new ItemStack(Items.BLUE_ICE, 9))),
                part(net.storagenexus.energy.RedstoneReactorBlockEntity.class, "depth", be -> be.setDepth(4)),
                part(net.storagenexus.energy.RedstoneReactorBlockEntity.class, "heat", be -> be.setHeat(0.42)),
                part(net.storagenexus.energy.RedstoneReactorBlockEntity.class, "energy", be -> be.energy().set(7777)))));
        cases.add(new Case("Iron Battery", ModBlocks.IRON_BATTERY, List.of(
                part(net.storagenexus.energy.BatteryBlockEntity.class, "energy", be -> be.energy().set(123_456)))));
        cases.add(new Case("Gold Battery", ModBlocks.GOLD_BATTERY, List.of(
                part(net.storagenexus.energy.BatteryBlockEntity.class, "energy", be -> be.energy().set(654_321)))));
        cases.add(new Case("Diamond Battery", ModBlocks.DIAMOND_BATTERY, List.of(
                part(net.storagenexus.energy.BatteryBlockEntity.class, "energy", be -> be.energy().set(9_999_999)))));
        cases.add(new Case("Freezer", ModBlocks.FREEZER, List.of(
                part(net.storagenexus.freezer.FreezerBlockEntity.class, "water", be -> be.setWater(3 * FluidConstants.BUCKET)),
                part(net.storagenexus.freezer.FreezerBlockEntity.class, "ice", be -> be.setItem(0, new ItemStack(Items.ICE, 5))),
                part(net.storagenexus.freezer.FreezerBlockEntity.class, "mode", be -> be.setMode(net.storagenexus.freezer.FreezerMode.BLUE_ICE)))));
        cases.add(new Case("Barter farm", net.storagenexus.essentials.registry.ModBlocks.FARMS.get(net.storagenexus.essentials.farm.FarmKind.PIGLIN_BARTER), List.of(
                part(net.storagenexus.essentials.farm.FarmBlockEntity.class, "output", be -> be.setItem(0, new ItemStack(Items.ENDER_PEARL, 3))),
                part(net.storagenexus.essentials.farm.FarmBlockEntity.class, "input", be -> be.setItem(net.storagenexus.essentials.farm.FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT, 9))),
                part(net.storagenexus.essentials.farm.FarmBlockEntity.class, "progress", be -> be.setProgress(33)))));
        return cases;
    }

    private static List<Part> barrelParts() {
        return List.of(
                part(TieredBarrelBlockEntity.class, "item in the first slot", be -> be.setItem(0, new ItemStack(Items.DIAMOND, 5))),
                part(TieredBarrelBlockEntity.class, "item in the last slot", be -> be.setItem(be.getContainerSize() - 1, new ItemStack(Items.OAK_PLANKS, 64))));
    }

    /** The link and its name: the name is changed on its own by renaming the link after linking. */
    private static <T extends LinkedBlockEntity> List<Part> linkParts(Class<T> type, LinkRegistry links) {
        return List.of(
                part(type, "link", be -> be.setLink(links.create(null, be.globalPos(), be.getLevel().getRandom()))),
                part(type, "link name", be -> be.setLink(links.create(null, be.globalPos(), be.getLevel().getRandom())), be -> {
                    UUID id = be.linkId();
                    links.rename(id, "Renamed " + id.toString().substring(0, 8));
                    be.setLink(id); // re-reads the name
                }));
    }

    /** One setting on the north side of the bundle's item conduit. */
    private static Part side(String what, Consumer<SideConfig> change) {
        return part(ConduitBundleBlockEntity.class, what, be -> {
            change.accept(be.config(ConduitType.ITEM, Direction.NORTH));
            be.changed();
        });
    }

    private static void insert(DrawerBlockEntity.DrawerSlot slot, Item item, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            slot.insert(ItemVariant.of(item), amount, tx);
            tx.commit();
        }
    }

    private static void extract(DrawerBlockEntity.DrawerSlot slot, Item item, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            slot.extract(ItemVariant.of(item), amount, tx);
            tx.commit();
        }
    }

    private static void upgrade(DrawerBlockEntity be, int slot, Item upgrade) {
        be.getUpgrades().set(slot, upgrade == null ? ItemStack.EMPTY : new ItemStack(upgrade));
        be.onUpgradesChanged();
    }

    private static void insertFluid(FluidDrawerBlockEntity.FluidSlot slot, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            slot.insert(FluidVariant.of(Fluids.WATER), amount, tx);
            tx.commit();
        }
    }

    private static void fluidLock(FluidDrawerBlockEntity be, boolean on) {
        be.getUpgrades().set(Upgradeable.LOCK_SLOT, on ? new ItemStack(ModItems.LOCK_UPGRADE) : ItemStack.EMPTY);
        be.onUpgradesChanged();
    }
}
