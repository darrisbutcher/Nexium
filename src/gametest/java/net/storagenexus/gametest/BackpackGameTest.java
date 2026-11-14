package net.storagenexus.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.backpack.AutoTorch;
import net.storagenexus.essentials.backpack.BackpackContents;
import net.storagenexus.essentials.backpack.BackpackMenu;
import net.storagenexus.essentials.backpack.BackpackModuleType;
import net.storagenexus.essentials.backpack.BackpackModules;
import net.storagenexus.essentials.backpack.Backpacks;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Backpack: its slot above the magnet slot, what it holds (36 slots, counts above 99), its module slots and their
 * switches, the Torch Stack module (torches to 256 a slot, and what happens to the extra torches when it goes), and the
 * Auto Torch module (torches from the Backpack, spaced out so nothing hostile can spawn, even behind walls and in caves
 * nobody has seen).
 */
public class BackpackGameTest {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);
    private static final int TORCH_SLOT = BackpackModules.SLOTS;

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(CENTER)));
        return player;
    }

    private static ItemStack module(BackpackModuleType type) {
        return new ItemStack(ModItems.BACKPACK_MODULES.get(type));
    }

    private static ItemStack backpackWith(BackpackModuleType... types) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        List<ItemStack> modules = new ArrayList<>();
        for (BackpackModuleType type : types) modules.add(module(type));
        while (modules.size() < BackpackModules.SLOTS) modules.add(ItemStack.EMPTY);
        BackpackModules.setSlots(backpack, modules);
        return backpack;
    }

    private static void storeTorches(ItemStack backpack, int count) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < BackpackContents.SIZE; i++) slots.add(ItemStack.EMPTY);
        int slot = 0;
        for (int left = count; left > 0; left -= BackpackContents.TORCH_STACK_MAX) slots.set(slot++, new ItemStack(Items.TORCH, Math.min(left, BackpackContents.TORCH_STACK_MAX)));
        BackpackContents.write(backpack, slots);
    }

    private static BackpackMenu openEquipped(ServerPlayer player) {
        return new BackpackMenu(1, player.getInventory(), Backpacks.EQUIPPED);
    }

    private static int total(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = BackpackContents.count(ExtraSlots.of(player).backpack(), item);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    // ---------------------------------------------------------------- the slot

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBackpackSlotTakesOnlyABackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        Slot slot = player.inventoryMenu.getSlot(ExtraSlots.MENU_BACKPACK);
        h.assertTrue(slot.mayPlace(new ItemStack(ModItems.BACKPACK)), "a backpack fits the backpack slot");
        h.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND)) && !slot.mayPlace(new ItemStack(ModItems.MAGNET)), "nothing else does");
        h.assertValueEqual(slot.getMaxStackSize(), 1, "it holds one");
        h.assertTrue(player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET).x == slot.x && slot.y < player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET).y,
                "it sits right above the trinket slot");

        player.getInventory().setItem(10, new ItemStack(ModItems.BACKPACK));
        player.inventoryMenu.quickMoveStack(player, 10);
        h.assertTrue(ExtraSlots.of(player).backpack().is(ModItems.BACKPACK), "shift-click moved the backpack to the backpack slot");
        h.assertTrue(player.getInventory().getItem(10).isEmpty(), "and out of the inventory");

        ExtraSlots.of(player).clearContent();
        player.setGameMode(GameType.CREATIVE);
        player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(ExtraSlots.MENU_BACKPACK, new ItemStack(Items.STONE)));
        h.assertTrue(ExtraSlots.of(player).backpack().isEmpty(), "creative can't put stone in it");
        player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(ExtraSlots.MENU_BACKPACK, new ItemStack(ModItems.BACKPACK)));
        h.assertTrue(ExtraSlots.of(player).backpack().is(ModItems.BACKPACK), "but a backpack, yes");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void sneakRightClickWearsItAndRightClickOpensIt(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack held = new ItemStack(ModItems.BACKPACK);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setShiftKeyDown(true);
        InteractionResult worn = ModItems.BACKPACK.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(worn == InteractionResult.SUCCESS && ExtraSlots.of(player).backpack().is(ModItems.BACKPACK), "sneak + right-click wears it");
        h.assertTrue(player.getMainHandItem().isEmpty(), "it left the hand");

        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BACKPACK));
        ModItems.BACKPACK.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.containerMenu instanceof BackpackMenu menu && menu.source() == player.getInventory().getSelectedSlot(),
                "right-click opens the backpack in the hand");
        player.closeContainer();

        Backpacks.openEquipped(player);
        h.assertTrue(player.containerMenu instanceof BackpackMenu menu && menu.source() == Backpacks.EQUIPPED, "the key opens the worn one");
        player.closeContainer();

        ExtraSlots.of(player).clearContent();
        Backpacks.openEquipped(player);
        h.assertTrue(!(player.containerMenu instanceof BackpackMenu), "with none worn the key opens nothing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBackpackAndItsContentsSaveAndDropOnDeath(GameTestHelper h) throws ReflectiveOperationException {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.TORCH_STACK, BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 300);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        player.saveWithoutId(output);
        ExtraSlots loaded = new ExtraSlots();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(loaded.backpack().is(ModItems.BACKPACK), "the backpack is saved with the player");
        h.assertValueEqual(BackpackContents.count(loaded.backpack(), Items.TORCH), 300, "with its 300 torches (stacks of 256 and 44)");
        h.assertTrue(BackpackModules.find(loaded.backpack(), BackpackModuleType.TORCH_STACK).is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.TORCH_STACK)), "and its modules");

        var dropEquipment = Player.class.getDeclaredMethod("dropEquipment", ServerLevel.class);
        dropEquipment.setAccessible(true);
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, true, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(!ExtraSlots.of(player).backpack().isEmpty(), "keepInventory keeps it");
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, false, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(ExtraSlots.of(player).backpack().isEmpty(), "dying empties the slot");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.BACKPACK) && BackpackContents.count(e.getItem(), Items.TORCH) == 300),
                "the backpack dropped with everything in it");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void countsAbove99SurviveTheSaveAndTheNetwork(GameTestHelper h) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        storeTorches(backpack, 256);
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = ItemStack.CODEC.encodeStart(ops, backpack).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        h.assertValueEqual(BackpackContents.count(decoded, Items.TORCH), 256, "256 torches saved and loaded again (vanilla's stack codec stops at 99)");
        h.assertValueEqual(BackpackContents.read(decoded).get(0).getCount(), 256, "in one slot");

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, backpack);
        h.assertValueEqual(BackpackContents.count(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), Items.TORCH), 256, "and sent to the client in full");
        RegistryFriendlyByteBuf torches = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(torches, new ItemStack(Items.TORCH, 256));
        h.assertValueEqual(ItemStack.OPTIONAL_STREAM_CODEC.decode(torches).getCount(), 256, "a stack of 256 torches crosses the network as it is");
        h.succeed();
    }

    // ---------------------------------------------------------------- the screen's menu

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuWritesEveryChangeBackToTheBackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, new ItemStack(ModItems.BACKPACK));
        BackpackMenu menu = openEquipped(player);
        h.assertValueEqual(menu.slots.size(), BackpackModules.SLOTS + BackpackContents.SIZE + 36, "5 module slots, 36 backpack slots and the inventory");
        menu.getSlot(TORCH_SLOT).set(new ItemStack(Items.DIAMOND, 5));
        menu.getSlot(TORCH_SLOT + 35).set(new ItemStack(Items.APPLE, 7));
        ItemStack backpack = ExtraSlots.of(player).backpack();
        h.assertValueEqual(BackpackContents.usedSlots(backpack), 2, "two slots are used");
        h.assertValueEqual(BackpackContents.count(backpack, Items.DIAMOND), 5, "5 diamonds are in it");
        h.assertValueEqual(BackpackContents.read(backpack).get(35).getCount(), 7, "7 apples in the last slot");

        BackpackMenu again = openEquipped(player);
        h.assertTrue(again.getSlot(TORCH_SLOT).getItem().is(Items.DIAMOND) && again.getSlot(TORCH_SLOT + 35).getItem().is(Items.APPLE), "opened again, it is all still there");

        menu.getSlot(TORCH_SLOT).set(ItemStack.EMPTY);
        h.assertValueEqual(BackpackContents.usedSlots(ExtraSlots.of(player).backpack()), 1, "taking the diamonds out empties that slot");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aBackpackDoesNotGoInsideABackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, new ItemStack(ModItems.BACKPACK));
        BackpackMenu menu = openEquipped(player);
        h.assertTrue(!menu.getSlot(TORCH_SLOT).mayPlace(new ItemStack(ModItems.BACKPACK)), "no backpack inside a backpack");
        h.assertTrue(menu.getSlot(TORCH_SLOT).mayPlace(new ItemStack(Items.SHULKER_BOX)), "a shulker box is fine");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void modulesGoInTheirOwnSlotsOnePerTypeWithASwitch(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, new ItemStack(ModItems.BACKPACK));
        BackpackMenu menu = openEquipped(player);
        h.assertTrue(menu.getSlot(0).mayPlace(module(BackpackModuleType.AUTO_TORCH)), "a module fits a module slot");
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.TORCH)), "a torch doesn't");
        h.assertTrue(menu.getSlot(TORCH_SLOT).mayPlace(module(BackpackModuleType.AUTO_TORCH)), "and as an item a module can be kept in a backpack slot");
        menu.getSlot(0).set(module(BackpackModuleType.AUTO_TORCH));
        h.assertTrue(!menu.getSlot(1).mayPlace(module(BackpackModuleType.AUTO_TORCH)), "a second Auto Torch module doesn't fit");
        h.assertTrue(menu.getSlot(1).mayPlace(module(BackpackModuleType.TORCH_STACK)), "but a Torch Stack module does");

        ItemStack backpack = ExtraSlots.of(player).backpack();
        h.assertTrue(BackpackModules.active(backpack, BackpackModuleType.AUTO_TORCH), "the module is installed and on");
        h.assertTrue(menu.clickMenuButton(player, BackpackMenu.BTN_TOGGLE_MODULE), "the switch for slot 0 works");
        h.assertTrue(menu.getSlot(0).getItem().has(ModComponents.MODULE_OFF), "it is off");
        h.assertTrue(!BackpackModules.active(ExtraSlots.of(player).backpack(), BackpackModuleType.AUTO_TORCH), "and the backpack says so");
        menu.clickMenuButton(player, BackpackMenu.BTN_TOGGLE_MODULE);
        h.assertTrue(BackpackModules.active(ExtraSlots.of(player).backpack(), BackpackModuleType.AUTO_TORCH), "the same switch turns it on again");
        menu.clickMenuButton(player, BackpackMenu.BTN_TOGGLE_MODULE + 3);
        h.assertTrue(BackpackModules.active(ExtraSlots.of(player).backpack(), BackpackModuleType.AUTO_TORCH), "a switch over an empty slot does nothing");

        menu.getSlot(0).set(ItemStack.EMPTY);
        h.assertTrue(BackpackModules.find(ExtraSlots.of(player).backpack(), BackpackModuleType.AUTO_TORCH).isEmpty(), "taking the module out uninstalls it");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theSwitchSurvivesTakingTheModuleOut(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        BackpackModules.toggle(backpack, 0);
        h.assertTrue(BackpackModules.slots(backpack).get(0).has(ModComponents.MODULE_OFF), "the module is off, kept on the module itself");
        h.assertTrue(BackpackModules.toggle(backpack, 4) == null, "an empty slot has nothing to switch");
        h.succeed();
    }

    // ---------------------------------------------------------------- Torch Stack

    @GameTest(environment = EssentialsEnvironment.ID)
    public void torchesStackTo256WithTheModule(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpackWith(BackpackModuleType.TORCH_STACK));
        BackpackMenu menu = openEquipped(player);
        h.assertTrue(menu.torchStack(), "the module is in and on");
        Slot slot = menu.getSlot(TORCH_SLOT);
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.TORCH)), 256, "a slot takes 256 torches");
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.COBBLESTONE)), 64, "but cobblestone still stacks to 64");
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)), 1, "and a sword to 1");

        ItemStack torches = new ItemStack(Items.TORCH, 256);
        slot.safeInsert(torches);
        h.assertValueEqual(slot.getItem().getCount(), 256, "256 torches went into the slot");
        h.assertTrue(torches.isEmpty(), "all of them");
        h.assertValueEqual(BackpackContents.read(ExtraSlots.of(player).backpack()).get(0).getCount(), 256, "and are saved as 256");

        // Shift-clicking a stack from the inventory tops it up, past 99
        slot.set(new ItemStack(Items.TORCH, 200));
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 64));
        menu.quickMoveStack(player, menu.slots.size() - 9);
        h.assertValueEqual(slot.getItem().getCount(), 256, "shift-click topped it up to 256");
        h.assertTrue(player.getInventory().getItem(0).isEmpty(), "the other 8 went to another free slot of the backpack");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 264, "264 torches in the backpack in all");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void torchesStackTo64WithoutTheModuleOrWithItOff(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpackWith());
        BackpackMenu menu = openEquipped(player);
        h.assertTrue(!menu.torchStack(), "no module: no stacking past 64");
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getMaxStackSize(new ItemStack(Items.TORCH)), 64, "a slot takes 64 torches");

        menu.getSlot(0).set(module(BackpackModuleType.TORCH_STACK));
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getMaxStackSize(new ItemStack(Items.TORCH)), 256, "with the module, 256");
        menu.clickMenuButton(player, BackpackMenu.BTN_TOGGLE_MODULE);
        h.assertTrue(!menu.torchStack(), "the module switched off");
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getMaxStackSize(new ItemStack(Items.TORCH)), 64, "is back to 64");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void takingTheModuleOutKeepsEveryTorch(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.TORCH_STACK);
        storeTorches(backpack, 256);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        BackpackMenu menu = openEquipped(player);
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getItem().getCount(), 256, "the stack of 256 is there");

        menu.getSlot(0).set(ItemStack.EMPTY);
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack stack = menu.getSlot(TORCH_SLOT + i).getItem();
            h.assertTrue(stack.getCount() <= stack.getMaxStackSize(), "slot " + i + " holds at most a normal stack, holds " + stack.getCount());
        }
        h.assertValueEqual(total(player, Items.TORCH), 256, "all 256 torches are still there, in the backpack or the inventory");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 256, "here, in the backpack's other slots");

        // Full backpack: the extra goes to the inventory
        ItemStack full = backpackWith(BackpackModuleType.TORCH_STACK);
        List<ItemStack> slots = new ArrayList<>();
        slots.add(new ItemStack(Items.TORCH, 256));
        for (int i = 1; i < BackpackContents.SIZE; i++) slots.add(new ItemStack(Items.COBBLESTONE, 64));
        BackpackContents.write(full, slots);
        player.getInventory().clearContent();
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, full);
        BackpackMenu fullMenu = openEquipped(player);
        fullMenu.getSlot(0).set(ItemStack.EMPTY);
        h.assertValueEqual(fullMenu.getSlot(TORCH_SLOT).getItem().getCount(), 64, "the slot keeps 64");
        h.assertValueEqual(total(player, Items.TORCH), 256, "and the other 192 went to the inventory");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuHoldsStacksOverTheItemsOwnLimit(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.TORCH_STACK);
        storeTorches(backpack, 256);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        BackpackMenu menu = openEquipped(player);
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getItem().getCount(), 256, "the server's menu holds 256");
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getItem().getMaxStackSize(), 64, "of an item that stacks to 64");
        h.succeed();
    }

    // ---------------------------------------------------------------- Auto Torch: when it works

    @GameTest(environment = EssentialsEnvironment.ID)
    public void autoTorchNeedsTheModuleOnTorchesAndTheBackpackWorn(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 10);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        h.assertTrue(AutoTorch.active(player), "worn, with the module on and torches in it: working");

        player.setGameMode(GameType.ADVENTURE);
        h.assertTrue(!AutoTorch.active(player), "not in adventure mode");
        player.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!AutoTorch.active(player), "not in spectator mode");
        player.setGameMode(GameType.SURVIVAL);
        h.assertTrue(AutoTorch.active(player), "back in survival: working again");

        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);
        h.assertTrue(!AutoTorch.active(player), "module off: not working");
        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);

        BackpackContents.take(ExtraSlots.of(player).backpack(), Items.TORCH, 10);
        h.assertTrue(!AutoTorch.active(player), "no torches in the backpack: not working");

        ItemStack inInventory = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(inInventory, 10);
        ExtraSlots.of(player).clearContent();
        player.getInventory().setItem(0, inInventory);
        h.assertTrue(!AutoTorch.active(player), "a backpack that isn't worn does nothing");

        ItemStack worn = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(worn, 10);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
        h.assertTrue(AutoTorch.active(player), "worn: working");
        Backpacks.open(player, Backpacks.EQUIPPED);
        h.assertTrue(!AutoTorch.active(player), "not while the backpack's screen is open");
        player.closeContainer();
        h.assertTrue(AutoTorch.active(player), "and again once it is closed");
        h.succeed();
    }

    // ---------------------------------------------------------------- Auto Torch: placing torches

    /** A sealed stone box: floor at y-1, air from y up to y+3, ceiling at y+4, walls where the interior ends. */
    private static void room(ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                for (int y = -1; y <= 4; y++) {
                    boolean inside = x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= 0 && y <= 3;
                    level.setBlock(origin.offset(x, y, z), inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean chunksLoaded(ServerLevel level, BlockPos origin, int radius) {
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null || !chunk.isLightCorrect()) return false;
            }
        }
        return true;
    }

    private static void forceChunks(ServerLevel level, BlockPos origin, int radius, boolean on) {
        for (int cx = (origin.getX() - radius) >> 4; cx <= (origin.getX() + radius) >> 4; cx++) {
            for (int cz = (origin.getZ() - radius) >> 4; cz <= (origin.getZ() + radius) >> 4; cz++) level.setChunkForced(cx, cz, on);
        }
    }

    private static int countTorches(ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ) {
        int n = 0;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) for (int y = 0; y <= 3; y++) if (level.getBlockState(origin.offset(x, y, z)).is(Blocks.TORCH)) n++;
        return n;
    }

    /** Every torch within the radius of the player, wherever it is: in the room or on the dark ground around it. */
    private static int countTorchesAround(ServerLevel level, BlockPos origin, int radius) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(Blocks.TORCH)) n++;
        }
        return n;
    }

    /** Every floor block of the area has block light, so nothing hostile can spawn on it. */
    private static void assertLit(GameTestHelper h, ServerLevel level, BlockPos origin, int minX, int maxX, int minZ, int maxZ, String where) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = origin.offset(x, 0, z);
                if (level.getBlockState(pos).is(Blocks.TORCH)) continue;
                h.assertTrue(level.getBrightness(LightLayer.BLOCK, pos) >= 1, where + ": " + pos.toShortString() + " is still dark");
            }
        }
    }

    @GameTest(environment = "storagenexus:isolated", padding = 40, maxTicks = 200)
    public void autoTorchLightsADarkRoomSpacedOut(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH, BackpackModuleType.TORCH_STACK);
        storeTorches(backpack, 64);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        int[] placed = {0};
        h.startSequence()
                .thenExecute(() -> forceChunks(level, origin, 20, true))
                .thenWaitUntil(() -> h.assertTrue(chunksLoaded(level, origin, 20), "waiting for the chunks around the room"))
                .thenExecute(() -> {
                    room(level, origin, -14, 14, -14, 14);
                    placed[0] = AutoTorch.sweepNow(player);
                    h.assertTrue(placed[0] >= 2 && placed[0] <= AutoTorch.MAX_PER_SWEEP, "one sweep placed several torches: " + placed[0]);
                    h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 64 - placed[0], "taken from the backpack, one each");
                    h.assertValueEqual(countTorchesAround(level, origin, 32), placed[0], "that many torches stand around: the room, and the dark ground outside it");
                    h.assertTrue(countTorches(level, origin, -14, 14, -14, 14) >= 2, "several of them in the room");
                })
                .thenWaitUntil(() -> assertLit(h, level, origin, -14, 14, -14, 14, "the room"))
                .thenExecute(() -> {
                    int before = countTorches(level, origin, -14, 14, -14, 14);
                    int more = AutoTorch.sweepNow(player);
                    int standing = countTorches(level, origin, -14, 14, -14, 14);
                    forceChunks(level, origin, 20, false);
                    System.out.println("[auto torch] 29x29 room: " + standing + " torches in it (" + placed[0] + " torches in the first sweep around the player, " + more + " in the second)");
                    // A map of the room for reading by eye: block light on the floor (0-e), T for a torch
                    for (int x = -14; x <= 14; x++) {
                        StringBuilder row = new StringBuilder("[light map] ");
                        for (int z = -14; z <= 14; z++) {
                            BlockPos pos = origin.offset(x, 0, z);
                            row.append(level.getBlockState(pos).is(Blocks.TORCH) ? 'T' : Character.forDigit(level.getBrightness(LightLayer.BLOCK, pos), 16));
                        }
                        System.out.println(row);
                    }
                    h.assertValueEqual(standing, before, "once the room is lit, the next sweep puts no more torches in it");
                    h.assertTrue(standing <= 8, "a 29 x 29 room needs only a handful of torches, has " + standing);
                })
                .thenSucceed();
    }

    @GameTest(environment = "storagenexus:isolated", padding = 40, maxTicks = 200)
    public void autoTorchLightsBothSidesOfAWallAndACaveNobodySees(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH, BackpackModuleType.TORCH_STACK);
        storeTorches(backpack, 128);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        h.startSequence()
                .thenExecute(() -> forceChunks(level, origin, 36, true))
                .thenWaitUntil(() -> h.assertTrue(chunksLoaded(level, origin, 36), "waiting for the chunks around"))
                .thenExecute(() -> {
                    // A room of 2 halves with a solid wall between them (the player is in the first), and a sealed cave behind stone
                    room(level, origin, -14, 14, -14, 14);
                    for (int z = -14; z <= 14; z++) for (int y = 0; y <= 3; y++) level.setBlock(origin.offset(6, y, z), Blocks.STONE.defaultBlockState(), 2);
                    room(level, origin.offset(22, 0, 0), -3, 3, -3, 3);
                    h.assertTrue(AutoTorch.sweepNow(player) >= 1, "the first sweep placed torches");
                })
                // The light engine needs a moment before the next sweep can see the new light
                .thenIdle(6).thenExecute(() -> AutoTorch.sweepNow(player))
                .thenIdle(6).thenExecute(() -> AutoTorch.sweepNow(player))
                .thenIdle(6).thenExecute(() -> {
                    h.assertTrue(countTorches(level, origin.offset(22, 0, 0), -3, 3, -3, 3) >= 1, "a torch stands in the cave behind the stone, which nobody has seen");
                    h.assertTrue(countTorches(level, origin, 7, 14, -14, 14) >= 1, "and one on the far side of the wall");
                })
                .thenWaitUntil(() -> {
                    assertLit(h, level, origin, -14, 5, -14, 14, "the near half");
                    assertLit(h, level, origin, 7, 14, -14, 14, "the far half");
                    assertLit(h, level, origin.offset(22, 0, 0), -3, 3, -3, 3, "the cave");
                })
                .thenExecute(() -> forceChunks(level, origin, 36, false))
                .thenSucceed();
    }

    @GameTest(environment = "storagenexus:isolated", padding = 40, maxTicks = 100)
    public void autoTorchLeavesLitGroundAndRunsOutOfTorchesGracefully(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 2);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        h.startSequence()
                .thenExecute(() -> forceChunks(level, origin, 20, true))
                .thenWaitUntil(() -> h.assertTrue(chunksLoaded(level, origin, 20), "waiting for the chunks around the room"))
                .thenExecute(() -> {
                    room(level, origin, -14, 14, -14, 14);
                    int placed = AutoTorch.sweepNow(player);
                    h.assertValueEqual(placed, 2, "with 2 torches it places 2");
                    h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 0, "and the backpack is out of them");
                    h.assertValueEqual(AutoTorch.sweepNow(player), 0, "out of torches it does nothing");
                    forceChunks(level, origin, 20, false);
                })
                .thenSucceed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void thePlanSkipsGroundWhereItMayNotBuild(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 10);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        player.setGameMode(GameType.ADVENTURE);
        h.assertValueEqual(AutoTorch.sweepNow(player), 0, "adventure mode can't place blocks, and neither can Auto Torch");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 10, "no torches used");
        h.succeed();
    }
}
