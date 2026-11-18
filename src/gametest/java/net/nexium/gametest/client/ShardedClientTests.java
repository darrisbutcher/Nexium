package net.nexium.gametest.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.List;

/**
 * Runs the client tests, or one share of them. The tests are listed under the {@code nexium-client-test} entrypoint in this
 * mod's fabric.mod.json (the one {@code fabric-client-gametest} entrypoint is this class), so CI can split them over several
 * jobs: with the environment variable {@code NEXIUM_SHARD=i/n} this runs every n-th test starting at i (counting from 0).
 * Without it, all of them run, as before. {@code NEXIUM_ONLY=SomeClientTest} runs just that one test. Every share sees the same list in the same order, so together they cover each test once.
 */
public class ShardedClientTests implements FabricClientGameTest {
    /** Whether the test at {@code position} belongs to share {@code index} of {@code count}. */
    public static boolean selected(int position, int index, int count) {
        return position % count == index;
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        String spec = System.getenv().getOrDefault("NEXIUM_SHARD", "0/1").trim();
        String[] parts = spec.split("/");
        int index = Integer.parseInt(parts[0]), count = Integer.parseInt(parts[1]);
        if (count < 1 || index < 0 || index >= count) throw new IllegalArgumentException("NEXIUM_SHARD must be i/n with 0 <= i < n, got " + spec);
        List<EntrypointContainer<FabricClientGameTest>> tests = FabricLoader.getInstance()
                .getEntrypointContainers("nexium-client-test", FabricClientGameTest.class);
        System.out.println("[nexium-shard] share " + index + " of " + count + ", " + tests.size() + " client tests in all");
        String only = System.getenv("NEXIUM_ONLY");
        int ran = 0;
        for (int i = 0; i < tests.size(); i++) {
            FabricClientGameTest test = tests.get(i).getEntrypoint();
            // NEXIUM_ONLY=SomeClientTest runs just that test (handy while working on one)
            if (only != null ? !test.getClass().getSimpleName().equals(only) : !selected(i, index, count)) continue;
            System.out.println("[nexium-shard] running " + test.getClass().getSimpleName());
            test.runTest(ctx);
            ran++;
        }
        System.out.println("[nexium-shard] ran " + ran + " client tests");
    }
}
