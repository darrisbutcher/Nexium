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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.aiot.ToolMenuMenu;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleItem;
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
        h.assertTrue(menu.getSlot(1).mayPlace(module(ModuleType.NEXUS_LINK)), "a different module fits");

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
        ServerPlayer player = miner(h, ModuleType.AREA_MINING);
        mine(h, player, vein.getFirst());
        for (BlockPos p : vein) h.assertBlockPresent(Blocks.AIR, p);
        h.assertBlockPresent(Blocks.IRON_ORE, apart);
        h.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 2)); // an ore is vein mined, not 3x3 mined
        h.assertValueEqual(itemsOnGround(h), 4, "four raw iron");

        // A tree trunk comes down in one go
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(5, y, 5), Blocks.OAK_LOG);
        mine(h, player, new BlockPos(5, 1, 5));
        for (int y = 1; y <= 5; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(5, y, 5));

        // The key up and the 3x3 module switched off: one block at a time
        AiotModules.toggle(player.getMainHandItem(), 0);
        VeinKey.set(player, false);
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
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        int left = 0;
        try {
            config.aiotVeinMaxBlocks = 64;
            ServerPlayer player = miner(h);
            mine(h, player, new BlockPos(2, 1, 2));
            for (int x = 0; x < 5; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z < 5; z++) if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.COAL_ORE)) left++;
        } finally {
            config.aiotVeinMaxBlocks = max;
        }
        h.assertValueEqual(left, 100 - 64, "64 blocks mined, the rest left");
        h.succeed();
    }

    @GameTest
    public void veinMiningNeedsTheKeyHeld(GameTestHelper h) {
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
        // No module in the AIOT: the key is all it takes
        ServerPlayer player = miner(h);
        VeinKey.set(player, false);
        mine(h, player, new BlockPos(1, 1, 1));
        h.assertBlockPresent(Blocks.COAL_ORE, new BlockPos(2, 1, 1)); // key up: one block
        VeinKey.set(player, true);
        mine(h, player, new BlockPos(2, 1, 1));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(4, 1, 1)); // key held: the rest of the vein
        VeinKey.set(player, false);
        h.succeed();
    }

    @GameTest
    public void veinSizeIsSetInTheToolMenu(GameTestHelper h) {
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        try {
            config.aiotVeinMaxBlocks = 64;
            ServerPlayer player = miner(h);
            ToolMenuMenu menu = new ToolMenuMenu(1, player.getInventory(), player.getInventory().getSelectedSlot());
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 64, "defaults to the config's limit");
            menu.clickMenuButton(player, ToolMenuMenu.BTN_VEIN_SIZE);
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 4, "wraps to the smallest after the limit");
            menu.clickMenuButton(player, ToolMenuMenu.BTN_VEIN_SIZE);
            h.assertValueEqual(AiotModules.veinSize(player.getMainHandItem()), 9, "then steps up");
            h.assertTrue(player.getMainHandItem().has(ModComponents.VEIN_SIZE), "the AIOT itself keeps the new size, with no module");

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
        ServerPlayer player = miner(h);
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
            ServerPlayer player = miner(h);
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
        ServerPlayer player = miner(h, ModuleType.NEXUS_LINK);
        h.assertTrue(AiotModules.relink(player.getMainHandItem(), GlobalPos.of(h.getLevel().dimension(), h.absolutePos(terminal))), "linked");
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        long start = System.nanoTime();
        try {
            config.aiotVeinMaxBlocks = 64;
            mine(h, player, new BlockPos(2, 2, 2));
        } finally {
            config.aiotVeinMaxBlocks = max;
        }
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

    @GameTest
    public void theAreaKeySwitchesTheModule(GameTestHelper h) {
        // A 3x3 wall of stone at z = 3, centred on (2, 2, 3)
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        ServerPlayer player = miner(h, ModuleType.AREA_MINING);
        VeinKey.set(player, false); // V held would vein mine the stone instead
        BlockPos center = new BlockPos(2, 2, 3);
        lookAt(h, player, center);
        h.assertTrue(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "3x3 starts on");

        ToolMenu.toggleArea(player);
        h.assertFalse(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "the key turns 3x3 off");
        mine(h, player, center);
        int stoneLeft = 0;
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) if (h.getBlockState(new BlockPos(x, y, 3)).is(Blocks.STONE)) stoneLeft++;
        h.assertValueEqual(stoneLeft, 8, "with 3x3 off only the one block is mined");

        ToolMenu.toggleArea(player);
        h.assertTrue(AiotModules.active(player.getMainHandItem(), ModuleType.AREA_MINING), "and the key turns it back on");

        // An AIOT without the module, and any other item, are left alone
        ServerPlayer other = miner(h, ModuleType.NEXUS_LINK);
        ToolMenu.toggleArea(other);
        h.assertTrue(AiotModules.active(other.getMainHandItem(), ModuleType.NEXUS_LINK), "the key leaves the other modules alone");
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        ToolMenu.toggleArea(other);
        h.assertTrue(other.getMainHandItem().is(Items.DIAMOND_PICKAXE), "and a plain pickaxe");
        h.succeed();
    }

    @GameTest
    public void modulesDropWhenTheAiotBreaks(GameTestHelper h) {
        ServerPlayer player = miner(h, ModuleType.NEXUS_LINK, ModuleType.AREA_MINING);
        ItemStack tool = player.getMainHandItem();
        AiotModules.toggle(tool, 1); // 3x3 off: the module keeps that when it drops
        tool.setDamageValue(tool.getMaxDamage() - 1);
        BlockPos target = new BlockPos(2, 2, 3);
        h.setBlock(target, Blocks.STONE);
        lookAt(h, player, target);
        mine(h, player, target);

        h.assertTrue(player.getMainHandItem().isEmpty(), "the AIOT broke with that last block");
        List<ItemStack> modules = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.getItem() instanceof ModuleItem).toList();
        h.assertValueEqual(modules.size(), 2, "both modules dropped");
        for (ModuleType type : ModuleType.values()) {
            h.assertTrue(modules.stream().anyMatch(stack -> stack.is(ModItems.MODULES.get(type))), "the " + type.id() + " module dropped");
        }
        h.assertTrue(modules.stream().filter(stack -> stack.is(ModItems.MODULES.get(ModuleType.AREA_MINING)))
                .allMatch(stack -> stack.has(ModComponents.MODULE_OFF)), "the 3x3 module is still switched off");

        // An AIOT that wears out with nobody holding it has nowhere to drop them, and is just used up
        ItemStack lone = miner(h, ModuleType.AREA_MINING).getMainHandItem();
        lone.setDamageValue(lone.getMaxDamage() - 1);
        lone.hurtAndBreak(1, h.getLevel(), null, item -> {});
        h.assertTrue(lone.isEmpty(), "it is used up");
        h.succeed();
    }

    @GameTest
    public void tunnelLengthStepsInTheToolMenuAndWithTheWheel(GameTestHelper h) {
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotTunnelMaxLength;
        try {
            ServerPlayer player = miner(h, ModuleType.AREA_MINING);
            h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), 1, "a new 3x3 module is a plain 3x3");
            int[] expected = {2, 3, 8, 16, 24, 32, 1};
            for (int length : expected) {
                h.assertTrue(ToolMenu.cycleTunnelLength(player, player.getMainHandItem()), "the button works with the module in");
                h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), length, "the next tunnel length");
            }
            // The server's limit cuts the steps short: 2, 3, 8, then off
            config.aiotTunnelMaxLength = 8;
            for (int length : new int[]{2, 3, 8, 1}) {
                ToolMenu.cycleTunnelLength(player, player.getMainHandItem());
                h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), length, "the next length, limit 8");
            }
            // The tunnel key held with the wheel moves it up and down the same steps, and stops at the ends
            for (int length : new int[]{2, 3, 8, 8}) {
                ToolMenu.stepTunnel(player, 1);
                h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), length, "the wheel up steps to the next length (limit 8, then no further)");
            }
            for (int length : new int[]{3, 2, 1, 1}) {
                ToolMenu.stepTunnel(player, -1);
                h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), length, "the wheel down steps to the one before (then stays off)");
            }
            config.aiotTunnelMaxLength = 32;
            for (int length : new int[]{2, 3, 8, 16, 24, 32, 32}) {
                ToolMenu.stepTunnel(player, 1);
                h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), length, "up through every step to the top at 32");
            }
            ToolMenu.stepTunnel(player, -1);
            h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), 24, "and back down from the top");
            // A length from an older version that is no longer a step (4) goes to the nearest step in the direction of the wheel
            var slots = AiotModules.slots(player.getMainHandItem());
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).getItem() instanceof net.storagenexus.aiot.module.ModuleItem m && m.type() == ModuleType.AREA_MINING) {
                    ItemStack module = slots.get(i).copy();
                    module.set(net.storagenexus.registry.ModComponents.TUNNEL_LENGTH, 4);
                    slots.set(i, module);
                }
            }
            AiotModules.setSlots(player.getMainHandItem(), slots);
            h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), 4, "an older setting of 4 still digs 4");
            ToolMenu.stepTunnel(player, 1);
            h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), 8, "up from 4 is 8");
            AiotModules.setSlots(player.getMainHandItem(), slots);
            ToolMenu.stepTunnel(player, -1);
            h.assertValueEqual(AiotModules.tunnelLength(player.getMainHandItem()), 3, "down from 4 is 3");
            // No 3x3 module, no tunnel
            ServerPlayer other = miner(h, ModuleType.NEXUS_LINK);
            ToolMenu.stepTunnel(other, 1);
            h.assertValueEqual(AiotModules.tunnelLength(other.getMainHandItem()), 1, "the wheel does nothing without a 3x3 module");
            h.assertFalse(ToolMenu.cycleTunnelLength(other, other.getMainHandItem()), "nothing to set without a 3x3 module");
        } finally {
            config.aiotTunnelMaxLength = max;
        }
        h.succeed();
    }

    @GameTest
    public void aTunnelMinesLayersAwayFromThePlayer(GameTestHelper h) {
        // A solid block of stone 3 wide, 3 high and 5 deep (z = 3 to 7), with obsidian at the back of the first layer
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) for (int z = 3; z <= 7; z++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 3), Blocks.OBSIDIAN);
        ServerPlayer player = miner(h, ModuleType.AREA_MINING);
        VeinKey.set(player, false); // V held would vein mine the stone instead
        ToolMenu.cycleTunnelLength(player, player.getMainHandItem()); // 2
        ToolMenu.cycleTunnelLength(player, player.getMainHandItem()); // 3
        BlockPos center = new BlockPos(2, 2, 3);
        lookAt(h, player, center);
        int damage = player.getMainHandItem().getDamageValue();
        mine(h, player, center);
        for (int z = 3; z <= 5; z++) {
            for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) {
                if (x == 1 && y == 1 && z == 3) continue;
                h.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, z));
            }
        }
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(1, 1, 3));
        for (int z = 6; z <= 7; z++) for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
        h.assertValueEqual(player.getMainHandItem().getDamageValue() - damage, 3 * 9 - 1, "1 durability per block: 26 (obsidian left)");

        // Sneaking still mines the one block
        for (int z = 3; z <= 6; z++) for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        player.setShiftKeyDown(true);
        mine(h, player, center);
        h.assertBlockPresent(Blocks.AIR, center);
        h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 2, 4));
        h.succeed();
    }

    @GameTest
    public void veinMiningTakesStoneButNotContainers(GameTestHelper h) {
        // Rows along x: dirt (z = 1), stone and cobblestone (z = 3), two chests (z = 5)
        for (int x = 1; x <= 4; x++) {
            h.setBlock(new BlockPos(x, 1, 1), Blocks.DIRT);
            h.setBlock(new BlockPos(x, 1, 3), x <= 2 ? Blocks.STONE : Blocks.COBBLESTONE);
        }
        h.setBlock(new BlockPos(1, 1, 5), Blocks.CHEST);
        h.setBlock(new BlockPos(2, 1, 5), Blocks.CHEST);
        ServerPlayer player = miner(h);
        mine(h, player, new BlockPos(1, 1, 1));
        for (int x = 1; x <= 4; x++) h.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, 1));
        // Stone veins too, but only stone: the cobblestone next to it is another kind
        mine(h, player, new BlockPos(1, 1, 3));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 3));
        h.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(3, 1, 3));
        mine(h, player, new BlockPos(3, 1, 3));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(4, 1, 3));
        mine(h, player, new BlockPos(1, 1, 5));
        h.assertBlockPresent(Blocks.CHEST, new BlockPos(2, 1, 5));
        h.succeed();
    }

    @GameTest
    public void veinMiningClearsGrassesBushesAndLeafLitterTogether(GameTestHelper h) {
        // A mixed patch on grass blocks: short and tall grass, ferns, a bush, a dead bush and leaf litter, all touching
        net.minecraft.world.level.block.Block[] plants = {Blocks.SHORT_GRASS, Blocks.FERN, Blocks.BUSH, Blocks.LEAF_LITTER,
                Blocks.SHORT_DRY_GRASS, Blocks.FIREFLY_BUSH};
        for (int x = 0; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 2), Blocks.GRASS_BLOCK);
        h.setBlock(new BlockPos(6, 1, 2), Blocks.SAND);
        for (int x = 0; x < plants.length; x++) h.setBlock(new BlockPos(x, 2, 2), plants[x]);
        h.setBlock(new BlockPos(6, 2, 2), Blocks.DEAD_BUSH);
        ServerPlayer player = miner(h);
        int damage = player.getMainHandItem().getDamageValue();
        mine(h, player, new BlockPos(0, 2, 2));
        for (int x = 0; x <= 6; x++) h.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, 2));
        h.assertValueEqual(player.getMainHandItem().getDamageValue(), damage, "plants that break at once cost no durability");
        h.succeed();
    }

    @GameTest
    public void veinMiningNeedsNoModuleAndTakesObsidianCropsAndLeaves(GameTestHelper h) {
        // Rows along x, one block type each, z = 1, 3, 5 ... (no module in the AIOT)
        Block[] blocks = {Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.HAY_BLOCK, Blocks.MELON, Blocks.PUMPKIN,
                Blocks.OAK_LEAVES, Blocks.CHERRY_LEAVES, Blocks.MANGROVE_LEAVES};
        for (int row = 0; row < blocks.length; row++) {
            for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 1, row), blocks[row]);
        }
        ServerPlayer player = miner(h);
        for (int row = 0; row < blocks.length; row++) {
            mine(h, player, new BlockPos(1, 1, row));
            h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, row));
        }
        // Obsidian needs a diamond-grade tool to drop anything, so a stone AIOT takes one block only
        for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, 1, 9), Blocks.OBSIDIAN);
        ServerPlayer weak = h.makeMockServerPlayerInLevel();
        weak.setGameMode(GameType.SURVIVAL);
        weak.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AIOTS.get(AiotTier.STONE)));
        VeinKey.set(weak, true);
        mine(h, weak, new BlockPos(1, 1, 9));
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 1, 9));
        VeinKey.set(player, false);
        VeinKey.set(weak, false);
        h.succeed();
    }

    // ---------------------------------------------------------------- the retired Vein Mining Module

    private static net.minecraft.nbt.CompoundTag slotTag(int index, String id) {
        net.minecraft.nbt.CompoundTag slot = new net.minecraft.nbt.CompoundTag();
        slot.putInt("slot", index);
        net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
        item.putString("id", id);
        item.putInt("count", 1);
        slot.put("item", item);
        return slot;
    }

    /** An AIOT the way a world saved before the Vein Mining Module was retired holds it: three module slots, vein mining in one. */
    private static ItemStack oldAiot(GameTestHelper h, int veinSlot, int areaSlot, int linkSlot) {
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        net.minecraft.nbt.CompoundTag aiot = new net.minecraft.nbt.CompoundTag();
        aiot.putString("id", "storagenexus:diamond_aiot");
        aiot.putInt("count", 1);
        net.minecraft.nbt.ListTag modules = new net.minecraft.nbt.ListTag();
        modules.add(slotTag(veinSlot, "storagenexus:vein_mining_module"));
        modules.add(slotTag(areaSlot, "storagenexus:area_mining_module"));
        modules.add(slotTag(linkSlot, "storagenexus:nexus_link_module"));
        net.minecraft.nbt.CompoundTag components = new net.minecraft.nbt.CompoundTag();
        components.put("storagenexus:modules", modules);
        aiot.put("components", components);
        return ItemStack.CODEC.parse(ops, aiot).result().orElse(ItemStack.EMPTY);
    }

    @GameTest
    public void theRetiredModuleKeepsOldAiotsLoadingAndCannotBeCrafted(GameTestHelper h) {
        // An item id that no longer exists would make the whole AIOT fail to load, so the id stays registered
        ItemStack old = oldAiot(h, 0, 1, 2);
        h.assertTrue(!old.isEmpty() && old.is(ModItems.AIOTS.get(AiotTier.DIAMOND)), "an AIOT saved with the Vein Mining Module still loads");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.storagenexus.StorageNexus.id("vein_mining_module"))
                instanceof net.storagenexus.aiot.module.RetiredModuleItem, "the old id is the retired item");
        // But it is no module any more: no module type, no recipe, not in the creative tab
        h.assertValueEqual(ModuleType.values().length, 2, "two module types");
        h.assertTrue(!ModItems.TAB_ORDER.contains(ModItems.RETIRED_VEIN_MINING_MODULE), "not in the creative tab");
        boolean recipe = h.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(r -> r.id().identifier().getPath().equals("vein_mining_module"));
        h.assertTrue(!recipe, "no recipe for it");
        h.succeed();
    }

    @GameTest
    public void oldThreeSlotAiotsKeepTheirOtherModules(GameTestHelper h) {
        // Vein mining in slot 0, 3x3 in slot 1 (off) and the Nexus Link in the slot that no longer exists
        ItemStack old = oldAiot(h, 0, 1, 2);
        var slots = AiotModules.slots(old);
        h.assertValueEqual(slots.size(), 2, "two slots now");
        h.assertTrue(slots.get(1).getItem() instanceof ModuleItem m && m.type() == ModuleType.AREA_MINING, "the 3x3 module keeps its slot");
        h.assertTrue(slots.get(0).getItem() instanceof ModuleItem m && m.type() == ModuleType.NEXUS_LINK, "the Nexus Link moves to the free slot");
        h.assertValueEqual(AiotModules.stored(old).size(), 3, "the retired module is still in the data until it is taken out");

        // Every layout reads the same way: the two real modules, whatever order they were saved in
        for (int[] layout : new int[][]{{2, 0, 1}, {1, 2, 0}, {2, 1, 0}, {0, 2, 1}}) {
            var read = AiotModules.slots(oldAiot(h, layout[0], layout[1], layout[2]));
            h.assertTrue(!AiotModules.find(oldAiot(h, layout[0], layout[1], layout[2]), ModuleType.NEXUS_LINK).isEmpty()
                    && !AiotModules.find(oldAiot(h, layout[0], layout[1], layout[2]), ModuleType.AREA_MINING).isEmpty(), "both modules found");
            h.assertTrue(read.stream().filter(stack -> !stack.isEmpty()).count() == 2, "and exactly two stacks");
        }
        // A layout saved with the current two slots is read as it is
        ItemStack current = new ItemStack(ModItems.AIOTS.get(AiotTier.DIAMOND));
        var mine = AiotModules.slots(current);
        mine.set(1, module(ModuleType.AREA_MINING));
        AiotModules.setSlots(current, mine);
        h.assertTrue(AiotModules.slots(current).get(0).isEmpty() && AiotModules.slots(current).get(1).getItem() instanceof ModuleItem,
                "a module in the second slot stays in the second slot");
        h.succeed();
    }

    @GameTest
    public void openingTheToolMenuHandsBackARetiredModule(GameTestHelper h) {
        ServerPlayer player = miner(h);
        ItemStack old = oldAiot(h, 0, 1, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, old);
        ToolMenuMenu menu = new ToolMenuMenu(1, player.getInventory(), player.getInventory().getSelectedSlot());
        h.assertValueEqual(AiotModules.stored(player.getMainHandItem()).size(), 2, "the retired module is out of the AIOT");
        h.assertTrue(!AiotModules.find(player.getMainHandItem(), ModuleType.AREA_MINING).isEmpty()
                && !AiotModules.find(player.getMainHandItem(), ModuleType.NEXUS_LINK).isEmpty(), "and the other two stay");
        h.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 1, "its diamond came back");
        h.assertValueEqual(player.getInventory().countItem(Items.LAPIS_LAZULI), 4, "its 4 lapis");
        h.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 4, "and its 4 iron ingots");
        // The menu shows the modules that are left
        h.assertTrue(menu.getSlot(0).getItem().getItem() instanceof ModuleItem && menu.getSlot(1).getItem().getItem() instanceof ModuleItem, "both slots are filled");
        h.succeed();
    }

    @GameTest
    public void aLooseRetiredModuleGivesItsMaterialsWhenUsed(GameTestHelper h) {
        ServerPlayer player = miner(h);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RETIRED_VEIN_MINING_MODULE));
        // The way the game does it when the player right-clicks with it in hand
        var result = player.gameMode.useItem(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        h.assertTrue(result.consumesAction(), "right-clicking it does something");
        h.assertTrue(!player.getMainHandItem().is(ModItems.RETIRED_VEIN_MINING_MODULE), "and uses it up, " + player.getMainHandItem());
        h.assertTrue(player.getMainHandItem().is(Items.IRON_INGOT), "it turns into its iron: " + player.getMainHandItem());
        h.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 1, "a diamond");
        h.assertValueEqual(player.getInventory().countItem(Items.LAPIS_LAZULI), 4, "4 lapis");
        h.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 4, "4 iron ingots");
        h.succeed();
    }

    @GameTest
    public void aRetiredModuleInsideABreakingAiotStillDrops(GameTestHelper h) {
        ServerPlayer player = miner(h);
        ItemStack old = oldAiot(h, 0, 1, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, old);
        old.setDamageValue(old.getMaxDamage() - 1);
        BlockPos target = new BlockPos(2, 2, 3);
        h.setBlock(target, Blocks.STONE);
        lookAt(h, player, target);
        VeinKey.set(player, false);
        mine(h, player, target);
        h.assertTrue(player.getMainHandItem().isEmpty(), "the AIOT broke");
        long dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(ModItems.RETIRED_VEIN_MINING_MODULE) || stack.getItem() instanceof ModuleItem).count();
        h.assertValueEqual(dropped, 3L, "all three stacks dropped, the retired one too");
        h.succeed();
    }

    @GameTest
    public void veinSizeNeedsNoModule(GameTestHelper h) {
        ServerPlayer player = miner(h);
        ItemStack tool = player.getMainHandItem();
        var config = net.storagenexus.essentials.config.NexusConfig.get();
        int max = config.aiotVeinMaxBlocks;
        try {
            config.aiotVeinMaxBlocks = 256;
            h.assertValueEqual(AiotModules.veinSize(tool), 256, "an AIOT with no modules mines up to the limit");
            h.assertValueEqual(AiotModules.cycleVeinSize(tool), 4, "the button steps from the limit back to 4");
            h.assertValueEqual(AiotModules.cycleVeinSize(tool), 9, "then 9");
            h.assertValueEqual(AiotModules.cycleVeinSize(tool), 18, "then 18");
            h.assertValueEqual(AiotModules.veinSize(tool), 18, "and the AIOT remembers it");
            h.assertValueEqual(AiotModules.cycleVeinSize(new ItemStack(Items.DIAMOND_PICKAXE)), 0, "a plain pickaxe has no vein size");
        } finally {
            config.aiotVeinMaxBlocks = max;
        }
        h.succeed();
    }
}
