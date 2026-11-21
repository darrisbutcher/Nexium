package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.aiot.AiotEnchanting;
import net.nexium.aiot.AiotTier;
import net.nexium.aiot.MiningMode;
import net.nexium.essentials.grindstone.EnchantedGrindstoneMenu;
import net.nexium.essentials.grindstone.EnchantmentPulling;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.registry.ModComponents;

import java.util.List;

/**
 * The Enchanted Grindstone: it lists the enchantments on an item (tools and armor, enchanted books, an AIOT's inactive Silk Touch or
 * Fortune), and pulling one takes just that one off, onto a new Enchanted Book, using one Book up, for free. The item keeps its wear,
 * name and other enchantments; curses can be pulled; an enchanted book left with none becomes a plain Book; one that holds a single enchantment has
 * nothing to separate.
 */
public class EnchantedGrindstoneGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(POS)));
        return player;
    }

    private static EnchantedGrindstoneMenu menu(GameTestHelper h, ServerPlayer player) {
        return new EnchantedGrindstoneMenu(7, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(POS)));
    }

    private static ItemStack sword(GameTestHelper h) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(enchantment(h, Enchantments.SHARPNESS), 5);
        sword.enchant(enchantment(h, Enchantments.UNBREAKING), 3);
        sword.enchant(enchantment(h, Enchantments.VANISHING_CURSE), 1);
        sword.setDamageValue(300);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
        return sword;
    }

    private static ItemStack book(GameTestHelper h, Object... keysAndLevels) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        for (int i = 0; i < keysAndLevels.length; i += 2) {
            @SuppressWarnings("unchecked") ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) keysAndLevels[i];
            book.enchant(enchantment(h, key), (Integer) keysAndLevels[i + 1]);
        }
        return book;
    }

    private static ItemEnchantments stored(ItemStack stack) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    @GameTest
    public void itListsEveryEnchantmentOnAnItemInAFixedOrder(GameTestHelper h) {
        List<EnchantmentPulling.Entry> entries = EnchantmentPulling.entries(sword(h));
        h.assertTrue(entries.size() == 3, "the sword's three enchantments are listed, got " + entries.size());
        List<String> names = entries.stream().map(e -> e.enchantment().getRegisteredName()).toList();
        h.assertTrue(names.equals(names.stream().sorted().toList()), "in a fixed order, by name: " + names);
        h.assertTrue(entries.stream().anyMatch(e -> e.curse()), "a curse is listed like any other");
        h.assertTrue(EnchantmentPulling.entries(book(h, Enchantments.MENDING, 1, Enchantments.PROTECTION, 4)).size() == 2, "an enchanted book's stored enchantments are listed");
        h.assertTrue(EnchantmentPulling.entries(new ItemStack(Items.DIAMOND_SWORD)).isEmpty(), "an unenchanted item lists none");
        h.assertTrue(EnchantmentPulling.entries(ItemStack.EMPTY).isEmpty(), "and nothing lists none");
        h.succeed();
    }

    @GameTest
    public void pullingTakesOffJustThatEnchantmentAndKeepsTheRest(GameTestHelper h) {
        ItemStack sword = sword(h);
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(sword, enchantment(h, Enchantments.SHARPNESS));
        h.assertTrue(pulled != null, "Sharpness can be pulled off the sword");
        ItemEnchantments left = pulled.source().getEnchantments();
        h.assertTrue(left.size() == 2 && left.getLevel(enchantment(h, Enchantments.SHARPNESS)) == 0, "the sword lost Sharpness: " + left);
        h.assertTrue(left.getLevel(enchantment(h, Enchantments.UNBREAKING)) == 3, "and kept Unbreaking III");
        h.assertTrue(pulled.source().getDamageValue() == 300, "it kept its wear");
        h.assertTrue(pulled.source().getHoverName().getString().equals("Old Faithful"), "and its name");
        h.assertTrue(pulled.book().is(Items.ENCHANTED_BOOK), "the new item is an Enchanted Book");
        ItemEnchantments onBook = stored(pulled.book());
        h.assertTrue(onBook.size() == 1 && onBook.getLevel(enchantment(h, Enchantments.SHARPNESS)) == 5, "holding just Sharpness V: " + onBook);
        h.assertTrue(sword.getEnchantments().size() == 3, "the stack that was pulled from is not changed");
        h.succeed();
    }

    @GameTest
    public void aCurseComesOffLikeAnyOther(GameTestHelper h) {
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(sword(h), enchantment(h, Enchantments.VANISHING_CURSE));
        h.assertTrue(pulled != null, "Curse of Vanishing can be pulled");
        h.assertTrue(pulled.source().getEnchantments().size() == 2, "off the sword");
        h.assertTrue(stored(pulled.book()).getLevel(enchantment(h, Enchantments.VANISHING_CURSE)) == 1, "onto a book of its own");
        h.succeed();
    }

    @GameTest
    public void pullingTheOnlyEnchantmentLeavesAnUnenchantedItem(GameTestHelper h) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.enchant(enchantment(h, Enchantments.EFFICIENCY), 5);
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(pick, enchantment(h, Enchantments.EFFICIENCY));
        h.assertTrue(pulled != null, "the pickaxe's one enchantment can be pulled");
        h.assertFalse(pulled.source().has(DataComponents.ENCHANTMENTS), "it is left with no enchantments component at all");
        h.assertTrue(pulled.source().is(Items.DIAMOND_PICKAXE), "and is still the pickaxe");
        h.succeed();
    }

    @GameTest
    public void anEnchantedBookKeepsTheRestAndOneThatHoldsJustOneHasNothingToSeparate(GameTestHelper h) {
        ItemStack two = book(h, Enchantments.MENDING, 1, Enchantments.PROTECTION, 4);
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(two, enchantment(h, Enchantments.MENDING));
        h.assertTrue(pulled != null, "Mending can be pulled off a book that holds two");
        h.assertTrue(pulled.source().is(Items.ENCHANTED_BOOK) && stored(pulled.source()).size() == 1, "the book keeps the other one");
        h.assertTrue(stored(pulled.source()).getLevel(enchantment(h, Enchantments.PROTECTION)) == 4, "Protection IV");

        ItemStack one = book(h, Enchantments.MENDING, 1);
        h.assertFalse(EnchantmentPulling.pullable(one), "a book that holds one enchantment has nothing to separate");
        h.assertTrue(EnchantmentPulling.pull(one, enchantment(h, Enchantments.MENDING)) == null, "and pulling it does nothing");

        ItemStack named = book(h, Enchantments.MENDING, 1, Enchantments.PROTECTION, 4);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Spellbook"));
        h.assertTrue(EnchantmentPulling.pull(named, enchantment(h, Enchantments.MENDING)).source().getHoverName().getString().equals("Spellbook"), "a book keeps its name when it loses one");
        h.succeed();
    }

    @GameTest
    public void pullingFromAnAiotHandlesItsInactiveEnchantment(GameTestHelper h) {
        ItemStack aiot = AiotGameTest.withSilkTouchAndFortune(h, new ItemStack(net.nexium.registry.ModItems.AIOTS.get(AiotTier.DIAMOND)), MiningMode.FORTUNE);
        h.assertTrue(EnchantmentPulling.entries(aiot).size() == 2, "the inactive Silk Touch is listed with the active Fortune");

        EnchantmentPulling.Pulled inactive = EnchantmentPulling.pull(aiot, enchantment(h, Enchantments.SILK_TOUCH));
        h.assertTrue(inactive != null, "the inactive Silk Touch can be pulled");
        h.assertFalse(inactive.source().has(ModComponents.INACTIVE_ENCHANTMENTS), "the AIOT holds nothing inactive any more");
        h.assertFalse(inactive.source().has(ModComponents.MINING_MODE), "and has no mining mode, as with one of them");
        h.assertTrue(inactive.source().getEnchantments().getLevel(enchantment(h, Enchantments.FORTUNE)) == 3, "Fortune III stays active");
        h.assertTrue(stored(inactive.book()).getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "the book holds Silk Touch I");

        EnchantmentPulling.Pulled active = EnchantmentPulling.pull(aiot, enchantment(h, Enchantments.FORTUNE));
        h.assertTrue(active != null, "the active Fortune can be pulled");
        h.assertTrue(active.source().getEnchantments().getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "the inactive Silk Touch then takes its place as the active one");
        h.assertFalse(active.source().has(ModComponents.INACTIVE_ENCHANTMENTS), "nothing waits inactive");
        h.succeed();
    }

    @GameTest
    public void aToolWithoutSilkTouchOrFortuneIsNotGivenAnEmptyList(GameTestHelper h) {
        // The AIOT tidy-up must not touch ordinary tools (it would write an empty enchantments component on them)
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.enchant(enchantment(h, Enchantments.FORTUNE), 3);
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(pick, enchantment(h, Enchantments.FORTUNE));
        h.assertTrue(pulled != null && !pulled.source().has(DataComponents.ENCHANTMENTS), "an ordinary pickaxe ends with no enchantments component");
        h.assertFalse(pulled.source().has(ModComponents.MINING_MODE), "and no mining mode");
        h.succeed();
    }

    @GameTest
    public void aClickPullsOntoABookAndUsesOneBookForFree(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.experienceLevel = 0;
        EnchantedGrindstoneMenu menu = menu(h, player);
        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(sword(h));
        menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).set(new ItemStack(Items.BOOK, 3));
        h.assertTrue(menu.entries().size() == 3 && menu.hasBook() && menu.hasPullable(), "three enchantments listed, a Book ready");

        boolean ok = menu.clickMenuButton(player, menu.buttonFor(enchantment(h, Enchantments.SHARPNESS)));
        h.assertTrue(ok, "the click is accepted");
        h.assertTrue(menu.entries().size() == 2, "the sword is down to two enchantments");
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).getItem().getCount() == 2, "one Book was used up");
        h.assertTrue(count(player, Items.ENCHANTED_BOOK) == 1, "an Enchanted Book is in the inventory");
        boolean holdsSharpness = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(Items.ENCHANTED_BOOK) && stored(s).getLevel(enchantment(h, Enchantments.SHARPNESS)) == 5 && stored(s).size() == 1) holdsSharpness = true;
        }
        h.assertTrue(holdsSharpness, "holding just Sharpness V");
        h.assertTrue(player.experienceLevel == 0 && player.totalExperience == 0, "it cost no experience");

        h.assertTrue(menu.clickMenuButton(player, menu.buttonFor(enchantment(h, Enchantments.VANISHING_CURSE))), "a curse can be pulled too");
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).getItem().getCount() == 1, "using another Book");
        h.assertTrue(count(player, Items.ENCHANTED_BOOK) == 2, "and giving another book");
        h.succeed();
    }

    @GameTest
    public void aClickDoesNothingWithoutABookOrAnEnchantmentToPull(GameTestHelper h) {
        ServerPlayer player = player(h);
        EnchantedGrindstoneMenu menu = menu(h, player);
        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(sword(h));
        int sharpness = menu.buttonFor(enchantment(h, Enchantments.SHARPNESS));
        h.assertFalse(menu.clickMenuButton(player, sharpness), "with no Book in the slot a click does nothing");
        h.assertTrue(menu.entries().size() == 3 && count(player, Items.ENCHANTED_BOOK) == 0, "and nothing changed");

        menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).set(new ItemStack(Items.BOOK, 2));
        h.assertFalse(menu.clickMenuButton(player, menu.buttonFor(enchantment(h, Enchantments.MENDING))), "an enchantment the item does not hold does nothing");
        h.assertFalse(menu.clickMenuButton(player, -1), "a button number that is no enchantment does nothing");
        h.assertFalse(menu.clickMenuButton(player, 100000), "and neither does a huge one");
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).getItem().getCount() == 2, "no Book was used");

        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(book(h, Enchantments.MENDING, 1));
        h.assertFalse(menu.clickMenuButton(player, menu.buttonFor(enchantment(h, Enchantments.MENDING))), "a book that holds one enchantment has nothing to pull");
        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(ItemStack.EMPTY);
        h.assertFalse(menu.clickMenuButton(player, sharpness), "and with nothing in the item slot there is nothing to pull");
        h.succeed();
    }

    @GameTest
    public void withAFullInventoryTheNewBookDropsAtThePlayer(GameTestHelper h) {
        ServerPlayer player = player(h);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        EnchantedGrindstoneMenu menu = menu(h, player);
        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(sword(h));
        menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).set(new ItemStack(Items.BOOK));
        h.assertTrue(menu.clickMenuButton(player, menu.buttonFor(enchantment(h, Enchantments.SHARPNESS))), "the pull works");
        AABB around = new AABB(player.blockPosition()).inflate(4);
        boolean dropped = !h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(Items.ENCHANTED_BOOK)).isEmpty();
        h.assertTrue(dropped, "with no room, the Enchanted Book drops at the player's feet");
        h.succeed();
    }

    @GameTest
    public void theSlotsTakeOnlyWhatBelongsInThem(GameTestHelper h) {
        ServerPlayer player = player(h);
        EnchantedGrindstoneMenu menu = menu(h, player);
        var input = menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT);
        var books = menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT);
        h.assertTrue(input.mayPlace(sword(h)) && input.mayPlace(book(h, Enchantments.MENDING, 1)), "the item slot takes an enchanted tool or book");
        h.assertFalse(input.mayPlace(new ItemStack(Items.DIAMOND_SWORD)) || input.mayPlace(new ItemStack(Items.BOOK)), "but not an unenchanted item or a plain Book");
        h.assertTrue(input.getMaxStackSize() == 1, "and one item at a time");
        h.assertTrue(books.mayPlace(new ItemStack(Items.BOOK)), "the Book slot takes plain Books");
        h.assertFalse(books.mayPlace(book(h, Enchantments.MENDING, 1)) || books.mayPlace(new ItemStack(Items.PAPER)), "but not an enchanted book or anything else");
        h.succeed();
    }

    @GameTest
    public void shiftClickMovesItemsBetweenTheSlotsAndTheInventory(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.getInventory().setItem(0, sword(h));
        player.getInventory().setItem(1, new ItemStack(Items.BOOK, 5));
        player.getInventory().setItem(2, new ItemStack(Items.STONE, 5));
        EnchantedGrindstoneMenu menu = menu(h, player);
        net.minecraft.world.inventory.Slot swordSlot = null, bookSlot = null, stoneSlot = null;
        for (var slot : menu.slots) {
            if (slot.container != player.getInventory()) continue;
            if (slot.getContainerSlot() == 0) swordSlot = slot;
            if (slot.getContainerSlot() == 1) bookSlot = slot;
            if (slot.getContainerSlot() == 2) stoneSlot = slot;
        }
        menu.clicked(swordSlot.index, 0, ContainerInput.QUICK_MOVE, player);
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).getItem().is(Items.DIAMOND_SWORD), "a shift-click on an enchanted sword puts it in the item slot");
        menu.clicked(bookSlot.index, 0, ContainerInput.QUICK_MOVE, player);
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).getItem().getCount() == 5, "a shift-click on Books puts them in the Book slot");
        menu.clicked(stoneSlot.index, 0, ContainerInput.QUICK_MOVE, player);
        h.assertTrue(player.getInventory().getItem(2).getCount() == 5 || count(player, Items.STONE) == 5, "stone goes nowhere new, and is not lost");
        menu.clicked(EnchantedGrindstoneMenu.INPUT_SLOT, 0, ContainerInput.QUICK_MOVE, player);
        h.assertTrue(menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).getItem().isEmpty() && count(player, Items.DIAMOND_SWORD) == 1, "a shift-click on the item slot gives the sword back");
        h.succeed();
    }

    @GameTest
    public void closingGivesBackWhatIsInTheSlots(GameTestHelper h) {
        ServerPlayer player = player(h);
        EnchantedGrindstoneMenu menu = menu(h, player);
        menu.getSlot(EnchantedGrindstoneMenu.INPUT_SLOT).set(sword(h));
        menu.getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).set(new ItemStack(Items.BOOK, 4));
        menu.removed(player);
        h.assertTrue(count(player, Items.DIAMOND_SWORD) == 1 && count(player, Items.BOOK) == 4, "the sword and the four Books came back to the inventory");
        h.succeed();
    }

    @GameTest
    public void aRightClickOpensTheMenuInsteadOfTheVanillaGrindstone(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos pos = h.absolutePos(POS);
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.ENCHANTED_GRINDSTONE.defaultBlockState());
        BlockState state = h.getLevel().getBlockState(pos);
        h.assertTrue(state.getBlock() instanceof GrindstoneBlock, "it is a grindstone (same shape, same ways of being placed)");
        state.useWithoutItem(h.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        h.assertTrue(player.containerMenu instanceof EnchantedGrindstoneMenu, "a right-click opens the Enchanted Grindstone's menu, got " + player.containerMenu.getClass().getSimpleName());
        h.assertTrue(player.containerMenu.stillValid(player), "which stays valid while the block is there and the player is near");
        h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        h.assertFalse(player.containerMenu.stillValid(player), "and not once the block is gone");
        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "a pickaxe mines it");
        h.assertTrue(state.requiresCorrectToolForDrops(), "and it needs one to drop");
        h.succeed();
    }

    @GameTest
    public void aGrindstoneABookAndLapisMakeIt(GameTestHelper h) {
        CraftingInput input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.GRINDSTONE), new ItemStack(Items.BOOK), new ItemStack(Items.LAPIS_LAZULI),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        boolean makes = false;
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(input, h.getLevel()) && recipe.assemble(input).is(ModItems.ENCHANTED_GRINDSTONE)) makes = true;
        }
        h.assertTrue(makes, "a Grindstone, a Book and Lapis Lazuli, in any place, make an Enchanted Grindstone");
        CraftingInput noLapis = CraftingInput.of(3, 3, List.of(new ItemStack(Items.GRINDSTONE), new ItemStack(Items.BOOK), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        for (RecipeHolder<?> holder : h.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof CraftingRecipe recipe && recipe.matches(noLapis, h.getLevel())) {
                h.assertFalse(recipe.assemble(noLapis).is(ModItems.ENCHANTED_GRINDSTONE), "not without the Lapis Lazuli");
            }
        }
        h.succeed();
    }
}
