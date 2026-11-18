package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.drawer.DrawerWood;
import net.nexium.essentials.backpack.BackpackCompacting;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.backpack.BackpackRestocking;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.essentials.farm.Overclock;
import net.nexium.essentials.ironfarm.IronFarmBlockEntity;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.essentials.xpfarm.XpFarmBlockEntity;
import net.nexium.network.StorageNetwork;
import net.nexium.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * A stress test: how much of a 50 ms server tick a big base takes. Each scenario ticks its machines directly, many times over,
 * and prints the cost of one server tick of it (see the "[stress]" lines). The budgets are deliberately loose (several times what
 * a normal machine measures) so they only fail on a real regression, not on a slow runner; the numbers are what to look at.
 */
public class StressGameTest {
    private static final double TICK_MS = 50.0;

    private static void report(String what, double msPerTick, double budgetMs) {
        System.out.printf("[stress] %-52s %8.3f ms per server tick (%.1f%% of a 50 ms tick, budget %.1f ms)%n", what, msPerTick, msPerTick / TICK_MS * 100, budgetMs);
        if (msPerTick > budgetMs) throw new AssertionError(what + " costs " + msPerTick + " ms a tick, over its budget of " + budgetMs + " ms");
    }

    /** Warms up, then times {@code ticks} calls of the body; returns milliseconds per call. */
    private static double time(int ticks, Runnable body) {
        for (int i = 0; i < Math.max(20, ticks / 10); i++) body.run();
        long start = System.nanoTime();
        for (int i = 0; i < ticks; i++) body.run();
        return (System.nanoTime() - start) / 1.0e6 / ticks;
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 80, maxTicks = 4000)
    public void sixtyOverclockedFarmsOfEveryKindRunningFlatOut(GameTestHelper h) {
        List<FarmBlockEntity> farms = new ArrayList<>();
        FarmKind[] kinds = FarmKind.values();
        for (int i = 0; i < 60; i++) {
            BlockPos pos = new BlockPos(1 + i % 12 * 2, 1, 1 + i / 12 * 2);
            h.setBlock(pos, net.nexium.essentials.registry.ModBlocks.FARMS.get(kinds[i % kinds.length]));
            FarmBlockEntity farm = h.getBlockEntity(pos, FarmBlockEntity.class);
            farm.setOverclock(Overclock.MAX_LEVEL);
            farms.add(farm);
        }
        int[] tick = {0};
        double ms = time(1500, () -> {
            tick[0]++;
            for (FarmBlockEntity farm : farms) {
                farm.energy().set(farm.energy().capacity());
                // The output is emptied now and then, so they keep making things instead of stopping full
                if (tick[0] % 40 == 0) for (int s = 0; s < FarmBlockEntity.OUTPUT_SLOTS; s++) farm.setItem(s, ItemStack.EMPTY);
                FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
            }
        });
        report("60 farms (every kind) at x8", ms, 10.0);
        long made = 0;
        for (FarmBlockEntity farm : farms) for (int s = 0; s < FarmBlockEntity.OUTPUT_SLOTS; s++) made += farm.getItem(s).getCount();
        if (made == 0) throw new AssertionError("the farms made nothing, so the timing above measured idle machines");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 80, maxTicks = 4000)
    public void fortyIronAndExperienceFarmsAtEightTimes(GameTestHelper h) {
        List<IronFarmBlockEntity> iron = new ArrayList<>();
        List<XpFarmBlockEntity> xp = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            BlockPos a = new BlockPos(1 + i * 2, 1, 1), b = new BlockPos(1 + i * 2, 1, 3);
            h.setBlock(a, net.nexium.essentials.registry.ModBlocks.IRON_FARM);
            h.setBlock(b, net.nexium.essentials.registry.ModBlocks.XP_FARM);
            IronFarmBlockEntity ironFarm = h.getBlockEntity(a, IronFarmBlockEntity.class);
            XpFarmBlockEntity xpFarm = h.getBlockEntity(b, XpFarmBlockEntity.class);
            ironFarm.setOverclock(Overclock.MAX_LEVEL);
            xpFarm.setOverclock(Overclock.MAX_LEVEL);
            iron.add(ironFarm);
            xp.add(xpFarm);
        }
        int[] tick = {0};
        double ms = time(2000, () -> {
            tick[0]++;
            for (IronFarmBlockEntity f : iron) {
                f.energy().set(f.energy().capacity());
                if (tick[0] % 40 == 0) for (int s = 0; s < IronFarmBlockEntity.OUTPUT_SLOTS; s++) f.setItem(s, ItemStack.EMPTY);
                IronFarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
            }
            for (XpFarmBlockEntity f : xp) {
                f.energy().set(f.energy().capacity());
                if (tick[0] % 40 == 0) f.addExperience(-f.experience());
                XpFarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
            }
        });
        report("20 Iron + 20 Experience farms at x8", ms, 4.0);
        if (xp.get(0).experience() <= 0 && iron.get(0).timers().progress() <= 0) throw new AssertionError("the machines did no work");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 80, maxTicks = 4000)
    public void aBigStorageNetworkUnderConstantUse(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 1), ModBlocks.CRAFTING_TERMINAL);
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (item != Items.AIR && item.getDefaultMaxStackSize() > 1 && items.size() < 400) items.add(item);
        int drawers = 120;
        for (int i = 1; i <= drawers; i++) {
            BlockPos c = new BlockPos(i, 1, 1), d = new BlockPos(i, 2, 1);
            h.setBlock(c, ModBlocks.STORAGE_CONNECTOR);
            h.setBlock(d, ModBlocks.DRAWERS.get(DrawerWood.OAK)[2]);
            DrawerBlockEntity drawer = h.getBlockEntity(d, DrawerBlockEntity.class);
            for (int s = 0; s < drawer.slotCount(); s++) drawer.insertFromHand(s, new ItemStack(items.get((i * 4 + s) % items.size()), 32));
        }
        BlockPos terminal = h.absolutePos(new BlockPos(0, 1, 1));
        ItemVariant probe = ItemVariant.of(items.get(5));
        double ms = time(300, () -> {
            StorageNetwork network = StorageNetwork.find(h.getLevel(), terminal);
            if (network == null) throw new AssertionError("no network found");
            network.listItems();
            try (Transaction tx = Transaction.openOuter()) {
                long got = network.extractItem(probe, 8, tx);
                network.insertItem(probe, got, tx);
                tx.commit();
            }
        });
        report("120 connectors + 120 drawers: look up, list, take, put (cached)", ms, 6.0);
        if (StorageNetwork.find(h.getLevel(), terminal).listItems().size() < 100) throw new AssertionError("the network lists too few items");
        // The worst case: something changed in storage since the last lookup, so every lookup scans the whole network again
        double cold = time(200, () -> {
            net.nexium.util.StorageEvents.storageChanged();
            StorageNetwork network = StorageNetwork.find(h.getLevel(), terminal);
            network.listItems();
        });
        report("same network, rescanned every tick (storage changed)", cold, 25.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 80, maxTicks = 4000)
    public void aHundredConduitsWithTwentyMachinesOnThem(GameTestHelper h) {
        List<ConduitBundleBlockEntity> conduits = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            BlockPos pos = new BlockPos(1 + i, 1, 1);
            h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
            ConduitBundleBlockEntity c = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
            c.addConduit(ConduitType.ENERGY, 1);
            c.addConduit(ConduitType.ITEM, 1);
            conduits.add(c);
        }
        h.setBlock(new BlockPos(0, 1, 1), ModBlocks.IRON_BATTERY);
        List<FarmBlockEntity> farms = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            BlockPos pos = new BlockPos(5 + i * 5, 1, 2);
            h.setBlock(pos, net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE));
            FarmBlockEntity farm = h.getBlockEntity(pos, FarmBlockEntity.class);
            farm.setOverclock(Overclock.MAX_LEVEL);
            farms.add(farm);
        }
        var battery = h.getBlockEntity(new BlockPos(0, 1, 1), net.nexium.energy.BatteryBlockEntity.class);
        double ms = time(400, () -> {
            battery.energy().set(battery.energy().capacity());
            for (ConduitBundleBlockEntity c : conduits) ConduitBundleBlockEntity.serverTick(h.getLevel(), c.getBlockPos(), c.getBlockState(), c);
            for (FarmBlockEntity f : farms) FarmBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        });
        report("100 conduits (item + energy), battery, 20 farms at x8", ms, 6.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 80, maxTicks = 4000)
    public void twentyPlayersWithRestockingFeedingAndCompacting(GameTestHelper h) {
        List<ServerPlayer> players = new ArrayList<>();
        for (int p = 0; p < 20; p++) {
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
            ItemStack backpack = new ItemStack(ModItems.BACKPACK);
            List<ItemStack> modules = new ArrayList<>();
            for (BackpackModuleType type : List.of(BackpackModuleType.RESTOCKING, BackpackModuleType.COMPACTING, BackpackModuleType.FEEDING, BackpackModuleType.AUTO_TORCH)) {
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(type)));
            }
            while (modules.size() < BackpackModules.SLOTS) modules.add(ItemStack.EMPTY);
            BackpackModules.setSlots(backpack, modules);
            List<ItemStack> contents = new ArrayList<>();
            for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(new ItemStack(i % 3 == 0 ? Items.IRON_INGOT : i % 3 == 1 ? Items.COBBLESTONE : Items.BREAD, 64));
            BackpackContents.write(backpack, contents);
            ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
            for (int s = 0; s < 9; s++) player.getInventory().setItem(s, new ItemStack(Items.COBBLESTONE, 64));
            players.add(player);
        }
        int[] tick = {0};
        double ms = time(1000, () -> {
            tick[0]++;
            for (ServerPlayer player : players) {
                BackpackRestocking.tick(player);
                // The other modules run once a second, as in the game
                if (tick[0] % 20 == 0) BackpackCompacting.compactNow(player);
            }
        });
        report("20 players: Restocking each tick, Compacting each second", ms, 3.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 120, maxTicks = 6000)
    public void threeHundredFarmsAtEightTimes(GameTestHelper h) {
        List<FarmBlockEntity> farms = new ArrayList<>();
        FarmKind[] kinds = FarmKind.values();
        for (int i = 0; i < 300; i++) {
            BlockPos pos = new BlockPos(1 + i % 25 * 2, 1, 1 + i / 25 * 2);
            h.setBlock(pos, net.nexium.essentials.registry.ModBlocks.FARMS.get(kinds[i % kinds.length]));
            FarmBlockEntity farm = h.getBlockEntity(pos, FarmBlockEntity.class);
            farm.setOverclock(Overclock.MAX_LEVEL);
            farms.add(farm);
        }
        int[] tick = {0};
        double ms = time(500, () -> {
            tick[0]++;
            for (FarmBlockEntity farm : farms) {
                farm.energy().set(farm.energy().capacity());
                if (tick[0] % 40 == 0) for (int s = 0; s < FarmBlockEntity.OUTPUT_SLOTS; s++) farm.setItem(s, ItemStack.EMPTY);
                FarmBlockEntity.serverTick(h.getLevel(), farm.getBlockPos(), farm.getBlockState(), farm);
            }
        });
        report("300 farms (every kind) at x8", ms, 25.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 120, maxTicks = 6000)
    public void aStorageNetworkAtItsSizeLimitRescannedEveryTick(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 1), ModBlocks.CRAFTING_TERMINAL);
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (item != Items.AIR && item.getDefaultMaxStackSize() > 1 && items.size() < 400) items.add(item);
        // A 2D slab: connectors in rows with a drawer on top of every one, about 2,000 network and storage blocks all told
        int count = 0;
        for (int x = 1; x <= 40 && count < 1000; x++) {
            for (int z = 1; z <= 25 && count < 1000; z++, count++) {
                h.setBlock(new BlockPos(x, 1, z), ModBlocks.STORAGE_CONNECTOR);
                h.setBlock(new BlockPos(x, 2, z), ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
                h.getBlockEntity(new BlockPos(x, 2, z), DrawerBlockEntity.class).insertFromHand(0, new ItemStack(items.get(count % items.size()), 16));
            }
        }
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.STORAGE_CONNECTOR); // joins the terminal to the slab
        BlockPos terminal = h.absolutePos(new BlockPos(0, 1, 1));
        net.nexium.network.StorageNetwork first = StorageNetwork.find(h.getLevel(), terminal);
        System.out.println("[stress] the big network has " + first.nodeCount() + " blocks and " + first.inventoryCount() + " inventories");
        double cold = time(60, () -> {
            net.nexium.util.StorageEvents.storageChanged();
            StorageNetwork.find(h.getLevel(), terminal).listItems();
        });
        report("one network at its size limit, rescanned in one tick", cold, 40.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, padding = 120, maxTicks = 6000)
    public void twentyBusesOnOneBigNetworkAllRescanningInTheSameTick(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 1), ModBlocks.CRAFTING_TERMINAL);
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (item != Items.AIR && item.getDefaultMaxStackSize() > 1 && items.size() < 400) items.add(item);
        int count = 0;
        for (int x = 1; x <= 40 && count < 1000; x++) {
            for (int z = 1; z <= 25 && count < 1000; z++, count++) {
                h.setBlock(new BlockPos(x, 1, z), ModBlocks.STORAGE_CONNECTOR);
                h.setBlock(new BlockPos(x, 2, z), ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
                h.getBlockEntity(new BlockPos(x, 2, z), DrawerBlockEntity.class).insertFromHand(0, new ItemStack(items.get(count % items.size()), 16));
            }
        }
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.STORAGE_CONNECTOR);
        // Twenty places in the network that look it up, as buses and terminals do
        List<BlockPos> askers = new ArrayList<>();
        for (int i = 0; i < 20; i++) askers.add(h.absolutePos(new BlockPos(2 + i * 2, 1, 1 + i % 20)));
        double cold = time(40, () -> {
            net.nexium.util.StorageEvents.storageChanged();
            for (BlockPos asker : askers) StorageNetwork.find(h.getLevel(), asker).listItems();
        });
        report("20 askers on one network at its limit, one change per tick", cold, 20.0);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void oneScanServesEveryBlockOfANetworkAndOnlyThatNetwork(GameTestHelper h) {
        // Two networks apart: three connectors in a row, and three more a gap away
        for (int i = 0; i < 3; i++) {
            h.setBlock(new BlockPos(1 + i, 1, 1), ModBlocks.STORAGE_CONNECTOR);
            h.setBlock(new BlockPos(1 + i, 1, 5), ModBlocks.STORAGE_CONNECTOR);
        }
        net.nexium.util.StorageEvents.storageChanged();
        StorageNetwork a1 = StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)));
        StorageNetwork a3 = StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 1)));
        StorageNetwork b1 = StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 5)));
        h.assertTrue(a1 == a3, "two blocks of one network share one scan");
        h.assertTrue(a1 != b1, "another network has a scan of its own");
        h.assertValueEqual(a1.nodeCount(), 3, "each is the three blocks it is");
        // A change in storage makes the next lookup scan again, once, for all of them
        net.nexium.util.StorageEvents.storageChanged();
        StorageNetwork again = StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 1)));
        h.assertTrue(again != a1, "after a change the scan is made afresh");
        h.assertTrue(StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1))) == again, "and the rest of the network share it");
        // Joining the two networks makes one
        h.setBlock(new BlockPos(1, 1, 2), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(1, 1, 3), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(1, 1, 4), ModBlocks.STORAGE_CONNECTOR);
        net.nexium.util.StorageEvents.storageChanged();
        StorageNetwork joined = StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 5)));
        h.assertValueEqual(joined.nodeCount(), 9, "joined, they are nine blocks");
        h.assertTrue(StorageNetwork.find(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 1))) == joined, "and share one scan");
        h.succeed();
    }
}
