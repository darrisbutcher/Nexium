package net.nexium.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.nexium.essentials.backpack.BackpackCompacting;
import net.nexium.essentials.backpack.BackpackFeeding;
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
import net.nexium.essentials.backpack.AutoTorch;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackMenu;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.backpack.Backpacks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Backpack: its slot above the magnet slot, what it holds (36 slots, counts above 99), its module slots and their
 * switches, stacks of 1,024 (and the retired Torch Stack module still loading), and the
 * Auto Torch module (torches from the Backpack, spaced out so nothing hostile can spawn, even behind walls and in caves
 * nobody has seen), the Feeding module (eats only what fits the missing hunger, never foods with effects), the Compacting
 * module (packs storage blocks, only packs that craft straight back) and the recipes of the Backpack and every module.
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
        for (int left = count; left > 0; left -= BackpackContents.STACK_MAX) slots.set(slot++, new ItemStack(Items.TORCH, Math.min(left, BackpackContents.STACK_MAX)));
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
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 600);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        player.saveWithoutId(output);
        ExtraSlots loaded = new ExtraSlots();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(loaded.backpack().is(ModItems.BACKPACK), "the backpack is saved with the player");
        h.assertValueEqual(BackpackContents.count(loaded.backpack(), Items.TORCH), 600, "with its 600 torches (one stack)");
        h.assertTrue(BackpackModules.find(loaded.backpack(), BackpackModuleType.AUTO_TORCH).is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.AUTO_TORCH)), "and its modules");

        var dropEquipment = Player.class.getDeclaredMethod("dropEquipment", ServerLevel.class);
        dropEquipment.setAccessible(true);
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, true, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(!ExtraSlots.of(player).backpack().isEmpty(), "keepInventory keeps it");
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, false, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(ExtraSlots.of(player).backpack().isEmpty(), "dying empties the slot");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.BACKPACK) && BackpackContents.count(e.getItem(), Items.TORCH) == 600),
                "the backpack dropped with everything in it");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void countsAbove99SurviveTheSaveAndTheNetwork(GameTestHelper h) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        storeTorches(backpack, 512);
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = ItemStack.CODEC.encodeStart(ops, backpack).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        h.assertValueEqual(BackpackContents.count(decoded, Items.TORCH), 512, "512 torches saved and loaded again (vanilla's stack codec stops at 99)");
        h.assertValueEqual(BackpackContents.read(decoded).get(0).getCount(), 512, "in one slot");

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, backpack);
        h.assertValueEqual(BackpackContents.count(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), Items.TORCH), 512, "and sent to the client in full");
        RegistryFriendlyByteBuf torches = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(torches, new ItemStack(Items.TORCH, 512));
        h.assertValueEqual(ItemStack.OPTIONAL_STREAM_CODEC.decode(torches).getCount(), 512, "a stack of 512 torches crosses the network as it is");
        h.succeed();
    }

    // ---------------------------------------------------------------- the screen's menu

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuWritesEveryChangeBackToTheBackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, new ItemStack(ModItems.BACKPACK));
        BackpackMenu menu = openEquipped(player);
        h.assertValueEqual(menu.slots.size(), BackpackModules.SLOTS + BackpackContents.SIZE + 36 + 9 + 1 + 1,
                "5 module slots, 36 backpack slots, the inventory, and after it the crafting grid (9), its result slot and the trash slot");
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
        h.assertTrue(menu.getSlot(1).mayPlace(module(BackpackModuleType.RESTOCKING)), "but a Restocking module does");
        h.assertTrue(!menu.getSlot(1).mayPlace(module(BackpackModuleType.TORCH_STACK)), "and the retired Torch Stack module does not");

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

    // ---------------------------------------------------------------- Stacks of 1024

    @GameTest(environment = EssentialsEnvironment.ID)
    public void everyStackableItemStacksTo1024(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpackWith());
        BackpackMenu menu = openEquipped(player);
        Slot slot = menu.getSlot(TORCH_SLOT);
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.TORCH)), 1024, "a slot takes 1,024 torches");
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.COBBLESTONE)), 1024, "and 1,024 cobblestone");
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)), 1024, "and 1,024 of an item that stacks to 16");
        h.assertValueEqual(slot.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)), 1, "but a sword still holds 1");

        ItemStack torches = new ItemStack(Items.TORCH, 1024);
        slot.safeInsert(torches);
        h.assertValueEqual(slot.getItem().getCount(), 1024, "1,024 torches went into the slot");
        h.assertTrue(torches.isEmpty(), "all of them");
        h.assertValueEqual(BackpackContents.read(ExtraSlots.of(player).backpack()).get(0).getCount(), 1024, "and are saved as 1,024");

        // Shift-clicking a stack from the inventory tops it up, past 99
        slot.set(new ItemStack(Items.COBBLESTONE, 1000));
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        int hotbar = -1;
        for (Slot s : menu.slots) if (s.container == player.getInventory() && s.getContainerSlot() == 0) hotbar = s.index;
        menu.quickMoveStack(player, hotbar);
        h.assertValueEqual(slot.getItem().getCount(), 1024, "shift-click topped it up to 1,024");
        h.assertTrue(player.getInventory().getItem(0).isEmpty(), "the other 40 went to another free slot of the backpack");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.COBBLESTONE), 1064, "1,064 cobblestone in the backpack in all");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void everyItemShiftClicksIntoTheBackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpackWith());
        BackpackMenu menu = openEquipped(player);
        int hotbar = -1;
        for (Slot s : menu.slots) if (s.container == player.getInventory() && s.getContainerSlot() == 0) hotbar = s.index;
        java.util.List<String> failed = new java.util.ArrayList<>();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item == Items.AIR || item instanceof net.nexium.essentials.backpack.BackpackItem || item instanceof net.nexium.essentials.backpack.BackpackModuleItem) continue;
            for (int i = 0; i < BackpackContents.SIZE; i++) menu.getSlot(BackpackModules.SLOTS + i).set(ItemStack.EMPTY);
            ItemStack stack = new ItemStack(item);
            player.getInventory().setItem(0, stack);
            menu.quickMoveStack(player, hotbar);
            if (!player.getInventory().getItem(0).isEmpty()) failed.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
            player.getInventory().setItem(0, ItemStack.EMPTY);
        }
        h.assertTrue(failed.isEmpty(), "these did not shift-click into the backpack: " + failed);
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theRetiredTorchStackModuleDoesNothingAndStillLoads(GameTestHelper h) {
        ServerPlayer player = player(h);
        // A backpack saved with the old module in it: it opens, keeps the module in its slot, and stacks as every backpack does
        ItemStack backpack = backpackWith(BackpackModuleType.TORCH_STACK);
        storeTorches(backpack, 600);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        BackpackMenu menu = openEquipped(player);
        h.assertTrue(menu.getSlot(0).getItem().is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.TORCH_STACK)), "the old module is still in its slot");
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getItem().getCount(), 600, "the 600 torches are one stack of 600");
        h.assertValueEqual(menu.getSlot(TORCH_SLOT + 1).getItem().getCount(), 0, "nothing was split off");
        h.assertTrue(BackpackModuleType.TORCH_STACK.retired(), "it is retired");
        h.assertTrue(!BackpackModuleType.RESTOCKING.retired() && !BackpackModuleType.AUTO_TORCH.retired(), "the others are not");
        h.assertTrue(!menu.getSlot(1).mayPlace(module(BackpackModuleType.TORCH_STACK)), "and it can't be put in a slot again");
        h.assertTrue(!net.nexium.essentials.registry.ModItems.TAB_ORDER.contains(ModItems.BACKPACK_MODULES.get(BackpackModuleType.TORCH_STACK)), "nor is it in the creative tab");
        menu.getSlot(0).set(ItemStack.EMPTY);
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.TORCH), 600, "taking it out loses nothing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuHoldsStacksOverTheItemsOwnLimit(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack backpack = backpackWith();
        storeTorches(backpack, 1024);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        BackpackMenu menu = openEquipped(player);
        h.assertValueEqual(menu.getSlot(TORCH_SLOT).getItem().getCount(), 1024, "the server's menu holds 1,024");
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
        h.assertTrue(AutoTorch.active(player), "with the backpack's screen open too");
        player.closeContainer();
        h.assertTrue(AutoTorch.active(player), "and once it is closed");
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

    @GameTest(environment = "nexium:isolated", padding = 40, maxTicks = 300)
    public void autoTorchLightsADarkRoomSpacedOut(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
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
                // The module's own tick sweeps run for this player too: let any in flight finish and the light settle, so the count below is
                // not racing a torch placed a moment ago (the light of which the next sweep has not read yet)
                .thenIdle(60)
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
                    // The module's own tick sweeps also run for this player while the test does, so a torch or two more than the explicit sweeps place is normal (6 to 9 seen)
                    h.assertTrue(standing <= 10, "a 29 x 29 room needs only a handful of torches, has " + standing);
                })
                .thenSucceed();
    }

    @GameTest(environment = "nexium:isolated", padding = 40, maxTicks = 200)
    public void autoTorchLightsBothSidesOfAWallAndACaveNobodySees(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
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

    @GameTest(environment = "nexium:isolated", padding = 40, maxTicks = 100)
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
    // ---------------------------------------------------------------- recipes

    private static ItemStack craft(GameTestHelper h, String pattern, java.util.Map<Character, net.minecraft.world.item.Item> key) {
        List<ItemStack> grid = new ArrayList<>();
        for (char c : pattern.toCharArray()) grid.add(c == '.' ? ItemStack.EMPTY : new ItemStack(key.get(c)));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        java.util.Optional<RecipeHolder<CraftingRecipe>> recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        return recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBackpackAndEveryModuleCraftFromTheirRecipes(GameTestHelper h) {
        h.assertTrue(craft(h, "LLLLCLLLL", java.util.Map.of('L', Items.LEATHER, 'C', Items.CHEST)).is(ModItems.BACKPACK), "8 leather around a chest make a Backpack");
        h.assertTrue(craft(h, "ITITLTITI", java.util.Map.of('I', Items.IRON_INGOT, 'T', Items.TORCH, 'L', Items.REDSTONE_LAMP))
                .is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.AUTO_TORCH)), "iron, 4 torches and a redstone lamp make the Auto Torch module");
        h.assertTrue(craft(h, "ITITCTITI", java.util.Map.of('I', Items.IRON_INGOT, 'T', Items.TORCH, 'C', Items.CHEST)).isEmpty(),
                "the retired Torch Stack module has no recipe any more");
        h.assertTrue(craft(h, "IHICDCIHI", java.util.Map.of('I', Items.IRON_INGOT, 'H', Items.HOPPER, 'C', Items.CHEST, 'D', Items.DIAMOND))
                .is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.RESTOCKING)), "iron, 2 hoppers, 2 chests and a diamond make the Restocking module");
        h.assertTrue(craft(h, "IBIBGBIBI", java.util.Map.of('I', Items.IRON_INGOT, 'B', Items.BREAD, 'G', Items.GOLDEN_CARROT))
                .is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)), "iron, 4 bread and a golden carrot make the Feeding module");
        h.assertTrue(craft(h, "IPIPTPIPI", java.util.Map.of('I', Items.IRON_INGOT, 'P', Items.PISTON, 'T', Items.CRAFTING_TABLE))
                .is(ModItems.BACKPACK_MODULES.get(BackpackModuleType.COMPACTING)), "iron, 4 pistons and a crafting table make the Compacting module");
        h.succeed();
    }

    // ---------------------------------------------------------------- Feeding

    private static void fill(ItemStack backpack, ItemStack... stacks) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < BackpackContents.SIZE; i++) slots.add(i < stacks.length ? stacks[i] : ItemStack.EMPTY);
        BackpackContents.write(backpack, slots);
    }

    private static ServerPlayer wearing(GameTestHelper h, ItemStack backpack) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        return player;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingPicksTheBestFoodThatWastesNothing(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        fill(backpack, new ItemStack(Items.APPLE, 3), new ItemStack(Items.BREAD, 3), new ItemStack(Items.COOKED_BEEF, 3));
        ServerPlayer player = wearing(h, backpack);

        player.getFoodData().setFoodLevel(20);
        h.assertTrue(!BackpackFeeding.feed(player), "a full food bar eats nothing");
        player.getFoodData().setFoodLevel(18);
        h.assertTrue(!BackpackFeeding.feed(player), "missing 2: nothing in the backpack fits, so nothing is eaten (no waste)");
        h.assertValueEqual(BackpackContents.usedSlots(ExtraSlots.of(player).backpack()), 3, "all three stacks are still there");

        player.getFoodData().setFoodLevel(12);
        h.assertTrue(BackpackFeeding.feed(player), "missing 8: it eats");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.COOKED_BEEF), 2, "the cooked beef (8), the best that fits, not the bread or the apple");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 20, "and the food bar is full");

        player.getFoodData().setFoodLevel(15);
        h.assertTrue(BackpackFeeding.feed(player), "missing 5: it eats");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.BREAD), 2, "bread (5), not the apple (4): the one that restores the most without waste");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 20, "full again");

        player.getFoodData().setFoodLevel(16);
        h.assertTrue(BackpackFeeding.feed(player), "missing 4: it eats");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.APPLE), 2, "the apple (4), the only one that fits");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingNeverEatsFoodsWithSideEffects(GameTestHelper h) {
        for (net.minecraft.world.item.Item risky : List.of(Items.ROTTEN_FLESH, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.PUFFERFISH,
                Items.CHORUS_FRUIT, Items.POISONOUS_POTATO, Items.SPIDER_EYE, Items.HONEY_BOTTLE)) {
            h.assertTrue(BackpackFeeding.safeFood(new ItemStack(risky)) == null, risky + " is not safe to eat for you");
        }
        // A suspicious stew as it is made carries its effects, which act when it is eaten
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new net.minecraft.world.item.component.SuspiciousStewEffects(
                List.of(new net.minecraft.world.item.component.SuspiciousStewEffects.Entry(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 100))));
        h.assertTrue(BackpackFeeding.safeFood(stew) == null, "a suspicious stew with effects is not safe to eat for you");
        for (net.minecraft.world.item.Item plain : List.of(Items.BREAD, Items.APPLE, Items.COOKED_BEEF, Items.GOLDEN_CARROT, Items.DRIED_KELP, Items.MUSHROOM_STEW)) {
            h.assertTrue(BackpackFeeding.safeFood(new ItemStack(plain)) != null, plain + " is safe to eat for you");
        }
        h.assertTrue(BackpackFeeding.safeFood(new ItemStack(Items.DIAMOND)) == null && BackpackFeeding.safeFood(ItemStack.EMPTY) == null, "and what is not food is not eaten");

        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        fill(backpack, new ItemStack(Items.ROTTEN_FLESH, 8), new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.PUFFERFISH, 2), new ItemStack(Items.DIAMOND, 5));
        ServerPlayer player = wearing(h, backpack);
        player.getFoodData().setFoodLevel(0);
        h.assertTrue(!BackpackFeeding.feed(player), "starving, with only risky food: nothing is eaten");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 0, "the food bar is still empty");
        h.assertValueEqual(BackpackContents.usedSlots(ExtraSlots.of(player).backpack()), 4, "and nothing left the backpack");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingNeedsTheModuleOnTheBackpackWornAndASurvivalPlayer(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        fill(backpack, new ItemStack(Items.BREAD, 4));
        ServerPlayer player = wearing(h, backpack);
        player.getFoodData().setFoodLevel(5);

        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);
        h.assertTrue(!BackpackFeeding.feed(player), "module off: nothing is eaten");
        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);

        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(!BackpackFeeding.feed(player), "creative mode: nothing is eaten");
        player.setGameMode(GameType.SURVIVAL);

        ItemStack carried = ExtraSlots.of(player).backpack().copy();
        ExtraSlots.of(player).clearContent();
        player.getInventory().setItem(0, carried);
        h.assertTrue(!BackpackFeeding.feed(player), "a backpack that isn't worn does nothing");
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, carried);
        player.getInventory().clearContent();

        h.assertTrue(BackpackFeeding.feed(player), "worn, on, survival, screen closed, hungry: it eats");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.BREAD), 3, "one bread");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "restoring its 5 points");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingKeepsABowlFromStew(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        fill(backpack, new ItemStack(Items.MUSHROOM_STEW));
        ServerPlayer player = wearing(h, backpack);
        player.getFoodData().setFoodLevel(0);
        h.assertTrue(BackpackFeeding.feed(player), "it eats the stew");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.MUSHROOM_STEW), 0, "the stew is gone");
        h.assertValueEqual(total(player, Items.BOWL), 1, "and the bowl came back, in the backpack or the inventory");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingPicksByNutritionThenSaturationThenSlot(GameTestHelper h) {
        var foods = List.of(new BackpackFeeding.Food(0, 4, 2.4F), new BackpackFeeding.Food(1, 5, 6.0F), new BackpackFeeding.Food(2, 5, 9.0F), new BackpackFeeding.Food(3, 8, 12.8F),
                new BackpackFeeding.Food(4, 0, 1.0F), new BackpackFeeding.Food(5, 5, 9.0F));
        h.assertValueEqual(BackpackFeeding.pick(20, foods), 3, "plenty missing: the most nutritious");
        h.assertValueEqual(BackpackFeeding.pick(8, foods), 3, "exactly what the best restores");
        h.assertValueEqual(BackpackFeeding.pick(7, foods), 2, "too little for the best: 5 points, and of the two with the most saturation the earlier slot");
        h.assertValueEqual(BackpackFeeding.pick(4, foods), 0, "only the 4-point food fits");
        h.assertValueEqual(BackpackFeeding.pick(3, foods), -1, "nothing fits: nothing is eaten");
        h.assertValueEqual(BackpackFeeding.pick(5, List.of()), -1, "no food at all");
        h.assertValueEqual(BackpackFeeding.pick(5, List.of(new BackpackFeeding.Food(0, 0, 5.0F))), -1, "a food that restores nothing is never picked");
        h.succeed();
    }

    // ---------------------------------------------------------------- Feeding settings

    /** A Backpack with a Feeding module in slot 0 set to this level and waste setting. */
    private static ItemStack feedingBackpack(int level, boolean waste) {
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        List<ItemStack> slots = new ArrayList<>(BackpackModules.slots(backpack));
        slots.set(0, BackpackFeeding.withWaste(BackpackFeeding.withLevel(slots.get(0), level), waste));
        BackpackModules.setSlots(backpack, slots);
        return backpack;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingWaitsUntilTheFoodBarIsAtItsLevel(GameTestHelper h) {
        ItemStack backpack = feedingBackpack(14, false);
        fill(backpack, new ItemStack(Items.APPLE, 4)); // 4 points
        ServerPlayer player = wearing(h, backpack);
        player.getFoodData().setFoodLevel(15);
        h.assertTrue(!BackpackFeeding.feed(player), "level 14, food 15: not hungry enough yet, though an apple would fit");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.APPLE), 4, "nothing was eaten");
        player.getFoodData().setFoodLevel(14);
        h.assertTrue(BackpackFeeding.feed(player), "level 14, food 14: it eats");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 18, "an apple restores 4");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.APPLE), 3, "one apple gone");
        player.getFoodData().setFoodLevel(3);
        h.assertTrue(BackpackFeeding.feed(player), "well below the level it eats too");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingMayWasteOnceAtItsLevel(GameTestHelper h) {
        // Only an 8-point food, and 2 points missing: with never waste on nothing is eaten, with it off the food is eaten
        ItemStack strict = feedingBackpack(19, false);
        fill(strict, new ItemStack(Items.COOKED_BEEF, 4));
        ServerPlayer player = wearing(h, strict);
        player.getFoodData().setFoodLevel(18);
        h.assertTrue(!BackpackFeeding.feed(player), "never waste: 2 missing does not fit 8, so nothing is eaten");

        ItemStack lenient = feedingBackpack(19, true);
        fill(lenient, new ItemStack(Items.COOKED_BEEF, 4));
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, lenient);
        h.assertTrue(BackpackFeeding.feed(player), "may waste: it eats the beef though only 2 points are missing");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 20, "the bar is full (the rest of the food is lost)");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.COOKED_BEEF), 3, "one beef eaten");

        // And the level still applies: level 10 with waste allowed waits until the bar is at 10
        ItemStack both = feedingBackpack(10, true);
        fill(both, new ItemStack(Items.COOKED_BEEF, 4));
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, both);
        player.getFoodData().setFoodLevel(11);
        h.assertTrue(!BackpackFeeding.feed(player), "level 10, food 11: not yet, waste allowed or not");
        player.getFoodData().setFoodLevel(10);
        h.assertTrue(BackpackFeeding.feed(player), "level 10, food 10: it eats");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingWithWasteAllowedPicksTheBestFitThenTheSmallest(GameTestHelper h) {
        var foods = List.of(new BackpackFeeding.Food(0, 4, 2.4F), new BackpackFeeding.Food(1, 5, 6.0F), new BackpackFeeding.Food(2, 8, 12.8F),
                new BackpackFeeding.Food(3, 4, 9.0F), new BackpackFeeding.Food(4, 0, 1.0F));
        h.assertValueEqual(BackpackFeeding.pick(6, foods, true), 1, "something fits: the best fit as before (5 of 6)");
        h.assertValueEqual(BackpackFeeding.pick(3, foods, true), 3, "nothing fits 3: the smallest, 4, and of the two with 4 the one with more saturation");
        h.assertValueEqual(BackpackFeeding.pick(3, foods, false), -1, "never waste: nothing");
        h.assertValueEqual(BackpackFeeding.pick(1, List.of(new BackpackFeeding.Food(0, 8, 12.8F)), true), 0, "only a big food: it is eaten");
        h.assertValueEqual(BackpackFeeding.pick(3, List.of(new BackpackFeeding.Food(0, 0, 5.0F)), true), -1, "a food that restores nothing is never picked, waste or not");
        h.assertValueEqual(BackpackFeeding.pick(5, List.of(), true), -1, "no food at all");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingLevelStepsThroughItsListBothWays(GameTestHelper h) {
        int[] list = {19, 18, 16, 14, 12, 10, 8, 6, 4, 2};
        int level = 19;
        for (int i = 1; i <= list.length; i++) {
            level = BackpackFeeding.nextLevel(level, false);
            h.assertValueEqual(level, list[i % list.length], "stepping lower, step " + i + " (and round to 19 after the last)");
        }
        for (int i = 1; i <= list.length; i++) {
            level = BackpackFeeding.nextLevel(level, true);
            h.assertValueEqual(level, list[(list.length - i) % list.length], "stepping higher, step " + i + " (the other way round the list)");
        }
        h.assertValueEqual(BackpackFeeding.nextLevel(15, false), 14, "a level between two in the list goes on to the lower one");
        h.assertValueEqual(BackpackFeeding.nextLevel(15, true), 16, "or back to the higher one");
        h.assertValueEqual(BackpackFeeding.nextLevel(1, false), 19, "below the list it wraps round");
        h.assertValueEqual(BackpackFeeding.nextLevel(1, true), 2, "or comes back up to the lowest");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aModuleLeftAloneStoresNothingAndStacksWithANewOne(GameTestHelper h) {
        ItemStack fresh = module(BackpackModuleType.FEEDING);
        h.assertValueEqual(BackpackFeeding.level(fresh), 19, "a new module eats at 19");
        h.assertTrue(!BackpackFeeding.allowsWaste(fresh), "and never wastes");
        ItemStack set = BackpackFeeding.withWaste(BackpackFeeding.withLevel(fresh, 12), true);
        h.assertValueEqual(BackpackFeeding.level(set), 12, "the level is kept on the module");
        h.assertTrue(BackpackFeeding.allowsWaste(set), "and so is the waste switch");
        ItemStack back = BackpackFeeding.withWaste(BackpackFeeding.withLevel(set, 19), false);
        h.assertTrue(ItemStack.isSameItemSameComponents(back, fresh), "set back to the defaults it is the same as a new module again");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theScreenButtonsChangeTheModulesSettingsAndTheyAreSaved(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.COMPACTING, BackpackModuleType.FEEDING); // Feeding is not in the first slot
        ServerPlayer player = wearing(h, backpack);
        BackpackMenu menu = openScreen(player);
        java.util.function.Supplier<ItemStack> saved = () -> BackpackModules.find(ExtraSlots.of(player).backpack(), BackpackModuleType.FEEDING);

        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_LEVEL);
        h.assertValueEqual(BackpackFeeding.level(saved.get()), 18, "a click on the level button lowers it one step, and the Backpack has it saved");
        h.assertValueEqual(BackpackFeeding.level(menu.getSlot(1).getItem()), 18, "the screen's module slot shows the same");
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_LEVEL);
        h.assertValueEqual(BackpackFeeding.level(saved.get()), 16, "and another");
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_LEVEL_BACK);
        h.assertValueEqual(BackpackFeeding.level(saved.get()), 18, "the back button raises it again");

        h.assertTrue(!BackpackFeeding.allowsWaste(saved.get()), "never waste to begin with");
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_WASTE);
        h.assertTrue(BackpackFeeding.allowsWaste(saved.get()), "the waste button allows it");
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_WASTE);
        h.assertTrue(!BackpackFeeding.allowsWaste(saved.get()), "and switches it off again");

        h.assertTrue(BackpackModules.find(ExtraSlots.of(player).backpack(), BackpackModuleType.COMPACTING).getComponents()
                .equals(module(BackpackModuleType.COMPACTING).getComponents()), "the other module was not touched");
        player.closeContainer();
        h.assertValueEqual(BackpackFeeding.level(saved.get()), 18, "closing the screen keeps it");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theScreenButtonsDoNothingWithoutAFeedingModule(GameTestHelper h) {
        ServerPlayer player = wearing(h, backpackWith(BackpackModuleType.COMPACTING));
        BackpackMenu menu = openScreen(player);
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_LEVEL);
        menu.clickMenuButton(player, BackpackMenu.BTN_FEEDING_WASTE);
        h.assertTrue(BackpackModules.find(ExtraSlots.of(player).backpack(), BackpackModuleType.FEEDING).isEmpty(), "no Feeding module appeared");
        h.assertTrue(BackpackModules.find(ExtraSlots.of(player).backpack(), BackpackModuleType.COMPACTING).getComponents()
                .equals(module(BackpackModuleType.COMPACTING).getComponents()), "and the Compacting module has no settings put on it");
        h.succeed();
    }

    // ---------------------------------------------------------------- Crafting module and trash

    /** A player wearing a Backpack with the Crafting module on and these stacks in its storage. */
    private static ServerPlayer craftingWith(GameTestHelper h, ItemStack... stacks) {
        ItemStack backpack = backpackWith(BackpackModuleType.CRAFTING);
        fill(backpack, stacks);
        return wearing(h, backpack);
    }

    private static void click(BackpackMenu menu, ServerPlayer player, Slot slot, net.minecraft.world.inventory.ContainerInput input) {
        menu.clicked(slot.index, 0, input, player);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theCraftingGridCraftsAndRefillsFromTheBackpack(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 14));
        BackpackMenu menu = openScreen(player);
        h.assertTrue(menu.craftingActive(), "the module is installed and on");
        // Two planks one above the other make four sticks
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS));
        menu.getInputGridSlots().get(3).set(new ItemStack(Items.OAK_PLANKS));
        h.assertTrue(menu.getResultSlot().getItem().is(Items.STICK) && menu.getResultSlot().getItem().getCount() == 4, "the result slot shows 4 sticks, got " + menu.getResultSlot().getItem());
        click(menu, player, menu.getResultSlot(), net.minecraft.world.inventory.ContainerInput.PICKUP);
        h.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 4, "the sticks are in the hand");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 12, "the two planks that went into the grid came from the Backpack's 14, so 12 are left");
        h.assertTrue(menu.getInputGridSlots().get(0).getItem().is(Items.OAK_PLANKS) && menu.getInputGridSlots().get(3).getItem().is(Items.OAK_PLANKS), "and both grid slots are full again");
        h.assertTrue(menu.getResultSlot().getItem().is(Items.STICK), "so the next 4 sticks are already showing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void shiftClickOnTheResultCraftsAsMuchAsThereIs(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 14));
        BackpackMenu menu = openScreen(player);
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS));
        menu.getInputGridSlots().get(3).set(new ItemStack(Items.OAK_PLANKS));
        click(menu, player, menu.getResultSlot(), net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
        int sticks = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(Items.STICK)) sticks += player.getInventory().getItem(i).getCount();
        h.assertValueEqual(sticks, 32, "all 16 planks (2 in the grid and 14 in the Backpack) became 32 sticks in your inventory");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 0, "none left in the Backpack");
        h.assertTrue(menu.getInputGridSlots().get(0).getItem().isEmpty(), "and the grid is empty");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theGridIsOnlyThereWhileTheModuleIsOn(GameTestHelper h) {
        ServerPlayer without = wearing(h, backpackWith());
        BackpackMenu menu = openScreen(without);
        Slot grid = menu.getInputGridSlots().get(0);
        h.assertTrue(!menu.craftingActive() && !grid.isActive() && !grid.mayPlace(new ItemStack(Items.OAK_PLANKS)), "no module: no grid");
        h.assertTrue(!menu.getResultSlot().isActive() && !menu.getResultSlot().mayPickup(without), "and no result slot");
        without.closeContainer();

        ServerPlayer with = craftingWith(h);
        BackpackMenu on = openScreen(with);
        h.assertTrue(on.craftingActive() && on.getInputGridSlots().get(0).isActive() && on.getInputGridSlots().get(0).mayPlace(new ItemStack(Items.OAK_PLANKS)), "the module on: the grid is there");
        on.clickMenuButton(with, BackpackMenu.BTN_TOGGLE_MODULE);
        h.assertTrue(!on.craftingActive() && !on.getInputGridSlots().get(0).isActive(), "switched off: the grid is gone");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void takingTheModuleOutOrSwitchingItOffReturnsTheGrid(GameTestHelper h) {
        ServerPlayer player = craftingWith(h);
        BackpackMenu menu = openScreen(player);
        menu.getInputGridSlots().get(0).set(new ItemStack(Items.OAK_PLANKS, 3));
        menu.getInputGridSlots().get(4).set(new ItemStack(Items.COBBLESTONE, 5));
        menu.clickMenuButton(player, BackpackMenu.BTN_TOGGLE_MODULE); // switched off
        h.assertTrue(menu.getInputGridSlots().get(0).getItem().isEmpty() && menu.getInputGridSlots().get(4).getItem().isEmpty(), "the grid is emptied");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 3, "the planks are in the Backpack");
        h.assertValueEqual(held(player, Items.COBBLESTONE), 5, "and the cobblestone");
        h.assertTrue(menu.getResultSlot().getItem().isEmpty(), "and there is no result left showing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void closingTheScreenReturnsTheGridAndEmptiesTheTrash(GameTestHelper h) {
        ServerPlayer player = craftingWith(h);
        BackpackMenu menu = openScreen(player);
        menu.getInputGridSlots().get(1).set(new ItemStack(Items.IRON_INGOT, 4));
        menu.trashSlot().set(new ItemStack(Items.DIRT, 10));
        player.closeContainer();
        h.assertValueEqual(held(player, Items.IRON_INGOT), 4, "what was in the grid is back in the Backpack");
        h.assertTrue(menu.trashSlot().getItem().isEmpty(), "the trash is empty");
        h.assertValueEqual(total(player, Items.DIRT), 0, "and what was in it is gone, not in the Backpack or the inventory");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theTrashKeepsTheLastItemUntilAnotherGoesIn(GameTestHelper h) {
        ServerPlayer player = wearing(h, backpackWith()); // no module: the trash is always there
        BackpackMenu menu = openScreen(player);
        Slot trash = menu.trashSlot();
        menu.setCarried(new ItemStack(Items.DIRT, 20));
        click(menu, player, trash, net.minecraft.world.inventory.ContainerInput.PICKUP);
        h.assertTrue(trash.getItem().is(Items.DIRT) && trash.getItem().getCount() == 20 && menu.getCarried().isEmpty(), "dirt put in the trash stays in it, and the hand is empty");
        // Take it back
        click(menu, player, trash, net.minecraft.world.inventory.ContainerInput.PICKUP);
        h.assertTrue(menu.getCarried().is(Items.DIRT) && menu.getCarried().getCount() == 20 && trash.getItem().isEmpty(), "clicking the trash with an empty hand takes the dirt back");
        // Put it in, then put something else in: the first is destroyed
        click(menu, player, trash, net.minecraft.world.inventory.ContainerInput.PICKUP);
        menu.setCarried(new ItemStack(Items.STONE, 7));
        click(menu, player, trash, net.minecraft.world.inventory.ContainerInput.PICKUP);
        h.assertTrue(trash.getItem().is(Items.STONE) && trash.getItem().getCount() == 7 && menu.getCarried().isEmpty(), "putting stone in replaces the dirt: the trash holds the stone, and nothing was handed back");
        h.assertValueEqual(total(player, Items.DIRT), 0, "the dirt is gone");
        // Shift-click on it gives the item back to the inventory
        click(menu, player, trash, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
        h.assertTrue(trash.getItem().isEmpty(), "shift-click empties the trash");
        h.assertValueEqual(total(player, Items.STONE), 7, "into the inventory");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void shiftClickNeverPutsAnythingInTheTrashOrTheGrid(GameTestHelper h) {
        ServerPlayer player = craftingWith(h);
        BackpackMenu menu = openScreen(player);
        player.getInventory().setItem(10, new ItemStack(Items.OAK_PLANKS, 8));
        // Find the inventory slot that holds the planks
        Slot holder = null;
        for (Slot s : menu.slots) if (s.container == player.getInventory() && s.getItem().is(Items.OAK_PLANKS)) holder = s;
        h.assertTrue(holder != null, "the planks are in a player slot");
        click(menu, player, holder, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
        h.assertTrue(menu.trashSlot().getItem().isEmpty(), "nothing went in the trash");
        for (Slot grid : menu.getInputGridSlots()) h.assertTrue(grid.getItem().isEmpty(), "nothing went in the grid");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 8, "the planks went into the Backpack's storage");
        h.succeed();
    }

    // ---------------------------------------------------------------- Crafting module: recipe book and recipe viewers

    private static RecipeHolder<?> recipe(GameTestHelper h, String id) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.withDefaultNamespace(id));
        return h.getLevel().recipeAccess().byKey(key).orElseThrow();
    }

    private static int inInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    private static int inGrid(BackpackMenu menu, net.minecraft.world.item.Item item) {
        int n = 0;
        for (Slot s : menu.getInputGridSlots()) if (s.getItem().is(item)) n += s.getItem().getCount();
        return n;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theRecipeBookFillsTheGridFromTheBackpackBeforeTheInventory(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 3));
        player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        BackpackMenu menu = openScreen(player);
        // A crafting table is four planks, one in each of four slots
        menu.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        h.assertValueEqual(inGrid(menu, Items.OAK_PLANKS), 4, "four planks are in the grid");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 0, "all three of the Backpack's planks were used first");
        h.assertValueEqual(inInventory(player, Items.OAK_PLANKS), 4, "and one of the inventory's five after them");
        h.assertTrue(menu.getResultSlot().getItem().is(Items.CRAFTING_TABLE), "so the result slot shows a crafting table");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theResultSlotIsNeverPartOfADoubleClickPickUpAll(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 8));
        BackpackMenu menu = openScreen(player);
        h.assertFalse(menu.canTakeItemForPickAll(ItemStack.EMPTY, menu.getResultSlot()), "the result slot is left out, so a quick second click crafts again");
        h.assertTrue(menu.canTakeItemForPickAll(ItemStack.EMPTY, menu.getSlot(BackpackModules.SLOTS)), "an ordinary slot still takes part");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theCraftingGridCanBeHiddenAndTheBackpackRemembersIt(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 8));
        BackpackMenu menu = openScreen(player);
        h.assertTrue(menu.craftingActive() && !menu.craftingHidden(), "the grid starts showing");
        menu.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        h.assertValueEqual(inGrid(menu, Items.OAK_PLANKS), 4, "with four planks in it");

        h.assertTrue(menu.clickMenuButton(player, BackpackMenu.BTN_CRAFTING_HIDE), "the hide button works");
        h.assertTrue(menu.craftingHidden() && !menu.craftingActive() && menu.craftingModuleOn(), "the grid is hidden, the module still on");
        h.assertValueEqual(inGrid(menu, Items.OAK_PLANKS), 0, "what was in the grid went back");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 8, "into the Backpack");
        h.assertFalse(menu.getInputGridSlots().get(0).isActive() || menu.getResultSlot().isActive(), "no grid slot is there");
        h.assertFalse(menu.getInputGridSlots().get(0).mayPlace(new ItemStack(Items.OAK_PLANKS)), "and the grid takes nothing");
        h.assertTrue(menu.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), player.getInventory())
                == net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction.NOTHING && inGrid(menu, Items.OAK_PLANKS) == 0, "the recipe book places nothing in a hidden grid");

        // The Backpack remembers: a new menu opens with the grid hidden
        BackpackMenu again = openScreen(player);
        h.assertTrue(again.craftingHidden() && !again.craftingActive(), "reopened, it is still hidden");

        // Show it again
        h.assertTrue(again.clickMenuButton(player, BackpackMenu.BTN_CRAFTING_HIDE), "the same button shows it");
        h.assertTrue(again.craftingActive() && !again.craftingHidden(), "the grid is back");
        again.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), player.getInventory());
        h.assertValueEqual(inGrid(again, Items.OAK_PLANKS), 4, "and works");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theRecipeBookNeedsTheModuleAndTheIngredients(GameTestHelper h) {
        ServerPlayer without = wearing(h, backpackWith());
        without.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        BackpackMenu none = openScreen(without);
        h.assertTrue(none.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), without.getInventory()) == net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction.NOTHING
                && inGrid(none, Items.OAK_PLANKS) == 0, "without the module nothing is placed");
        without.closeContainer();

        ServerPlayer short1 = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 2));
        BackpackMenu menu = openScreen(short1);
        h.assertTrue(menu.handlePlacement(false, true, recipe(h, "crafting_table"), h.getLevel(), short1.getInventory())
                == net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE, "two planks are not enough for four: the book shows the ghost recipe");
        h.assertValueEqual(inGrid(menu, Items.OAK_PLANKS), 0, "and nothing is placed");
        h.assertValueEqual(held(short1, Items.OAK_PLANKS), 2, "the Backpack still has its two");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theRecipeBookCountsWhatTheBackpackHolds(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 4));
        BackpackMenu menu = openScreen(player);
        var stacked = new net.minecraft.world.entity.player.StackedItemContents();
        menu.fillCraftSlotsStackedContents(stacked);
        h.assertTrue(stacked.canCraft((net.minecraft.world.item.crafting.CraftingRecipe) recipe(h, "crafting_table").value(), null),
                "four planks only in the Backpack: the recipe book counts the crafting table as makeable");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aRecipeViewerFillsTheGridFromTheBackpackWithMaxItems(GameTestHelper h) {
        ServerPlayer player = craftingWith(h, new ItemStack(Items.OAK_PLANKS, 40));
        BackpackMenu menu = openScreen(player);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.withDefaultNamespace("crafting_table"));
        menu.fillGrid(player, new net.nexium.terminal.FillGridPayload(key, true));
        // 40 planks make 10 crafting tables: each of the four grid slots gets 10
        h.assertValueEqual(inGrid(menu, Items.OAK_PLANKS), 40, "the grid holds all 40 planks, 10 in each of four slots");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 0, "taken from the Backpack");
        h.assertTrue(menu.getResultSlot().getItem().is(Items.CRAFTING_TABLE), "a crafting table in the result slot");

        ServerPlayer none = wearing(h, backpackWith());
        none.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 8));
        BackpackMenu off = openScreen(none);
        off.fillGrid(none, new net.nexium.terminal.FillGridPayload(key, false));
        h.assertValueEqual(inGrid(off, Items.OAK_PLANKS), 0, "without the module a recipe viewer fills nothing");
        h.succeed();
    }

    // ---------------------------------------------------------------- Compacting

    private static ServerPlayer compactingWith(GameTestHelper h, ItemStack... stacks) {
        ItemStack backpack = backpackWith(BackpackModuleType.COMPACTING);
        fill(backpack, stacks);
        return wearing(h, backpack);
    }

    private static int held(ServerPlayer player, net.minecraft.world.item.Item item) {
        return BackpackContents.count(ExtraSlots.of(player).backpack(), item);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingPacksIngotsIntoBlocksAndKeepsTheRest(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_INGOT, 20), new ItemStack(Items.GOLD_INGOT, 8));
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs");
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 2, "18 of the 20 iron ingots became 2 iron blocks");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 2, "2 iron ingots are left");
        h.assertValueEqual(held(player, Items.GOLD_INGOT), 8, "8 gold ingots are too few for a block");
        h.assertValueEqual(held(player, Items.GOLD_BLOCK), 0, "so there is no gold block");
        h.assertTrue(!BackpackCompacting.compactNow(player), "a second pass finds nothing more to pack");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingGoesFromNuggetsToIngotsToBlocks(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_NUGGET, 64), new ItemStack(Items.IRON_NUGGET, 26));
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs");
        // 90 nuggets are 10 ingots, which are 1 block and 1 ingot
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "an iron block");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 1, "and an iron ingot");
        h.assertValueEqual(held(player, Items.IRON_NUGGET), 0, "and no nuggets");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingKnowsStorageBlocksOfEveryKind(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.WHEAT, 9), new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.REDSTONE, 9), new ItemStack(Items.LAPIS_LAZULI, 9),
                new ItemStack(Items.COAL, 9), new ItemStack(Items.EMERALD, 9), new ItemStack(Items.RAW_IRON, 9), new ItemStack(Items.SLIME_BALL, 9), new ItemStack(Items.BONE_MEAL, 9));
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs");
        for (net.minecraft.world.item.Item block : List.of(Items.HAY_BLOCK, Items.DIAMOND_BLOCK, Items.REDSTONE_BLOCK, Items.LAPIS_BLOCK, Items.COAL_BLOCK,
                Items.EMERALD_BLOCK, Items.RAW_IRON_BLOCK, Items.SLIME_BLOCK, Items.BONE_BLOCK)) {
            h.assertValueEqual(held(player, block), 1, block + " made");
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingLeavesPacksThatDoNotCraftBack(GameTestHelper h) {
        // Glowstone dust, snowballs, clay balls and melon slices used to be here: they have a way back now. These still have none
        // (planks make a crafting table, quartz, nether bricks and string make blocks that already craft into slabs, flint makes
        // gravel, honey bottles need their glass bottles back)
        ServerPlayer player = compactingWith(h, new ItemStack(Items.OAK_PLANKS, 16), new ItemStack(Items.QUARTZ, 8), new ItemStack(Items.STRING, 8),
                new ItemStack(Items.NETHER_BRICK, 8), new ItemStack(Items.FLINT, 8), new ItemStack(Items.HONEY_BOTTLE, 4));
        h.assertTrue(!BackpackCompacting.compactNow(player), "nothing here packs back, so nothing is packed");
        h.assertValueEqual(held(player, Items.OAK_PLANKS), 16, "planks stay");
        h.assertValueEqual(held(player, Items.QUARTZ), 8, "quartz stays");
        h.assertValueEqual(held(player, Items.STRING), 8, "string stays");
        h.assertValueEqual(held(player, Items.NETHER_BRICK), 8, "nether bricks stay");
        h.assertValueEqual(held(player, Items.FLINT), 8, "flint stays");
        h.assertValueEqual(held(player, Items.HONEY_BOTTLE), 4, "honey bottles stay");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingLeavesRenamedAndEnchantedItemsAlone(GameTestHelper h) {
        ItemStack renamed = new ItemStack(Items.IRON_INGOT, 9);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Dad's iron"));
        ItemStack mixed = new ItemStack(Items.GOLD_INGOT, 5);
        ItemStack namedGold = new ItemStack(Items.GOLD_INGOT, 5);
        namedGold.set(DataComponents.CUSTOM_NAME, Component.literal("Shiny"));
        ServerPlayer player = compactingWith(h, renamed, mixed, namedGold);
        h.assertTrue(!BackpackCompacting.compactNow(player), "9 renamed ingots, and 5 + 5 gold of which 5 are renamed: nothing packs");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 9, "the renamed iron is kept as it is");
        h.assertValueEqual(held(player, Items.GOLD_INGOT), 10, "and so is the gold");
        h.assertTrue(BackpackContents.read(ExtraSlots.of(player).backpack()).get(0).has(DataComponents.CUSTOM_NAME), "its name too");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingAddsToAnExistingBlockStack(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_BLOCK, 10), new ItemStack(Items.IRON_INGOT, 9));
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs");
        h.assertValueEqual(BackpackContents.read(ExtraSlots.of(player).backpack()).get(0).getCount(), 11, "the new block went on the stack of iron blocks");
        h.assertValueEqual(BackpackContents.usedSlots(ExtraSlots.of(player).backpack()), 1, "and the ingots' slot is empty");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingPacksBulkBlocksIntoCompressedBlocks(GameTestHelper h) {
        // 729 cobblestone in eleven stacks of 64 and one of 25: nine packs of 81, nine of those, then one more step
        ItemStack[] stacks = new ItemStack[12];
        for (int i = 0; i < 11; i++) stacks[i] = new ItemStack(Items.COBBLESTONE, 64);
        stacks[11] = new ItemStack(Items.COBBLESTONE, 25);
        ServerPlayer player = compactingWith(h, stacks);
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs");
        h.assertValueEqual(held(player, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.COBBLESTONE, 3)), 1, "one Triple Compressed Cobblestone");
        h.assertValueEqual(held(player, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.COBBLESTONE, 2)), 0, "and no Double Compressed left over");
        h.assertValueEqual(held(player, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.COBBLESTONE, 1)), 0, "or Compressed");
        h.assertValueEqual(held(player, Items.COBBLESTONE), 0, "or plain cobblestone");
        h.assertValueEqual(BackpackContents.usedSlots(ExtraSlots.of(player).backpack()), 1, "all in one slot");

        // 800 is one of those and 71 more: 7 Compressed (63) and 8 plain
        ItemStack[] more = new ItemStack[13];
        for (int i = 0; i < 12; i++) more[i] = new ItemStack(Items.COBBLESTONE, 64);
        more[12] = new ItemStack(Items.COBBLESTONE, 32);
        ServerPlayer other = compactingWith(h, more);
        h.assertTrue(BackpackCompacting.compactNow(other), "800 cobblestone pack");
        h.assertValueEqual(held(other, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.COBBLESTONE, 3)), 1, "a Triple Compressed");
        h.assertValueEqual(held(other, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.COBBLESTONE, 1)), 7, "7 Compressed");
        h.assertValueEqual(held(other, Items.COBBLESTONE), 8, "and 8 plain cobblestone");

        // Soil, and the Nether and End, pack the same way; sand is packed 9 to 1, not 4 to sandstone
        ServerPlayer soil = compactingWith(h, new ItemStack(Items.SAND, 20), new ItemStack(Items.DIRT, 9), new ItemStack(Items.NETHERRACK, 9), new ItemStack(Items.END_STONE, 18));
        h.assertTrue(BackpackCompacting.compactNow(soil), "they pack");
        h.assertValueEqual(held(soil, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.SAND, 1)), 2, "20 sand: 2 Compressed Sand");
        h.assertValueEqual(held(soil, Items.SAND), 2, "and 2 sand, with no sandstone");
        h.assertValueEqual(held(soil, Items.SANDSTONE), 0, "none");
        h.assertValueEqual(held(soil, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.DIRT, 1)), 1, "a Compressed Dirt");
        h.assertValueEqual(held(soil, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.NETHERRACK, 1)), 1, "a Compressed Netherrack");
        h.assertValueEqual(held(soil, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.END_STONE, 1)), 2, "two Compressed End Stone");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingPacksTheNewBlocksAndTheOneWayPacks(GameTestHelper h) {
        net.nexium.essentials.compressed.CompressedBlocks.Base calcite = net.nexium.essentials.compressed.CompressedBlocks.Base.CALCITE;
        ServerPlayer player = compactingWith(h, new ItemStack(Items.CALCITE, 20), new ItemStack(Items.SNOWBALL, 9), new ItemStack(Items.CLAY_BALL, 4),
                new ItemStack(Items.MELON_SLICE, 10), new ItemStack(Items.OBSIDIAN, 9), new ItemStack(Items.GLOWSTONE_DUST, 3));
        h.assertTrue(BackpackCompacting.compactNow(player), "they pack");
        h.assertValueEqual(held(player, net.nexium.essentials.compressed.CompressedBlocks.item(calcite, 1)), 2, "20 calcite: 2 Compressed Calcite");
        h.assertValueEqual(held(player, Items.CALCITE), 2, "and 2 calcite");
        h.assertValueEqual(held(player, Items.SNOW_BLOCK), 2, "9 snowballs: 2 snow blocks");
        h.assertValueEqual(held(player, Items.SNOWBALL), 1, "and a snowball");
        h.assertValueEqual(held(player, Items.CLAY), 1, "4 clay balls: a clay block");
        h.assertValueEqual(held(player, Items.MELON), 1, "9 of the 10 melon slices: a melon");
        h.assertValueEqual(held(player, Items.MELON_SLICE), 1, "and a slice");
        h.assertValueEqual(held(player, net.nexium.essentials.compressed.CompressedBlocks.item(net.nexium.essentials.compressed.CompressedBlocks.Base.OBSIDIAN, 1)), 1, "9 obsidian: a Compressed Obsidian");
        h.assertValueEqual(held(player, Items.GLOWSTONE_DUST), 3, "3 glowstone dust are too few for a block and stay");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingNeedsRoomForTheBlock(GameTestHelper h) {
        // 35 full slots, and 10 iron ingots in the last: the 9 that pack leave 1 ingot in the slot, so there is no room for the block
        ItemStack[] stacks = new ItemStack[BackpackContents.SIZE];
        for (int i = 0; i < stacks.length - 1; i++) stacks[i] = new ItemStack(Items.STICK, 64);
        stacks[stacks.length - 1] = new ItemStack(Items.IRON_INGOT, 10);
        ServerPlayer player = compactingWith(h, stacks);
        h.assertTrue(!BackpackCompacting.compactNow(player), "no room for a block: nothing is packed");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 10, "all 10 ingots are still there");
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 0, "and no block was lost into thin air");

        // 9 ingots free their own slot, so the block fits
        stacks[stacks.length - 1] = new ItemStack(Items.IRON_INGOT, 9);
        ItemStack backpack = backpackWith(BackpackModuleType.COMPACTING);
        fill(backpack, stacks);
        ServerPlayer other = wearing(h, backpack);
        h.assertTrue(BackpackCompacting.compactNow(other), "9 ingots in a full backpack: the block takes the slot they free");
        h.assertValueEqual(held(other, Items.IRON_BLOCK), 1, "an iron block");
        h.assertValueEqual(held(other, Items.IRON_INGOT), 0, "and no ingots");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingNeedsTheModuleOnAndTheBackpackWorn(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_INGOT, 9));
        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);
        h.assertTrue(!BackpackCompacting.compactNow(player), "module off: nothing is packed");
        BackpackModules.toggle(ExtraSlots.of(player).backpack(), 0);

        ItemStack carried = ExtraSlots.of(player).backpack().copy();
        ExtraSlots.of(player).clearContent();
        player.getInventory().setItem(0, carried);
        h.assertTrue(!BackpackCompacting.compactNow(player), "a backpack that isn't worn does nothing");
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, carried);
        player.getInventory().clearContent();

        h.assertTrue(BackpackCompacting.compactNow(player), "worn, on, screen closed: it packs");
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "an iron block");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void insertFillsStacksThenEmptySlotsAndReturnsWhatDoesNotFit(GameTestHelper h) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < 3; i++) slots.add(ItemStack.EMPTY);
        slots.set(0, new ItemStack(Items.COBBLESTONE, 60));
        slots.set(1, new ItemStack(Items.DIRT, 64));
        ItemStack left = BackpackContents.insert(slots, new ItemStack(Items.COBBLESTONE, 2100));
        h.assertValueEqual(slots.get(0).getCount(), 1024, "the stack of cobblestone is topped up to 1,024");
        h.assertValueEqual(slots.get(2).getCount(), 1024, "the empty slot takes a full stack");
        h.assertValueEqual(left.getCount(), 2100 - 964 - 1024, "and what is left does not fit anywhere");
        h.assertValueEqual(slots.get(1).getCount(), 64, "dirt is left alone");

        List<ItemStack> torches = new ArrayList<>(List.of(new ItemStack(Items.TORCH, 1000), ItemStack.EMPTY));
        h.assertTrue(BackpackContents.insert(torches, new ItemStack(Items.TORCH, 100)).isEmpty(), "100 more torches fit");
        h.assertValueEqual(torches.get(0).getCount(), 1024, "the first stack goes to 1,024");
        h.assertValueEqual(torches.get(1).getCount(), 76, "and the rest goes in the next slot");
        List<ItemStack> two = new ArrayList<>(List.of(new ItemStack(Items.TORCH, 1000), ItemStack.EMPTY));
        ItemStack tooMany = BackpackContents.insert(two, new ItemStack(Items.TORCH, 2000));
        h.assertValueEqual(two.get(0).getCount(), 1024, "the first stack stops at 1,024");
        h.assertValueEqual(two.get(1).getCount(), 1024, "the next slot takes 1,024");
        h.assertValueEqual(tooMany.getCount(), 952, "and 952 of the 2,000 do not fit");
        h.succeed();
    }

    // ---------------------------------------------------------------- the modules with the screen open

    private static BackpackMenu openScreen(ServerPlayer player) {
        Backpacks.open(player, Backpacks.EQUIPPED);
        return (BackpackMenu) player.containerMenu;
    }

    /** How many of the item the open screen shows in the Backpack's slots. */
    private static int onScreen(BackpackMenu menu, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack stack = menu.getSlot(TORCH_SLOT + i).getItem();
            if (stack.is(item)) n += stack.getCount();
        }
        return n;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingPacksWhileTheScreenIsOpenAndItStaysPacked(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_INGOT, 9), new ItemStack(Items.GOLD_INGOT, 3));
        BackpackMenu menu = openScreen(player);
        h.assertTrue(BackpackCompacting.compactNow(player), "it packs with the screen open");
        h.assertValueEqual(onScreen(menu, Items.IRON_BLOCK), 1, "the screen shows the iron block");
        h.assertValueEqual(onScreen(menu, Items.IRON_INGOT), 0, "and no ingots");
        h.assertValueEqual(onScreen(menu, Items.GOLD_INGOT), 3, "the gold is as it was");
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "the Backpack has the block saved");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 0, "and no ingots");
        // The screen saves all of its slots on every change: the block must not be put back over by the old ingots
        menu.getSlot(TORCH_SLOT + 20).set(new ItemStack(Items.APPLE));
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "still one iron block after the screen saves again");
        h.assertValueEqual(held(player, Items.IRON_INGOT), 0, "and still no ingots");
        h.assertValueEqual(held(player, Items.APPLE), 1, "with the change that was made on the screen");
        // Closing keeps it all
        player.closeContainer();
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "closed: the block is kept");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void compactingTakesWhatYouJustPutInTheOpenScreen(GameTestHelper h) {
        ServerPlayer player = compactingWith(h, new ItemStack(Items.IRON_INGOT, 5));
        BackpackMenu menu = openScreen(player);
        h.assertTrue(!BackpackCompacting.compactNow(player), "5 ingots do not make a block");
        // Four more put in by hand on the screen
        menu.getSlot(TORCH_SLOT + 7).set(new ItemStack(Items.IRON_INGOT, 4));
        h.assertTrue(BackpackCompacting.compactNow(player), "with the 4 just put in there are 9, and they pack");
        h.assertValueEqual(onScreen(menu, Items.IRON_BLOCK), 1, "an iron block on the screen");
        h.assertValueEqual(onScreen(menu, Items.IRON_INGOT), 0, "all 9 ingots were used, wherever they were");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void feedingEatsFromTheOpenScreenAndItStays(GameTestHelper h) {
        ItemStack backpack = backpackWith(BackpackModuleType.FEEDING);
        fill(backpack, new ItemStack(Items.BREAD, 4));
        ServerPlayer player = wearing(h, backpack);
        player.getFoodData().setFoodLevel(5);
        BackpackMenu menu = openScreen(player);
        h.assertTrue(BackpackFeeding.feed(player), "it eats with the screen open");
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "bread restores 5");
        h.assertValueEqual(onScreen(menu, Items.BREAD), 3, "the screen shows one bread less");
        h.assertValueEqual(held(player, Items.BREAD), 3, "and the Backpack has 3 saved");
        menu.getSlot(TORCH_SLOT + 20).set(new ItemStack(Items.APPLE));
        h.assertValueEqual(held(player, Items.BREAD), 3, "still 3 after the screen saves again (the eaten one does not come back)");
        h.succeed();
    }

    @GameTest(environment = "nexium:isolated", padding = 40, maxTicks = 200)
    public void autoTorchTakesTheTorchesFromTheOpenScreen(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(CENTER);
        ItemStack backpack = backpackWith(BackpackModuleType.AUTO_TORCH);
        storeTorches(backpack, 64);
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
        h.startSequence()
                .thenExecute(() -> forceChunks(level, origin, 20, true))
                .thenWaitUntil(() -> h.assertTrue(chunksLoaded(level, origin, 20), "waiting for the chunks around the room"))
                .thenExecute(() -> {
                    room(level, origin, -14, 14, -14, 14);
                    BackpackMenu menu = openScreen(player);
                    h.assertTrue(AutoTorch.active(player), "working with the screen open");
                    int placed = AutoTorch.sweepNow(player);
                    forceChunks(level, origin, 20, false);
                    h.assertTrue(placed >= 1, "a sweep placed torches with the screen open: " + placed);
                    h.assertValueEqual(onScreen(menu, Items.TORCH), 64 - placed, "the screen shows the torches that were taken");
                    h.assertValueEqual(held(player, Items.TORCH), 64 - placed, "and the Backpack has them taken in what is saved");
                    menu.getSlot(TORCH_SLOT + 20).set(new ItemStack(Items.APPLE));
                    h.assertValueEqual(held(player, Items.TORCH), 64 - placed, "still, after the screen saves again (they do not come back)");
                })
                .thenSucceed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aBackpackOpenFromTheInventoryDoesNotStopTheWornOne(GameTestHelper h) {
        ItemStack worn = backpackWith(BackpackModuleType.COMPACTING);
        fill(worn, new ItemStack(Items.IRON_INGOT, 9));
        ServerPlayer player = wearing(h, worn);
        ItemStack other = backpackWith();
        fill(other, new ItemStack(Items.GOLD_INGOT, 9));
        player.getInventory().setItem(12, other);
        Backpacks.open(player, 12);
        BackpackMenu menu = (BackpackMenu) player.containerMenu;
        h.assertValueEqual(menu.source(), 12, "the other Backpack is the one open");
        h.assertTrue(BackpackCompacting.compactNow(player), "the worn Backpack still packs");
        h.assertValueEqual(held(player, Items.IRON_BLOCK), 1, "its ingots became a block");
        h.assertValueEqual(onScreen(menu, Items.GOLD_INGOT), 9, "the Backpack on the screen is untouched");
        h.assertValueEqual(onScreen(menu, Items.GOLD_BLOCK), 0, "no block in it");
        h.succeed();
    }
}
