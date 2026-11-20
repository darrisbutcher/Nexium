package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.EnergyNetwork;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.network.StorageNetwork;
import net.nexium.registry.ModBlocks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The 1.24.3 maintenance fixes: the energy network no longer sits in a map that is never emptied, a storage network scan never loads a
 * chunk, and a settings file that can't be read is kept instead of being overwritten. (Undo's permission check is in BuilderGameTest and
 * the anchors' forced chunks in AnchorGameTest.)
 */
public class MaintenanceGameTest {
    @GameTest(environment = EssentialsEnvironment.ID)
    public void theEnergyNetworkLivesOnItsConduitsNotInAStaticMap(GameTestHelper h) throws ReflectiveOperationException {
        // A static map from conduits to networks whose values hold those conduits is never emptied: it kept every conduit, and the
        // whole level with them, alive after a chunk unloaded or the world was left
        for (Field f : EnergyNetwork.class.getDeclaredFields()) {
            h.assertTrue(!(Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())), "EnergyNetwork keeps no static map: " + f.getName());
        }
        BlockPos a = new BlockPos(1, 2, 1), b = a.east();
        h.setBlock(a, ModBlocks.CONDUIT_BUNDLE);
        h.setBlock(b, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity one = h.getBlockEntity(a, ConduitBundleBlockEntity.class);
        ConduitBundleBlockEntity two = h.getBlockEntity(b, ConduitBundleBlockEntity.class);
        one.addConduit(ConduitType.ENERGY, 1);
        two.addConduit(ConduitType.ENERGY, 1);
        Field held = ConduitBundleBlockEntity.class.getDeclaredField("energyNetwork");
        held.setAccessible(true);

        EnergyNetwork network = EnergyNetwork.of(one);
        h.assertValueEqual(network.size(), 2, "the two conduits are one network");
        h.assertTrue(EnergyNetwork.of(one) == network && EnergyNetwork.of(two) == network, "found again without a rescan");
        h.assertTrue(held.get(one) == network && held.get(two) == network, "each conduit holds its network");

        // One is broken: it lets go of the network, and the other finds a new one without it
        h.setBlock(b, Blocks.AIR);
        h.assertTrue(held.get(two) == null, "the removed conduit let go of its network");
        EnergyNetwork after = EnergyNetwork.of(one);
        h.assertTrue(after != network && after.size() == 1 && held.get(one) == after, "the one left finds a network of its own");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aStorageNetworkScanStopsAtTheEdgeOfWhatIsLoaded(GameTestHelper h) {
        // A line of cables with a connector at x 2 and a chest beside it at x 3 (the chest is the inventory the network would reach)
        for (int x = 1; x <= 5; x++) h.setBlock(new BlockPos(x, 2, 1), ModBlocks.INVENTORY_CABLE);
        h.setBlock(new BlockPos(2, 2, 1), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST);
        ServerLevel level = h.getLevel();
        BlockPos start = h.absolutePos(new BlockPos(1, 2, 1));

        StorageNetwork everything = StorageNetwork.scan(level, start, pos -> true);
        h.assertValueEqual(everything.nodeCount(), 5, "with everything loaded, the whole line is one network");
        h.assertValueEqual(everything.inventoryCount(), 1, "and it reaches the chest");

        // Say the chunk beyond x 3 is not loaded: the scan must not look there, so the network ends at x 3
        int limit = h.absolutePos(new BlockPos(4, 2, 1)).getX();
        StorageNetwork short_ = StorageNetwork.scan(level, start, pos -> pos.getX() < limit);
        h.assertValueEqual(short_.nodeCount(), 3, "the network ends where the loaded chunks do");

        // And if the chest's own chunk is the one that is not loaded, it is left out
        int chestZ = h.absolutePos(new BlockPos(2, 2, 2)).getZ();
        StorageNetwork noChest = StorageNetwork.scan(level, start, pos -> pos.getZ() < chestZ);
        h.assertValueEqual(noChest.nodeCount(), 5, "the cables are all still there");
        h.assertValueEqual(noChest.inventoryCount(), 0, "but an inventory in an unloaded chunk is not reached");
        h.succeed();
    }

    @GameTest
    public void anUnreadableSettingsFileIsKeptNotOverwritten(GameTestHelper h) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("nexium-config-test");
            Path file = dir.resolve("nexium.json");
            String typed = "{\"mobPickUp\": false, \"magnetMaxRange\": ";   // cut off in the middle, as a slip of the keyboard might
            Files.writeString(file, typed);

            NexusConfig config = NexusConfig.loadFrom(file);
            h.assertTrue(config.mobPickUp && config.magnetMaxRange == 16, "the defaults are used for now");
            List<Path> kept = brokenCopies(dir);
            h.assertTrue(kept.size() == 1 && Files.readString(kept.getFirst()).equals(typed), "what was in the file is kept, exactly: " + kept);
            h.assertTrue(Files.exists(file), "and a fresh, readable file is written in its place");

            // The fresh file reads cleanly: nothing more is set aside
            NexusConfig again = NexusConfig.loadFrom(file);
            h.assertTrue(again.mobPickUp && brokenCopies(dir).size() == 1, "a readable file is not set aside");

            // A file the player edited properly is read as it is
            Files.writeString(file, "{\"mobPickUp\": false, \"magnetMaxRange\": 20}");
            NexusConfig edited = NexusConfig.loadFrom(file);
            h.assertTrue(!edited.mobPickUp && edited.magnetMaxRange == 20 && brokenCopies(dir).size() == 1, "an edited file keeps its settings");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteAll(dir);
        }
        h.succeed();
    }

    private static List<Path> brokenCopies(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("nexium.json.broken-")).toList();
        }
    }

    private static void deleteAll(Path dir) {
        if (dir == null) return;
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // A temp directory left behind is harmless
        }
    }
}
