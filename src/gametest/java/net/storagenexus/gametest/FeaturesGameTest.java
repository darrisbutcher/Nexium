package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.barrel.TieredBarrelBlockEntity;
import net.storagenexus.dimension.DimensionalInputBlockEntity;
import net.storagenexus.dimension.DimensionalOutputBlockEntity;
import net.storagenexus.dimension.LinkRegistry;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.filter.FilterData;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.fluid.FluidIntakeBlockEntity;
import net.storagenexus.network.LevelEmitterBlock;
import net.storagenexus.network.LevelEmitterBlockEntity;
import net.storagenexus.network.NetworkBusBlock;
import net.storagenexus.network.NetworkBusBlockEntity;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;
import net.storagenexus.terminal.TerminalMenu;

import java.util.List;

/** Import/Export Bus, Level Emitter, barrels, fluid links, the intake filter, the capacity cap and the clear button. */
public class FeaturesGameTest {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1);
    private static final BlockPos BUS = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos CONNECTOR = new BlockPos(4, 1, 1);
    private static final BlockPos STORE = new BlockPos(5, 1, 1);

    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    /** chest - bus (facing the chest) - cable - connector - chest: the left chest is outside the network, the right one in. */
    private static NetworkBusBlockEntity busLayout(GameTestHelper h, boolean export) {
        h.setBlock(SOURCE, Blocks.CHEST);
        h.setBlock(CABLE, ModBlocks.INVENTORY_CABLE);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(STORE, Blocks.CHEST);
        h.setBlock(BUS, ModBlocks.NETWORK_BUS.defaultBlockState()
                .setValue(NetworkBusBlock.FACING, Direction.WEST).setValue(NetworkBusBlock.EXPORT, export));
        return h.getBlockEntity(BUS, NetworkBusBlockEntity.class);
    }

    private static void run(GameTestHelper h, NetworkBusBlockEntity bus) {
        bus.run(h.getLevel(), h.absolutePos(BUS), h.getLevel().getBlockState(h.absolutePos(BUS)));
    }

    @GameTest
    public void busImportsIntoTheNetwork(GameTestHelper h) {
        NetworkBusBlockEntity bus = busLayout(h, false);
        ChestBlockEntity source = h.getBlockEntity(SOURCE, ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.OAK_PLANKS, 40));
        run(h, bus);
        h.assertValueEqual(count(h.getBlockEntity(STORE, ChestBlockEntity.class), Items.OAK_PLANKS), 32, "imported in one pass");
        run(h, bus);
        h.assertValueEqual(count(h.getBlockEntity(STORE, ChestBlockEntity.class), Items.OAK_PLANKS), 40, "imported the rest");
        h.assertValueEqual(count(source, Items.OAK_PLANKS), 0, "source emptied");
        h.succeed();
    }

    @GameTest
    public void busExportsOnlyWhatTheFilterAllows(GameTestHelper h) {
        NetworkBusBlockEntity bus = busLayout(h, true);
        ChestBlockEntity store = h.getBlockEntity(STORE, ChestBlockEntity.class);
        store.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        store.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
        ItemStack filter = new ItemStack(ModItems.BASIC_FILTER);
        filter.set(ModComponents.FILTER, FilterData.empty(false).withEntries(List.of(new ItemStack(Items.IRON_INGOT))));
        bus.filterContainer().setItem(0, filter);
        run(h, bus);
        ChestBlockEntity out = h.getBlockEntity(SOURCE, ChestBlockEntity.class);
        h.assertValueEqual(count(out, Items.IRON_INGOT), 32, "iron exported");
        h.assertValueEqual(count(out, Items.OAK_PLANKS), 0, "planks held back by the filter");
        h.succeed();
    }

    @GameTest
    public void levelEmitterFollowsTheNetworkCount(GameTestHelper h) {
        BlockPos emitterPos = new BlockPos(1, 1, 1);
        h.setBlock(emitterPos, ModBlocks.LEVEL_EMITTER);
        h.setBlock(new BlockPos(2, 1, 1), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.CHEST);
        h.getBlockEntity(new BlockPos(3, 1, 1), ChestBlockEntity.class).setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        LevelEmitterBlockEntity be = h.getBlockEntity(emitterPos, LevelEmitterBlockEntity.class);
        be.setItem(new ItemStack(Items.OAK_PLANKS));
        be.setAmount(5);
        h.assertTrue(h.getBlockState(emitterPos).getValue(LevelEmitterBlock.POWERED), "on with 10 planks and a limit of 5");
        be.setAmount(20);
        h.assertTrue(!h.getBlockState(emitterPos).getValue(LevelEmitterBlock.POWERED), "off with 10 planks and a limit of 20");
        be.toggleBelow();
        h.assertTrue(h.getBlockState(emitterPos).getValue(LevelEmitterBlock.POWERED), "on when set to fewer than 20");
        h.succeed();
    }

    @GameTest
    public void barrelUpgradesKeepContents(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity vanilla = h.getBlockEntity(pos, BarrelBlockEntity.class);
        vanilla.setItem(3, new ItemStack(Items.OAK_PLANKS, 10));
        net.minecraft.server.level.ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        h.assertTrue(!ModItems.BARREL_UPGRADES.get(BarrelTier.GOLD).apply(level, abs), "gold can't skip iron");
        int[] expected = {45, 66, 84, 102, 120};
        for (BarrelTier tier : BarrelTier.values()) {
            h.assertTrue(ModItems.BARREL_UPGRADES.get(tier).apply(level, abs), tier + " upgrade applies");
            TieredBarrelBlockEntity be = h.getBlockEntity(pos, TieredBarrelBlockEntity.class);
            h.assertValueEqual(be.getContainerSize(), expected[tier.ordinal()], tier + " slots");
            h.assertValueEqual(be.getItem(3).getCount(), 10, tier + " kept the planks");
            int slots = tier.slots();
            h.assertValueEqual(net.storagenexus.barrel.BarrelMenu.rows(slots) * net.storagenexus.barrel.BarrelMenu.columns(slots), slots,
                    tier + " fills its grid with no gaps");
        }
        h.assertValueEqual(level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3)).size(), 0, "nothing dropped");
        h.succeed();
    }

    /** A barrel's items survive being saved and loaded again, as when its chunk unloads or the world is reopened. */
    @GameTest
    public void barrelsKeepContentsThroughSaveAndLoad(GameTestHelper h) {
        for (BarrelTier tier : BarrelTier.values()) {
            BlockPos pos = new BlockPos(1 + tier.ordinal(), 1, 1);
            h.setBlock(pos, ModBlocks.BARRELS.get(tier));
            TieredBarrelBlockEntity be = h.getBlockEntity(pos, TieredBarrelBlockEntity.class);
            int last = be.getContainerSize() - 1;
            be.setItem(0, new ItemStack(Items.DIAMOND, 5));
            be.setItem(last, new ItemStack(Items.OAK_PLANKS, 64));

            CompoundTag saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(pos), h.getBlockState(pos), saved, h.getLevel().registryAccess());
            h.assertTrue(loaded instanceof TieredBarrelBlockEntity, tier + " barrel loads back as a tiered barrel");
            TieredBarrelBlockEntity back = (TieredBarrelBlockEntity) loaded;
            h.assertValueEqual(back.getContainerSize(), be.getContainerSize(), tier + " slots after loading");
            h.assertTrue(back.getItem(0).is(Items.DIAMOND) && back.getItem(0).getCount() == 5, tier + " kept the diamonds in its first slot");
            h.assertTrue(back.getItem(last).is(Items.OAK_PLANKS) && back.getItem(last).getCount() == 64, tier + " kept the planks in its last slot");
        }
        h.succeed();
    }

    @GameTest
    public void fluidsTravelThroughLinks(GameTestHelper h) {
        BlockPos in = new BlockPos(1, 1, 1), out = new BlockPos(3, 2, 3), drawer = out.east();
        h.setBlock(in, ModBlocks.DIMENSIONAL_INPUT);
        h.setBlock(out, ModBlocks.DIMENSIONAL_OUTPUT);
        h.setBlock(drawer, ModBlocks.FLUID_DRAWER_1X1);
        DimensionalInputBlockEntity input = h.getBlockEntity(in, DimensionalInputBlockEntity.class);
        DimensionalOutputBlockEntity output = h.getBlockEntity(out, DimensionalOutputBlockEntity.class);
        LinkRegistry reg = LinkRegistry.get(h.getLevel().getServer());
        java.util.UUID id = reg.create(null, input.globalPos(), h.getLevel().getRandom());
        reg.addOutput(id, output.globalPos());
        input.setLink(id);
        output.setLink(id);
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(input.getFluidStorage().insert(FluidVariant.of(Fluids.WATER), 3 * FluidConstants.BUCKET, tx),
                    3 * FluidConstants.BUCKET, "fluid accepted by the Input");
            tx.commit();
        }
        DimensionalInputBlockEntity.serverTick(h.getLevel(), h.absolutePos(in), h.getLevel().getBlockState(h.absolutePos(in)), input);
        FluidDrawerBlockEntity d = h.getBlockEntity(drawer, FluidDrawerBlockEntity.class);
        h.assertValueEqual(d.slot(0).getAmount(), FluidConstants.BUCKET, "a bucket arrived in the drawer next to the Output");
        h.assertTrue(input.fluidAmount() == 2 * FluidConstants.BUCKET, "the rest is still waiting in the Input");
        h.succeed();
    }

    @GameTest
    public void intakeFilterAndLavaSource(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.FLUID_INTAKE);
        h.setBlock(pos.east(), Blocks.WATER);
        h.setBlock(pos.west(), ModBlocks.LAVA_SPRING);
        FluidIntakeBlockEntity be = h.getBlockEntity(pos, FluidIntakeBlockEntity.class);
        be.setFilter(FluidVariant.of(Fluids.LAVA));
        be.pullNow(h.getLevel());
        h.assertTrue(be.bufferAmount() == FluidConstants.BUCKET, "one bucket taken");
        h.assertTrue(h.getBlockState(pos.west()).is(ModBlocks.LAVA_SPRING), "the lava source is never used up");
        h.assertTrue(h.getBlockState(pos.east()).is(Blocks.WATER), "water left alone by a lava filter");
        be.pullNow(h.getLevel());
        be.pullNow(h.getLevel());
        be.pullNow(h.getLevel());
        be.pullNow(h.getLevel());
        h.assertTrue(be.bufferAmount() == 4 * FluidConstants.BUCKET, "lava keeps coming");
        h.succeed();
    }

    @GameTest
    public void capacityUpgradesAddTogether(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        be.tryInstall(new ItemStack(ModItems.NETHERITE_UPGRADE));
        h.assertValueEqual(be.capacityMultiplier(), 32, "one netherite upgrade");
        be.tryInstall(new ItemStack(ModItems.EMERALD_UPGRADE));
        h.assertValueEqual(be.capacityMultiplier(), 48, "netherite + emerald");
        be.tryInstall(new ItemStack(ModItems.NETHERITE_UPGRADE));
        be.tryInstall(new ItemStack(ModItems.NETHERITE_UPGRADE));
        h.assertValueEqual(be.capacityMultiplier(), 112, "three netherite + emerald, no cap");
        h.assertValueEqual(be.slot(0).capacity(), 54L * 64 * 112, "a 1x1 drawer then holds 112 x 54 stacks");
        h.succeed();
    }

    @GameTest
    public void strongerCapacityUpgradeSwapsIntoAFullDrawer(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        be.tryInstall(new ItemStack(ModItems.GOLD_UPGRADE));
        for (int i = 0; i < 3; i++) be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE));
        h.assertValueEqual(be.capacityMultiplier(), 10, "gold + three iron");
        // Fill it to the brim so no upgrade could normally come out
        be.insertFromHand(0, new ItemStack(Items.COBBLESTONE, 64));
        while (be.slot(0).getAmount() < be.slot(0).capacity()) be.insertFromHand(0, new ItemStack(Items.COBBLESTONE, 64));
        h.assertFalse(be.canRemoveUpgrade(0), "a full drawer keeps its upgrades");

        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos.above())));
        ItemStack held = new ItemStack(ModItems.NETHERITE_UPGRADE, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)).add(0, 0.5, 0), Direction.UP, h.absolutePos(pos), false);
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(held, h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertValueEqual(be.capacityMultiplier(), 40, "an iron upgrade became netherite: 4 + 2 + 2 + 32");
        h.assertValueEqual(held.getCount(), 1, "one netherite upgrade used");
        AABB around = new AABB(h.absolutePos(pos)).inflate(2);
        h.assertValueEqual(h.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(e -> e.getItem().is(ModItems.IRON_UPGRADE)).count(), 1L, "the iron upgrade popped out");

        // Next one replaces the next weakest (another iron), never a stronger one
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(held, h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertValueEqual(be.capacityMultiplier(), 70, "gold + iron + two netherite");
        ItemStack weaker = new ItemStack(ModItems.IRON_UPGRADE);
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(weaker, h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        h.assertValueEqual(weaker.getCount(), 1, "an iron upgrade can't replace anything stronger or equal");
        h.succeed();
    }

    @GameTest
    public void upgradeScreenSwapsInHigherCapacityAndShowsTotal(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWERS.get(DrawerWood.OAK)[1]);
        DrawerBlockEntity be = h.getBlockEntity(pos, DrawerBlockEntity.class);
        for (int i = 0; i < 4; i++) be.tryInstall(new ItemStack(ModItems.IRON_UPGRADE));
        while (be.slot(0).getAmount() < be.slot(0).capacity()) be.insertFromHand(0, new ItemStack(Items.COBBLESTONE, 64));
        h.assertFalse(be.canRemoveUpgrade(0), "the drawer is full");

        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos.above())));
        net.storagenexus.upgrade.UpgradeMenu menu = new net.storagenexus.upgrade.UpgradeMenu(0, player.getInventory(), be, be);
        h.assertValueEqual(menu.multiplier(), 8, "four iron");
        h.assertValueEqual(menu.totalCapacity(), 54L * 8, "54 stacks x8");
        h.assertValueEqual(menu.capacitySlots(), 2, "a 1x2 drawer");

        // Click a slot carrying a whole stack of netherite upgrades
        menu.setCarried(new ItemStack(ModItems.NETHERITE_UPGRADE, 5));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertValueEqual(be.capacityMultiplier(), 38, "an iron became netherite: 32 + 3 x 2");
        h.assertValueEqual(menu.getCarried().getCount(), 4, "one netherite used");
        h.assertValueEqual(player.getInventory().countItem(ModItems.IRON_UPGRADE), 1, "the iron went to the inventory");
        h.assertValueEqual(menu.totalCapacity(), 54L * 38, "the screen total follows");

        // Shift-click one from the inventory: replaces the next iron
        player.getInventory().setItem(9, new ItemStack(ModItems.EMERALD_UPGRADE, 2));
        menu.setCarried(ItemStack.EMPTY);
        int invSlot = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            var s = menu.slots.get(i);
            if (s.container == player.getInventory() && s.getContainerSlot() == 9) invSlot = i;
        }
        menu.clicked(invSlot, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
        h.assertValueEqual(be.capacityMultiplier(), 52, "netherite + emerald + two iron");
        h.assertValueEqual(player.getInventory().countItem(ModItems.EMERALD_UPGRADE), 1, "one emerald used per shift-click");
        h.assertValueEqual(player.getInventory().countItem(ModItems.IRON_UPGRADE), 2, "second iron in the inventory");

        // An equal or weaker one doesn't swap
        menu.setCarried(new ItemStack(ModItems.IRON_UPGRADE, 3));
        menu.clicked(2, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertValueEqual(be.capacityMultiplier(), 52, "iron over iron changes nothing");
        h.succeed();
    }

    @GameTest
    public void clearButtonEmptiesTheGrid(GameTestHelper h) {
        BlockPos terminal = new BlockPos(1, 1, 1);
        h.setBlock(terminal, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(new BlockPos(2, 1, 1), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(3, 1, 1), Blocks.CHEST);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(terminal.above())));
        TerminalMenu menu = new TerminalMenu(1, player.getInventory(), true, h.getLevel(), h.absolutePos(terminal), p -> true, null);
        player.containerMenu = menu;
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS, 5));
        menu.getInputGridSlots().get(4).set(new ItemStack(Items.IRON_INGOT, 3));
        menu.clickMenuButton(player, TerminalMenu.CLEAR_GRID);
        ChestBlockEntity chest = h.getBlockEntity(new BlockPos(3, 1, 1), ChestBlockEntity.class);
        h.assertValueEqual(count(chest, Items.OAK_PLANKS), 5, "planks went to the network");
        h.assertValueEqual(count(chest, Items.IRON_INGOT), 3, "iron went to the network");
        h.assertTrue(menu.getInputGridSlots().get(0).getItem().isEmpty(), "grid emptied");

        // Network full of other things: it goes to the inventory, and with that full, to the floor
        for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.DIRT, 64));
        menu.getInputGridSlots().get(1).set(new ItemStack(Items.GOLD_INGOT, 7));
        menu.clickMenuButton(player, TerminalMenu.CLEAR_GRID);
        h.assertValueEqual(player.getInventory().countItem(Items.GOLD_INGOT), 7, "gold went to the inventory");
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu.getInputGridSlots().get(2).set(new ItemStack(Items.EMERALD, 2));
        menu.clickMenuButton(player, TerminalMenu.CLEAR_GRID);
        int onFloor = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(40))) {
            if (e.getItem().is(Items.EMERALD)) onFloor += e.getItem().getCount();
        }
        h.assertValueEqual(onFloor, 2, "emeralds dropped on the ground");
        h.succeed();
    }

    /** A left click takes a whole stack of a mod item, whatever storage it sits in. */
    @GameTest
    public void takeStackOfUpgradesFromAnyStorage(GameTestHelper h) {
        ItemStack upgrades = new ItemStack(ModItems.BARREL_UPGRADES.get(BarrelTier.GOLD), 64);
        net.minecraft.world.level.block.Block[] stores = {Blocks.CHEST, ModBlocks.DRAWERS.get(DrawerWood.OAK)[0], ModBlocks.BARRELS.get(BarrelTier.IRON)};
        for (int i = 0; i < stores.length; i++) {
            BlockPos terminal = new BlockPos(1, 1 + i * 2, 1), store = terminal.east();
            h.setBlock(terminal, ModBlocks.CRAFTING_TERMINAL);
            h.setBlock(store, stores[i]);
            net.fabricmc.fabric.api.transfer.v1.item.ItemVariant v = net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(upgrades);
            h.setBlock(terminal.west(), ModBlocks.STORAGE_CONNECTOR);
            h.setBlock(terminal.west(), net.minecraft.world.level.block.Blocks.AIR);
            // the connector sits between the terminal and the store
            h.setBlock(terminal.east(), ModBlocks.STORAGE_CONNECTOR);
            h.setBlock(terminal.east(2), stores[i]);
            var storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(terminal.east(2)), Direction.WEST);
            try (Transaction tx = Transaction.openOuter()) {
                h.assertValueEqual(storage.insert(v, 64, tx), 64L, stores[i] + " accepts 64 upgrades");
                tx.commit();
            }
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.getInventory().clearContent();
            TerminalMenu menu = new TerminalMenu(1, player.getInventory(), true, h.getLevel(), h.absolutePos(terminal), p -> true, null);
            player.containerMenu = menu;
            menu.handleAction(player, new net.storagenexus.terminal.TerminalActionPayload(
                    net.storagenexus.terminal.TerminalActionPayload.TAKE_STACK, upgrades.copyWithCount(1), Fluids.EMPTY));
            h.assertValueEqual(menu.getCarried().getCount(), 64, "stack taken from " + stores[i]);
        }
        h.succeed();
    }

    @GameTest
    public void levelEmitterCountsFluidInBuckets(GameTestHelper h) {
        BlockPos emitterPos = new BlockPos(1, 1, 1), drawer = new BlockPos(3, 1, 1);
        h.setBlock(emitterPos, ModBlocks.LEVEL_EMITTER);
        h.setBlock(new BlockPos(2, 1, 1), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(drawer, ModBlocks.FLUID_DRAWER_1X1);
        try (Transaction tx = Transaction.openOuter()) {
            h.getBlockEntity(drawer, FluidDrawerBlockEntity.class).getFluidStorage()
                    .insert(FluidVariant.of(Fluids.LAVA), 3 * FluidConstants.BUCKET, tx);
            tx.commit();
        }
        LevelEmitterBlockEntity be = h.getBlockEntity(emitterPos, LevelEmitterBlockEntity.class);
        be.setItem(new ItemStack(Items.LAVA_BUCKET));
        be.setAmount(3);
        h.assertValueEqual(be.lastCount(), 3L, "three buckets of lava in a fluid drawer");
        h.assertTrue(h.getBlockState(emitterPos).getValue(LevelEmitterBlock.POWERED), "on at 3 of 3");
        be.setAmount(4);
        h.assertTrue(!h.getBlockState(emitterPos).getValue(LevelEmitterBlock.POWERED), "off at 3 of 4");
        h.succeed();
    }

    @GameTest
    public void usageCountsCompactingPoolsOnce(GameTestHelper h) {
        BlockPos terminal = new BlockPos(1, 1, 1), connector = new BlockPos(2, 1, 1);
        h.setBlock(terminal, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(connector, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(connector.east(), ModBlocks.COMPACTING_DRAWER);
        h.setBlock(connector.above(), Blocks.CHEST);
        try (Transaction tx = Transaction.openOuter()) {
            h.getBlockEntity(connector.east(), net.storagenexus.drawer.CompactingDrawerBlockEntity.class).getItemStorage()
                    .insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.IRON_INGOT), 10, tx);
            tx.commit();
        }
        h.getBlockEntity(connector.above(), ChestBlockEntity.class).setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        net.storagenexus.network.StorageNetwork.Usage u =
                net.storagenexus.network.StorageNetwork.find(h.getLevel(), h.absolutePos(terminal)).usage();
        // 10 ingots = 90 nuggets in the compacting pool, counted once in nuggets; plus 5 planks
        h.assertValueEqual(u.items(), 95L, "items stored");
        long compacting = 54L * 64 * 81; // 54 stacks of iron blocks, in nuggets
        h.assertValueEqual(u.itemCapacity(), compacting + 27L * 64, "capacity: the compacting pool once plus a chest");
        h.succeed();
    }
}
