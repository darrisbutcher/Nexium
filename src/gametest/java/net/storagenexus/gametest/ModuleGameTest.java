package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.ToolMenuMenu;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.aiot.module.VeinKey;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.List;

/** AIOT modules: the tool menu's module slots and switches, the Nexus Link, 3x3 mining and vein mining. */
public class ModuleGameTest {
    private static ItemStack module(ModuleType type) {
        return new ItemStack(ModItems.MODULES.get(type));
    }

    /** A survival player (so mining drops things) holding a diamond AIOT with these modules installed and on. */
    private static ServerPlayer miner(GameTestHelper h, ModuleType... types) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ItemStack aiot = new ItemStack(ModItems.AIOTS.get(AiotTier.DIAMOND));
        var slots = AiotModules.slots(aiot);
        for (int i = 0; i < types.length; i++) slots.set(i, module(types[i]));
        AiotModules.setSlots(aiot, slots);
        player.setItemInHand(InteractionHand.MAIN_HAND, aiot);
        VeinKey.set(player, true); // vein mining needs the key held
        return player;
    }

    /** Puts the player just in front of {@code rel}, looking straight at it from the north. */
    private static void lookAt(GameTestHelper h, ServerPlayer player, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        player.snapTo(abs.getX() + 0.5, abs.getY() - 1.0, abs.getZ() - 1.5, 0.0F, 0.0F); // yaw 0 = looking south (+z)
    }

    private static void mine(GameTestHelper h, ServerPlayer player, BlockPos rel) {
        player.gameMode.destroyBlock(h.absolutePos(rel));
    }

    private static int itemsOnGround(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static int count(ChestBlockEntity chest, Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void moduleSlotsAndSwitches(GameTestHelper h) {
        ServerPlayer player = miner(h);
        ToolMenuMenu menu = new ToolMenuMenu(1, player.getInventory(), player.getInventory().getSelectedSlot());
        h.assertTrue(menu.getSlot(0).mayPlace(module(ModuleType.AREA_MINING)), "a module goes in a module slot");
        menu.getSlot(0).set(module(ModuleType.AREA_MINING));
        h.assertTrue(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "it is installed in the AIOT, and on");
        h.assertFalse(menu.getSlot(1).mayPlace(module(ModuleType.AREA_MINING)), "only one of each module");
        h.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "only modules go in module slots");
        h.assertTrue(menu.getSlot(1).mayPlace(module(ModuleType.VEIN_MINING)), "a different module fits");

        menu.clickMenuButton(player, ToolMenuMenu.BTN_TOGGLE_MODULE);
        h.assertFalse(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "the switch turns it off");
        h.assertTrue(menu.getSlot(0).getItem().has(ModComponents.MODULE_OFF), "and the slot shows it off");
        menu.clickMenuButton(player, ToolMenuMenu.BTN_TOGGLE_MODULE);
        h.assertTrue(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "and on again");

        // Taking it out removes it from the AIOT
        menu.getSlot(0).set(ItemStack.EMPTY);
        h.assertTrue(AiotModules.find(player.getMainHandItem(), ModuleType.AREA_MINING).isEmpty(), "taken out");

        // The tool itself can't be moved while the menu is open
        int lockedSlot = menu.slots.size() - 9 + player.getInventory().getSelectedSlot();
        h.assertTrue(menu.quickMoveStack(player, lockedSlot).isEmpty(), "shift-clicking the tool does nothing");
        h.assertTrue(player.getMainHandItem().is(ModItems.AIOTS.get(AiotTier.DIAMOND)), "the AIOT stays in hand");

        // A Nexium pickaxe has the menu but no module slots
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEXIUM_PICKAXE));
        ToolMenuMenu pickaxeMenu = new ToolMenuMenu(2, player.getInventory(), player.getInventory().getSelectedSlot());
        h.assertFalse(pickaxeMenu.getSlot(0).isActive(), "no module slots on a Nexium pickaxe");
        h.assertFalse(pickaxeMenu.getSlot(0).mayPlace(module(ModuleType.AREA_MINING)), "so modules don't go in");
        h.succeed();
    }

    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1), CONNECTOR = new BlockPos(2, 1, 1), CHEST = new BlockPos(3, 1, 1);
    private static final BlockPos STONE = new BlockPos(1, 2, 4);

    @GameTest(maxTicks = 40)
    public void nexusLinkSendsDropsToTheNetwork(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CHEST, Blocks.CHEST);
        ServerPlayer player = miner(h, ModuleType.NEXUS_LINK);
        GlobalPos terminal = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(TERMINAL));
        h.assertTrue(AiotModules.relink(player.getMainHandItem(), terminal), "the installed Nexus Link takes the link");

        h.setBlock(STONE, Blocks.STONE);
        mine(h, player, STONE);
        h.assertBlockPresent(Blocks.AIR, STONE);
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        h.assertValueEqual(count(chest, Items.COBBLESTONE), 1, "cobblestone sent to the network's chest");
        h.assertValueEqual(itemsOnGround(h), 0, "nothing dropped on the ground");

        // Switched off: drops fall as usual
        AiotModules.toggle(player.getMainHandItem(), 0);
        h.setBlock(STONE, Blocks.STONE);
        mine(h, player, STONE);
        h.assertValueEqual(count(chest, Items.COBBLESTONE), 1, "nothing more in the chest");
        h.assertValueEqual(itemsOnGround(h), 1, "the cobblestone dropped");

        // On again but the terminal is gone: drops fall as usual too
        AiotModules.toggle(player.getMainHandItem(), 0);
        h.setBlock(TERMINAL, Blocks.AIR);
        h.setBlock(STONE, Blocks.STONE);
        mine(h, player, STONE);
        h.assertValueEqual(itemsOnGround(h), 2, "no network, so it drops");
        h.succeed();
    }

    @GameTest
    public void areaMiningMinesTheThreeByThree(GameTestHelper h) {
        // A 3x3 wall of stone at z = 3, centred on (2, 2, 3), with a chest in the corner of the wall
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        h.setBlock(new BlockPos(3, 3, 3), Blocks.CHEST);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.OBSIDIAN);
        ServerPlayer player = miner(h, ModuleType.AREA_MINING);
        BlockPos center = new BlockPos(2, 2, 3);
        lookAt(h, player, center);
        int durability = player.getMainHandItem().getDamageValue();
        mine(h, player, center);
        int stoneLeft = 0;
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) if (h.getBlockState(new BlockPos(x, y, 3)).is(Blocks.STONE)) stoneLeft++;
        h.assertValueEqual(stoneLeft, 0, "all the stone in the 3x3 is mined");
        h.assertBlockPresent(Blocks.CHEST, new BlockPos(3, 3, 3));
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(1, 1, 3));
        h.assertValueEqual(player.getMainHandItem().getDamageValue() - durability, 7, "1 durability per block (7 stone)");

        // Sneaking mines just the one block
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        player.setShiftKeyDown(true);
        mine(h, player, center);
        h.assertBlockPresent(Blocks.AIR, center);
        h.assertBlockPresent(Blocks.STONE, new BlockPos(1, 2, 3));
        h.succeed();
    }

    @GameTest
    public void veinMiningMinesWholeVeinsAndTrees(GameTestHelper h) {
        // An iron vein: a line and a corner-touching block, plus one that doesn't touch
        List<BlockPos> vein = List.of(new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1), new BlockPos(4, 2, 2));
        for (BlockPos p : vein) h.setBlock(p, Blocks.IRON_ORE);
        BlockPos apart = new BlockPos(1, 4, 4);
        h.setBlock(apart, Blocks.IRON_ORE);
        h.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        ServerPlayer player = miner(h, ModuleType.VEIN_MINING, ModuleType.AREA_MINING);
        mine(h, player, vein.getFirst());
        for (BlockPos p : vein) h.assertBlockPresent(Blocks.AIR, p);
        h.assertBlockPresent(Blocks.IRON_ORE, apart);
        h.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 2)); // an ore is vein mined, not 3x3 mined
        h.assertValueEqual(itemsOnGround(h), 4, "four raw iron");

        // A tree trunk comes down in one go
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(5, y, 5), Blocks.OAK_LOG);
        mine(h, player, new BlockPos(5, 1, 5));
        for (int y = 1; y <= 5; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(5, y, 5));

        // Both switched off: one block at a time
        AiotModules.toggle(player.getMainHandItem(), 0);
        AiotModules.toggle(player.getMainHandItem(), 1);
        for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(5, y, 5), Blocks.OAK_LOG);
        player.setShiftKeyDown(false);
        mine(h, player, new BlockPos(5, 1, 5));
        h.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(5, 2, 5));
        h.succeed();
    }

    @GameTest
    public void veinMiningStopsAtTheLimit(GameTestHelper h) {
        // A 5x5x4 block of coal ore (100 blocks): vein mining takes 64
        for (int x = 0; x < 5; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.COAL_ORE);
        ServerPlayer player = miner(h, ModuleType.VEIN_MINING);
        mine(h, player, new BlockPos(2, 1, 2));
        int left = 0;
        for (int x = 0; x < 5; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z < 5; z++) if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.COAL_ORE)) left++;
        h.assertValueEqual(left, 100 - 64, "64 blocks mined, the rest left");
        h.succeed();
    }

    @GameTest
    public void veinMiningNeedsTheKeyHeld(GameTestHelper h) {
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
        ServerPlayer player = miner(h, ModuleType.VEIN_MINING);
        VeinKey.set(player, false);
        mine(h, player, new BlockPos(1, 1, 1));
        h.assertBlockPresent(Blocks.COAL_ORE, new BlockPos(2, 1, 1)); // key up: one block
        VeinKey.set(player, true);
        mine(h, player, new BlockPos(2, 1, 1));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(4, 1, 1)); // key held: the rest of the vein
        // No module, key held: still one block
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
        ServerPlayer plain = miner(h);
        mine(h, plain, new BlockPos(1, 1, 1));
        h.assertBlockPresent(Blocks.COAL_ORE, new BlockPos(2, 1, 1));
        VeinKey.set(player, false);
        VeinKey.set(plain, false);
        h.succeed();
    }

    @GameTest
    public void veinSizeIsSetInTheToolMenu(GameTestHelper h) {
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        try {
            config.aiotVeinMaxBlocks = 64;
            ServerPlayer player = miner(h, ModuleType.VEIN_MINING);
            ToolMenuMenu menu = new ToolMenuMenu(1, player.getInventory(), player.getInventory().getSelectedSlot());
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 64, "defaults to the config's limit");
            menu.clickMenuButton(player, ToolMenuMenu.BTN_VEIN_SIZE);
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 4, "wraps to the smallest after the limit");
            menu.clickMenuButton(player, ToolMenuMenu.BTN_VEIN_SIZE);
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 9, "then steps up");
            h.assertTrue(menu.getSlot(0).getItem().has(ModComponents.VEIN_SIZE), "the slot shows the new size");

            for (int x = 0; x < 20; x++) h.setBlock(new BlockPos(x % 10, 1 + x / 10, 1), Blocks.COAL_ORE);
            mine(h, player, new BlockPos(0, 1, 1));
            int left = 0;
            for (int x = 0; x < 20; x++) if (h.getBlockState(new BlockPos(x % 10, 1 + x / 10, 1)).is(Blocks.COAL_ORE)) left++;
            h.assertValueEqual(left, 20 - 9, "a size of 9 mines 9 blocks");

            // A lower config limit caps the setting
            config.aiotVeinMaxBlocks = 5;
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 5, "the config's limit still caps it");
            h.assertValueEqual(new ToolMenuMenu(2, player.getInventory(), player.getInventory().getSelectedSlot()).veinMax(), 5, "and the menu carries the limit to the screen");
            VeinKey.set(player, false);
        } finally {
            config.aiotVeinMaxBlocks = max;
        }
        h.succeed();
    }

    @GameTest
    public void veinMiningJoinsOreVariants(GameTestHelper h) {
        // Stone and deepslate diamond ore side by side are one vein; an iron ore next to them is not part of it
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(2, 1, 1), c = new BlockPos(3, 1, 1), iron = new BlockPos(2, 2, 1);
        h.setBlock(a, Blocks.DIAMOND_ORE);
        h.setBlock(b, Blocks.DEEPSLATE_DIAMOND_ORE);
        h.setBlock(c, Blocks.DIAMOND_ORE);
        h.setBlock(iron, Blocks.IRON_ORE);
        ServerPlayer player = miner(h, ModuleType.VEIN_MINING);
        mine(h, player, a);
        h.assertBlockPresent(Blocks.AIR, b);
        h.assertBlockPresent(Blocks.AIR, c);
        h.assertBlockPresent(Blocks.IRON_ORE, iron);
        h.succeed();
    }

    @GameTest
    public void configSwitchesTurnTheModulesOff(GameTestHelper h) {
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        boolean vein = config.aiotVeinMining;
        int max = config.aiotVeinMaxBlocks;
        try {
            for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
            ServerPlayer player = miner(h, ModuleType.VEIN_MINING);
            config.aiotVeinMaxBlocks = 3;
            mine(h, player, new BlockPos(1, 1, 1));
            h.assertBlockPresent(Blocks.COAL_ORE, new BlockPos(4, 1, 1)); // 3 of the 4: the limit is a setting
            for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
            config.aiotVeinMaxBlocks = max;
            config.aiotVeinMining = false;
            mine(h, player, new BlockPos(1, 1, 1));
            h.assertBlockPresent(Blocks.COAL_ORE, new BlockPos(2, 1, 1)); // off: just the one block
        } finally {
            config.aiotVeinMining = vein;
            config.aiotVeinMaxBlocks = max;
        }
        h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void nexusLinkTakesAWholeVeinInOneGo(GameTestHelper h) {
        // A terminal at the end of a line of connectors, with a barrel on each side of every connector
        BlockPos terminal = new BlockPos(7, 1, 6);
        h.setBlock(terminal, ModBlocks.CRAFTING_TERMINAL);
        for (int x = 0; x < 7; x++) {
            h.setBlock(new BlockPos(x, 1, 6), ModBlocks.STORAGE_CONNECTOR);
            h.setBlock(new BlockPos(x, 1, 5), Blocks.BARREL);
            h.setBlock(new BlockPos(x, 1, 7), Blocks.BARREL);
        }
        // 100 blocks of coal ore, 64 of which a swing takes
        for (int x = 0; x < 5; x++) for (int y = 2; y <= 5; y++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, y, z), Blocks.COAL_ORE);
        ServerPlayer player = miner(h, ModuleType.VEIN_MINING, ModuleType.NEXUS_LINK);
        h.assertTrue(AiotModules.relink(player.getMainHandItem(), GlobalPos.of(h.getLevel().dimension(), h.absolutePos(terminal))), "linked");
        long start = System.nanoTime();
        mine(h, player, new BlockPos(2, 2, 2));
        long micros = (System.nanoTime() - start) / 1000;
        net.storagenexus.StorageNexus.LOG.info("Vein of 64 with a Nexus Link into 14 barrels: {} microseconds", micros);

        int left = 0;
        for (int x = 0; x < 5; x++) for (int y = 2; y <= 5; y++) for (int z = 0; z < 5; z++) if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.COAL_ORE)) left++;
        h.assertValueEqual(left, 100 - 64, "64 blocks mined");
        long coal = net.storagenexus.network.StorageNetwork.find(h.getLevel(), h.absolutePos(terminal))
                .countItem(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COAL));
        h.assertTrue(coal >= 64, "at least 64 coal in the network, got " + coal);
        h.assertValueEqual(itemsOnGround(h), 0, "nothing on the ground");
        h.succeed();
    }

    @GameTest
    public void nexusLinkModuleLinksToATerminal(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, module(ModuleType.NEXUS_LINK));
        player.setShiftKeyDown(true);
        BlockPos abs = h.absolutePos(TERMINAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        GlobalPos link = player.getMainHandItem().get(ModComponents.TERMINAL_LINK);
        h.assertTrue(link != null && link.pos().equals(abs), "sneak + right-click links the module to the terminal, got " + link);
        h.succeed();
    }
}
