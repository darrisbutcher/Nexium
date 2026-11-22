package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.network.FilteredConnectorBlock;
import net.nexium.network.StorageNetwork;
import net.nexium.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage connectors add inventories as storage, so they leave machines alone (a furnace shows a different slot behind each face, and a
 * connector saw only one): the vanilla machines and Nexium's own, from a setting that starts with them and can be edited.
 */
public class ConnectorMachinesGameTest {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2), CONNECTOR = new BlockPos(2, 3, 2);

    private static int inventories(GameTestHelper h) {
        return StorageNetwork.scan(h.getLevel(), h.absolutePos(CONNECTOR), pos -> true).inventoryCount();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aConnectorStillJoinsAChest(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.CHEST);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.assertValueEqual(inventories(h), 1, "a chest beside a connector is storage in the network");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aConnectorSkipsVanillaMachines(GameTestHelper h) {
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        for (Block machine : List.of(Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.BREWING_STAND, Blocks.HOPPER,
                Blocks.DISPENSER, Blocks.DROPPER, Blocks.CRAFTER)) {
            h.setBlock(MACHINE, machine);
            h.assertValueEqual(inventories(h), 0, BuiltInRegistries.BLOCK.getKey(machine) + " is not joined to the network");
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aConnectorSkipsNexiumsMachines(GameTestHelper h) {
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        List<Block> machines = new ArrayList<>(List.of(ModBlocks.NEXUS_FURNACE, ModBlocks.COMBUSTION_GENERATOR, ModBlocks.FREEZER,
                ModBlocks.REDSTONE_REACTOR, net.nexium.essentials.registry.ModBlocks.IRON_FARM, net.nexium.essentials.registry.ModBlocks.XP_FARM,
                net.nexium.essentials.registry.ModBlocks.FARM_ASSEMBLER, net.nexium.essentials.registry.ModBlocks.TRASH_CAN));
        machines.addAll(net.nexium.essentials.registry.ModBlocks.FARMS.values());
        for (Block machine : machines) {
            h.setBlock(MACHINE, machine);
            h.assertValueEqual(inventories(h), 0, BuiltInRegistries.BLOCK.getKey(machine) + " is not joined to the network");
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aFilteredConnectorSkipsMachinesToo(GameTestHelper h) {
        h.setBlock(MACHINE, Blocks.FURNACE);
        h.setBlock(CONNECTOR, ModBlocks.FILTERED_CONNECTOR);
        BlockState facingDown = h.getBlockState(CONNECTOR).setValue(FilteredConnectorBlock.FACING, Direction.DOWN);
        h.getLevel().setBlock(h.absolutePos(CONNECTOR), facingDown, 3);
        h.assertValueEqual(inventories(h), 0, "a Filtered Connector facing a furnace does not join it");
        h.setBlock(MACHINE, Blocks.CHEST);
        h.assertValueEqual(inventories(h), 1, "but joins a chest it faces");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void takingAnEntryOffTheListConnectsAgain(GameTestHelper h) {
        NexusConfig config = NexusConfig.get();
        List<String> saved = new ArrayList<>(config.connectorBlacklist);
        try {
            h.setBlock(MACHINE, Blocks.FURNACE);
            h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
            h.assertValueEqual(inventories(h), 0, "refused by default");
            config.connectorBlacklist.remove("minecraft:furnace");
            h.assertValueEqual(inventories(h), 1, "joined once it is off the list");
            config.connectorBlacklist.clear();
            h.setBlock(MACHINE, ModBlocks.NEXUS_FURNACE);
            h.assertValueEqual(inventories(h), 1, "with an empty list nothing is refused");
            config.connectorBlacklist.add("#nexium:machines");
            h.assertValueEqual(inventories(h), 0, "and a tag on the list refuses every block in it");
        } finally {
            config.connectorBlacklist.clear();
            config.connectorBlacklist.addAll(saved);
        }
        h.succeed();
    }

    @GameTest
    public void everyNexiumMachineIsInTheMachinesTag(GameTestHelper h) {
        TagKey<Block> machines = TagKey.create(Registries.BLOCK, Identifier.parse("nexium:machines"));
        List<String> missing = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("nexium")) continue;
            boolean machine = block instanceof net.nexium.essentials.farm.FarmBlock || block instanceof net.nexium.essentials.ironfarm.IronFarmBlock
                    || block instanceof net.nexium.essentials.xpfarm.XpFarmBlock || block instanceof net.nexium.essentials.assembler.FarmAssemblerBlock
                    || block instanceof net.nexium.energy.NexusFurnaceBlock || block instanceof net.nexium.energy.CombustionGeneratorBlock
                    || block instanceof net.nexium.energy.ThermalGeneratorBlock || block instanceof net.nexium.energy.SolarPanelBlock
                    || block instanceof net.nexium.energy.RedstoneReactorBlock || block instanceof net.nexium.energy.BatteryBlock
                    || block instanceof net.nexium.freezer.FreezerBlock || block instanceof net.nexium.energy.PowerMeterBlock
                    || block instanceof net.nexium.fluid.LavaSpringBlock;
            if (machine && !block.defaultBlockState().is(machines)) missing.add(BuiltInRegistries.BLOCK.getKey(block).toString());
        }
        h.assertTrue(missing.isEmpty(), "every machine is in #nexium:machines, missing: " + missing);
        h.succeed();
    }

    @GameTest
    public void theConnectorListStartsWithTheMachines(GameTestHelper h) {
        List<String> defaults = new NexusConfig().connectorBlacklist;
        h.assertTrue(defaults.containsAll(List.of("minecraft:furnace", "minecraft:hopper", "minecraft:crafter", "#nexium:machines")),
                "a new config starts with the vanilla machines and #nexium:machines: " + defaults);
        h.succeed();
    }
}
