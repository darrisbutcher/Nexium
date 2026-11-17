package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.drawer.CompactingRecipes;
import net.storagenexus.essentials.backpack.BackpackContents;
import net.storagenexus.essentials.backpack.BackpackMenu;
import net.storagenexus.essentials.backpack.BackpackModuleType;
import net.storagenexus.essentials.backpack.BackpackModules;
import net.storagenexus.essentials.backpack.BackpackRestocking;
import net.storagenexus.essentials.backpack.Backpacks;
import net.storagenexus.essentials.compressed.CompressedBlocks;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Restocking module: a hotbar or offhand stack being used up is topped up to a full stack from the Backpack at 8 (or when
 * the last one goes), only for use and not for a stack moved whole, and when the Backpack has none of the item it unpacks the
 * bigger forms (Triple Compressed stone through Double Compressed and Compressed to stone, an iron block to ingots), keeping
 * what was not needed. Customised items are never unpacked, and it works while the Backpack's screen is open.
 */
public class RestockingGameTest {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(CENTER)));
        return player;
    }

    /** A Backpack with the Restocking module (on) and these stacks in its first slots. */
    private static ItemStack backpack(boolean on, ItemStack... contents) {
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        ItemStack module = new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.RESTOCKING));
        if (!on) module.set(ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
        List<ItemStack> modules = new ArrayList<>(List.of(module));
        while (modules.size() < BackpackModules.SLOTS) modules.add(ItemStack.EMPTY);
        BackpackModules.setSlots(backpack, modules);
        List<ItemStack> slots = new ArrayList<>(List.of(contents));
        while (slots.size() < BackpackContents.SIZE) slots.add(ItemStack.EMPTY);
        BackpackContents.write(backpack, slots);
        return backpack;
    }

    private static void wear(ServerPlayer player, ItemStack backpack) {
        ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, backpack);
    }

    /** Uses this stack up one at a time, as placing blocks does, down to a count, ticking the module every step. */
    private static void useDownTo(ServerPlayer player, int slot, int count) {
        ItemStack stack = player.getInventory().getItem(slot);
        while (stack.getCount() > count) {
            int before = stack.getCount();
            stack.shrink(1);
            BackpackRestocking.tick(player);
            stack = player.getInventory().getItem(slot);
            // Stop once the module has topped it up
            if (stack.isEmpty() || stack.getCount() > before - 1) return;
        }
    }

    private static int held(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    private static int stored(ServerPlayer player, Item item) {
        return BackpackContents.count(ExtraSlots.of(player).backpack(), item);
    }

    private static List<ItemStack> slotsWith(ItemStack... contents) {
        List<ItemStack> slots = new ArrayList<>(List.of(contents));
        while (slots.size() < BackpackContents.SIZE) slots.add(ItemStack.EMPTY);
        return slots;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aStackGettingLowIsToppedUpToAFullStackFromTheBackpack(GameTestHelper h) {
        ServerPlayer player = player(h);
        wear(player, backpack(true, new ItemStack(Items.COBBLESTONE, 300)));
        player.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        BackpackRestocking.tick(player); // the module sees the stack
        useDownTo(player, 2, 9);
        h.assertValueEqual(player.getInventory().getItem(2).getCount(), 9, "9 left: not low enough yet");
        useDownTo(player, 2, 8);
        h.assertValueEqual(player.getInventory().getItem(2).getCount(), 64, "at 8 it is topped up to a full stack of 64");
        h.assertValueEqual(stored(player, Items.COBBLESTONE), 300 - 56, "56 came out of the Backpack");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theLastOneUsedRefillsTheSlotAndTheOffhandIsWatchedToo(GameTestHelper h) {
        ServerPlayer player = player(h);
        wear(player, backpack(true, new ItemStack(Items.ARROW, 100), new ItemStack(Items.TORCH, 200)));
        player.getInventory().setItem(0, new ItemStack(Items.ARROW, 2));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 3));
        BackpackRestocking.tick(player);
        useDownTo(player, 0, 0);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 64, "the last arrow used: the slot is refilled to 64");
        h.assertTrue(player.getInventory().getItem(0).is(Items.ARROW), "with arrows");
        player.getOffhandItem().shrink(1);
        BackpackRestocking.tick(player);
        h.assertValueEqual(player.getOffhandItem().getCount(), 64, "the offhand stack, down to 2 (under 8), is topped up too");
        h.assertValueEqual(stored(player, Items.TORCH), 200 - 62, "from the Backpack's torches");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aStackMovedOrThrownAwayWholeIsLeftAlone(GameTestHelper h) {
        ServerPlayer player = player(h);
        wear(player, backpack(true, new ItemStack(Items.COBBLESTONE, 300)));
        player.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        BackpackRestocking.tick(player);
        player.getInventory().setItem(2, ItemStack.EMPTY); // thrown away whole
        BackpackRestocking.tick(player);
        h.assertTrue(player.getInventory().getItem(2).isEmpty(), "nothing comes back");
        player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 5)); // a small stack put there, not used
        BackpackRestocking.tick(player);
        BackpackRestocking.tick(player);
        h.assertValueEqual(player.getInventory().getItem(3).getCount(), 5, "a small stack that is not being used down is left as it is");
        h.assertValueEqual(stored(player, Items.COBBLESTONE), 300, "and the Backpack gave nothing");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void itDoesNothingWhenOffWithoutTheModuleOrInCreative(GameTestHelper h) {
        ServerPlayer off = player(h);
        wear(off, backpack(false, new ItemStack(Items.COBBLESTONE, 300)));
        off.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        BackpackRestocking.tick(off);
        useDownTo(off, 2, 5);
        h.assertValueEqual(off.getInventory().getItem(2).getCount(), 5, "switched off: no restocking");

        ServerPlayer creative = player(h);
        creative.setGameMode(GameType.CREATIVE);
        wear(creative, backpack(true, new ItemStack(Items.COBBLESTONE, 300)));
        creative.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        BackpackRestocking.tick(creative);
        useDownTo(creative, 2, 5);
        h.assertValueEqual(creative.getInventory().getItem(2).getCount(), 5, "creative mode: no restocking");
        h.assertValueEqual(stored(creative, Items.COBBLESTONE), 300, "and the Backpack is untouched");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void withNoStoneLeftItUnpacksTheTripleCompressedStoneJustEnough(GameTestHelper h) {
        ServerPlayer player = player(h);
        Item triple = CompressedBlocks.item(CompressedBlocks.Base.STONE, 3);
        Item double_ = CompressedBlocks.item(CompressedBlocks.Base.STONE, 2);
        Item compressed = CompressedBlocks.item(CompressedBlocks.Base.STONE, 1);
        wear(player, backpack(true, new ItemStack(triple, 1)));
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 64));
        BackpackRestocking.tick(player);
        useDownTo(player, 0, 8);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 64, "the hotbar stack is back to 64 stone");
        h.assertValueEqual(stored(player, triple), 0, "the Triple Compressed stone was unpacked");
        h.assertValueEqual(stored(player, double_), 8, "into 9 Double Compressed, 1 of which was unpacked: 8 left");
        h.assertValueEqual(stored(player, compressed), 2, "that made 9 Compressed, 7 of which were unpacked (63 stone): 2 left");
        h.assertValueEqual(stored(player, Items.STONE), 7, "and the 7 stone that were not needed stay in the Backpack");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void unpackingTakesOnlyAsMuchAsItNeedsAndKeepsTheRest(GameTestHelper h) {
        ServerLevelHolder.run(h, level -> {
            Item compressed = CompressedBlocks.item(CompressedBlocks.Base.STONE, 1);
            List<ItemStack> slots = slotsWith(new ItemStack(compressed, 5));
            int got = BackpackRestocking.fill(slots, new ItemStack(Items.STONE), 20, item -> CompactingRecipes.packInto(level, item));
            h.assertValueEqual(got, 20, "20 stone wanted, 20 came out");
            h.assertValueEqual(count(slots, compressed), 2, "3 Compressed (27 stone) were unpacked: 2 are left");
            h.assertValueEqual(count(slots, Items.STONE), 7, "and the 7 stone that were not needed stay");
            return null;
        });
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void anIronBlockUnpacksIntoIngotsAndWhatIsThereIsUsedFirst(GameTestHelper h) {
        ServerLevelHolder.run(h, level -> {
            List<ItemStack> slots = slotsWith(new ItemStack(Items.IRON_BLOCK, 2), new ItemStack(Items.IRON_INGOT, 5));
            int got = BackpackRestocking.fill(slots, new ItemStack(Items.IRON_INGOT), 12, item -> CompactingRecipes.packInto(level, item));
            h.assertValueEqual(got, 12, "12 ingots wanted");
            h.assertValueEqual(count(slots, Items.IRON_BLOCK), 1, "the 5 ingots there were used first, then one block unpacked");
            h.assertValueEqual(count(slots, Items.IRON_INGOT), 2, "and the 2 ingots that were not needed stay (9 - 7)");
            return null;
        });
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aPartialRestockTakesWhatThereIsAndNothingIsLostWhenThereIsNoRoom(GameTestHelper h) {
        ServerLevelHolder.run(h, level -> {
            Item compressed = CompressedBlocks.item(CompressedBlocks.Base.STONE, 1);
            List<ItemStack> slots = slotsWith(new ItemStack(compressed, 1));
            int got = BackpackRestocking.fill(slots, new ItemStack(Items.STONE), 50, item -> CompactingRecipes.packInto(level, item));
            h.assertValueEqual(got, 9, "only 9 stone exist in all: that is what comes out");
            h.assertValueEqual(count(slots, compressed), 0, "the Compressed stone is gone");

            // A Backpack with no free slot: the block's own slot is free once it is unpacked, so it still fits
            List<ItemStack> full = new ArrayList<>();
            full.add(new ItemStack(compressed, 1));
            for (int i = 1; i < BackpackContents.SIZE; i++) full.add(new ItemStack(Items.DIRT, 64));
            int ok = BackpackRestocking.fill(full, new ItemStack(Items.STONE), 9, item -> CompactingRecipes.packInto(level, item));
            h.assertValueEqual(ok, 9, "the block's own slot is free once it is unpacked, so it fits");
            return null;
        });
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void customisedItemsAreNeverUnpackedButRestockFromTheirTwins(GameTestHelper h) {
        ServerLevelHolder.run(h, level -> {
            ItemStack named = new ItemStack(Items.STONE);
            named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Special"));
            Item compressed = CompressedBlocks.item(CompressedBlocks.Base.STONE, 1);
            List<ItemStack> slots = slotsWith(new ItemStack(compressed, 3), named.copyWithCount(10));
            int got = BackpackRestocking.fill(slots, named, 30, item -> CompactingRecipes.packInto(level, item));
            h.assertValueEqual(got, 10, "only the 10 named stones, not 27 unpacked plain ones");
            h.assertValueEqual(count(slots, compressed), 3, "the Compressed stone is not touched");
            return null;
        });
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void itWorksThroughTheOpenBackpackScreen(GameTestHelper h) {
        ServerPlayer player = player(h);
        wear(player, backpack(true, new ItemStack(Items.COBBLESTONE, 300)));
        BackpackMenu menu = new BackpackMenu(1, player.getInventory(), Backpacks.EQUIPPED);
        player.containerMenu = menu;
        player.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        BackpackRestocking.tick(player);
        useDownTo(player, 2, 8);
        h.assertValueEqual(player.getInventory().getItem(2).getCount(), 64, "topped up with the screen open");
        h.assertValueEqual(BackpackContents.count(ExtraSlots.of(player).backpack(), Items.COBBLESTONE), 300 - 56, "the Backpack lost 56, as the open screen holds it");
        h.succeed();
    }

    private static int count(List<ItemStack> slots, Item item) {
        int n = 0;
        for (ItemStack s : slots) if (s.is(item)) n += s.getCount();
        return n;
    }

    /** The test's level, for the recipe lookups. */
    private static final class ServerLevelHolder {
        static void run(GameTestHelper h, java.util.function.Function<net.minecraft.server.level.ServerLevel, Void> body) {
            body.apply(h.getLevel());
        }
    }
}
