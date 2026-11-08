package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.disk.DiskContents;
import net.storagenexus.disk.DiskData;
import net.storagenexus.disk.DiskDriveBlockEntity;
import net.storagenexus.disk.DiskTier;
import net.storagenexus.disk.DiskUsage;
import net.storagenexus.disk.StorageDiskItem;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Digital storage: disks in a Disk Drive hold items and fluids for the network, up to their size, of any kinds; drives
 * are filled before chests; contents stay with a disk when it moves and when the world saves; Move to disks empties
 * chests but not drawers; an empty disk comes apart; and the recipes.
 */
public class DiskGameTest {
    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1), DRIVE = new BlockPos(2, 1, 1);

    private static DiskDriveBlockEntity drive(GameTestHelper h, BlockPos at, ItemStack... disks) {
        h.setBlock(at, ModBlocks.DISK_DRIVE);
        DiskDriveBlockEntity be = h.getBlockEntity(at, DiskDriveBlockEntity.class);
        for (int i = 0; i < disks.length; i++) be.disks().setItem(i, disks[i]);
        return be;
    }

    private static ItemStack disk(DiskTier tier) {
        return new ItemStack(ModItems.STORAGE_DISKS.get(tier));
    }

    private static StorageNetwork network(GameTestHelper h) {
        StorageNetwork.clearCache();
        return StorageNetwork.find(h.getLevel(), h.absolutePos(TERMINAL));
    }

    private static long insert(StorageNetwork net, Item item, long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            long n = net.insertItem(ItemVariant.of(item), amount, tx);
            tx.commit();
            return n;
        }
    }

    @GameTest
    public void disksHoldItemsForTheNetwork(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        drive(h, DRIVE, disk(DiskTier.K16));
        StorageNetwork net = network(h);
        h.assertValueEqual(insert(net, Items.STONE, 100), 100L, "stone in");
        h.assertValueEqual(insert(net, Items.DIRT, 50), 50L, "dirt in, any kinds");
        h.assertValueEqual(net.countItem(ItemVariant.of(Items.STONE)), 100L, "the terminal sees the stone");
        StorageNetwork.Usage usage = net.usage();
        h.assertValueEqual(usage.items(), 150L, "150 stored");
        h.assertValueEqual(usage.itemCapacity(), 16_000L, "a 16k disk holds 16,000");
        h.assertValueEqual(insert(net, Items.COBBLESTONE, 20_000), 15_850L, "only up to its size");
        // A transaction that is not committed changes nothing
        try (Transaction tx = Transaction.openOuter()) {
            net.extractItem(ItemVariant.of(Items.STONE), 40, tx);
        }
        h.assertValueEqual(net.countItem(ItemVariant.of(Items.STONE)), 100L, "an aborted take leaves it all");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(net.extractItem(ItemVariant.of(Items.DIRT), 64, tx), 50L, "take all the dirt");
            tx.commit();
        }
        h.assertValueEqual(net.listItems().containsKey(ItemVariant.of(Items.DIRT)), false, "no empty kind is left listed");
        h.succeed();
    }

    @GameTest
    public void disksComeBeforeChests(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(new BlockPos(1, 1, 2), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.CHEST);
        drive(h, DRIVE, disk(DiskTier.K16));
        insert(network(h), Items.STONE, 64);
        ChestBlockEntity chest = h.getBlockEntity(new BlockPos(1, 1, 3), ChestBlockEntity.class);
        h.assertTrue(chest.isEmpty(), "nothing in the chest");
        h.assertValueEqual(h.getBlockEntity(DRIVE, DiskDriveBlockEntity.class).itemStorage().iterator().next().getAmount(), 64L, "all on the disk");
        h.succeed();
    }

    @GameTest
    public void fluidDisksHoldBuckets(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        drive(h, DRIVE, new ItemStack(ModItems.FLUID_DISKS.get(DiskTier.K16)));
        StorageNetwork net = network(h);
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual(net.insertFluid(FluidVariant.of(Fluids.WATER), 100 * FluidConstants.BUCKET, tx), 64 * FluidConstants.BUCKET,
                    "64 buckets fit");
            tx.commit();
        }
        h.assertValueEqual(net.countFluid(FluidVariant.of(Fluids.WATER)), 64 * FluidConstants.BUCKET, "the terminal sees the water");
        h.assertValueEqual(insert(net, Items.STONE, 1), 0L, "a fluid disk takes no items");
        h.succeed();
    }

    @GameTest
    public void contentsStayWithTheDiskAndTheSave(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        DiskDriveBlockEntity first = drive(h, DRIVE, disk(DiskTier.K64));
        insert(network(h), Items.DIAMOND, 500);
        // Taking the disk out writes its usage on it
        first.disks().setChanged();
        ItemStack moved = first.disks().removeItemNoUpdate(0);
        first.disks().setChanged();
        h.assertValueEqual(moved.getOrDefault(ModComponents.DISK_USAGE, DiskUsage.EMPTY), new DiskUsage(500, 1), "the tooltip shows 500 of 1 kind");
        h.assertValueEqual(network(h).countItem(ItemVariant.of(Items.DIAMOND)), 0L, "gone from the network with the disk");
        // Into another drive on another network
        BlockPos terminal2 = new BlockPos(4, 1, 4);
        h.setBlock(terminal2, ModBlocks.CRAFTING_TERMINAL);
        drive(h, new BlockPos(5, 1, 4), moved);
        StorageNetwork.clearCache();
        h.assertValueEqual(StorageNetwork.find(h.getLevel(), h.absolutePos(terminal2)).countItem(ItemVariant.of(Items.DIAMOND)), 500L,
                "the diamonds came with it");
        // Saved and loaded
        DiskData data = DiskData.get(h.getLevel().getServer());
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = DiskData.TYPE.codec().encodeStart(ops, data).getOrThrow();
        DiskData loaded = DiskData.TYPE.codec().parse(ops, saved).getOrThrow();
        UUID id = moved.get(ModComponents.DISK_ID);
        DiskContents<ItemVariant> contents = loaded.items(id);
        h.assertValueEqual(contents.amountOf(ItemVariant.of(Items.DIAMOND)), 500L, "500 diamonds after a save and load");
        h.succeed();
    }

    @GameTest
    public void moveToDisksEmptiesChestsButNotDrawers(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(new BlockPos(1, 1, 2), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.CHEST);
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        ChestBlockEntity chest = h.getBlockEntity(new BlockPos(1, 1, 3), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.STONE, 64));
        chest.setItem(5, new ItemStack(Items.OAK_LOG, 20));
        try (Transaction tx = Transaction.openOuter()) {
            ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(new BlockPos(0, 1, 2)), Direction.EAST)
                    .insert(ItemVariant.of(Items.COBBLESTONE), 100, tx);
            tx.commit();
        }
        DiskDriveBlockEntity be = drive(h, DRIVE, disk(DiskTier.K16));
        StorageNetwork.clearCache();
        DiskDriveBlockEntity.Moved moved = be.moveToDisks();
        h.assertValueEqual(moved.items(), 84L, "stone and logs moved");
        h.assertTrue(chest.isEmpty(), "the chest is empty");
        StorageNetwork net = network(h);
        h.assertValueEqual(net.countItem(ItemVariant.of(Items.COBBLESTONE)), 100L, "the drawer keeps its cobblestone");
        long onDisk = 0;
        for (var view : be.itemStorage()) onDisk += view.getAmount();
        h.assertValueEqual(onDisk, 84L, "84 on the disk");
        h.succeed();
    }

    @GameTest
    public void anEmptyDiskComesApart(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, disk(DiskTier.K256));
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().is(ModItems.DISK_CASING), "the casing is left in hand");
        h.assertTrue(player.getInventory().countItem(ModItems.STORAGE_PARTS.get(DiskTier.K256)) == 1, "and the 256k part is given back");
        // One that holds something stays whole
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        DiskDriveBlockEntity be = drive(h, DRIVE, disk(DiskTier.K16));
        insert(network(h), Items.STONE, 1);
        ItemStack full = be.disks().removeItemNoUpdate(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, full);
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getItem() instanceof StorageDiskItem, "a disk holding stone is not taken apart");
        h.succeed();
    }

    /** Connectors and cables reach out to a drive, whichever is placed first, as a player places them. */
    @GameTest
    public void connectorsAndCablesConnectToTheDrive(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        drive(h, DRIVE);
        BlockPos connector = DRIVE.east(), cable = DRIVE.west();
        for (BlockPos below : List.of(connector.below(), cable.below())) h.setBlock(below, Blocks.STONE);
        place(h, player, ModBlocks.STORAGE_CONNECTOR.asItem(), connector.below());
        place(h, player, ModBlocks.INVENTORY_CABLE.asItem(), cable.below());
        h.assertBlockProperty(connector, net.storagenexus.network.ArmedNetworkBlock.PROPS.get(Direction.WEST), true);
        h.assertBlockProperty(cable, net.storagenexus.network.InventoryCableBlock.PROPS.get(Direction.EAST), true);
        // And a drive placed after them: both turn to it
        BlockPos second = new BlockPos(2, 1, 3);
        h.setBlock(second.east(), ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(second.west(), ModBlocks.INVENTORY_CABLE);
        drive(h, second);
        h.assertBlockProperty(second.east(), net.storagenexus.network.ArmedNetworkBlock.PROPS.get(Direction.WEST), true);
        h.assertBlockProperty(second.west(), net.storagenexus.network.InventoryCableBlock.PROPS.get(Direction.EAST), true);
        h.succeed();
    }

    private static void place(GameTestHelper h, ServerPlayer player, Item item, BlockPos on) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockPos abs = h.absolutePos(on);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void recipes(GameTestHelper h) {
        ItemStack g = new ItemStack(Items.GLASS), r = new ItemStack(Items.REDSTONE), i = new ItemStack(Items.IRON_INGOT),
                c = new ItemStack(Items.COPPER_INGOT), e = ItemStack.EMPTY;
        ItemStack casing = craft(h, List.of(g, r, g, g, e, g, i, i, i));
        h.assertTrue(casing.is(ModItems.DISK_CASING) && casing.getCount() == 2, "glass, redstone and iron make 2 casings");
        ItemStack part = craft(h, List.of(r, c, r, c, new ItemStack(Items.LAPIS_LAZULI), c, r, c, r));
        h.assertTrue(part.is(ModItems.STORAGE_PARTS.get(DiskTier.K16)), "redstone, copper and lapis make a 16k part");
        ItemStack fluidPart = craft(h, List.of(r, c, r, c, new ItemStack(Items.BUCKET), c, r, c, r));
        h.assertTrue(fluidPart.is(ModItems.FLUID_PARTS.get(DiskTier.K16)), "with a bucket, a 16k fluid part");
        ItemStack p64 = craft(h, List.of(part, part, part, new ItemStack(Items.GOLD_INGOT)));
        h.assertTrue(p64.is(ModItems.STORAGE_PARTS.get(DiskTier.K64)), "3 parts and gold make a 64k part");
        ItemStack pN = craft(h, List.of(new ItemStack(ModItems.STORAGE_PARTS.get(DiskTier.M1)), new ItemStack(ModItems.STORAGE_PARTS.get(DiskTier.M1)),
                new ItemStack(ModItems.STORAGE_PARTS.get(DiskTier.M1)), new ItemStack(ModItems.NEXIUM_INGOT)));
        h.assertTrue(pN.is(ModItems.STORAGE_PARTS.get(DiskTier.NEXIUM)), "and a Nexium ingot the Nexium part");
        ItemStack disk = craft(h, List.of(new ItemStack(ModItems.DISK_CASING), p64));
        h.assertTrue(disk.is(ModItems.STORAGE_DISKS.get(DiskTier.K64)), "casing and part make the disk");
        ItemStack drive = craft(h, List.of(i, g, i, r, e, r, i, r, i));
        h.assertTrue(drive.is(ModBlocks.DISK_DRIVE.asItem()), "iron, glass and redstone make the Disk Drive");
        h.succeed();
    }
}
