package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.client.EnchantedGrindstoneScreen;
import net.nexium.essentials.grindstone.EnchantedGrindstoneMenu;
import net.nexium.essentials.grindstone.EnchantmentPulling;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;

import java.util.List;

/**
 * The Enchanted Grindstone in a real game, with the real mouse: the block stands beside a vanilla grindstone and is told apart by
 * its violet wheel; its menu opens on a right-click; an item put in lists its enchantments; the rows are dim and hint at a Book
 * until one is in the Book slot; a click on a row pulls that enchantment onto a book of its own (the item loses just it, one Book is used up); a
 * curse comes off the same way; an enchanted book that holds one enchantment has nothing to separate; a long list scrolls with the
 * mouse wheel; closing the screen gives back what is in the slots. Takes screenshots of each.
 */
public class EnchantedGrindstoneClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                for (int x = -3; x <= 3; x++) for (int z = 1; z <= 6; z++) for (int y = 0; y <= 3; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                player.level().setBlockAndUpdate(base[0].offset(-1, 0, 3), Blocks.GRINDSTONE.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(1, 0, 3), ModBlocks.ENCHANTED_GRINDSTONE.defaultBlockState());

                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(enchantment(server, Enchantments.SHARPNESS), 5);
                sword.enchant(enchantment(server, Enchantments.UNBREAKING), 3);
                sword.enchant(enchantment(server, Enchantments.MENDING), 1);
                sword.enchant(enchantment(server, Enchantments.LOOTING), 3);
                sword.enchant(enchantment(server, Enchantments.VANISHING_CURSE), 1);
                sword.setDamageValue(300);
                player.getInventory().setItem(0, sword);
                player.getInventory().setItem(1, new ItemStack(Items.BOOK, 3));
                ItemStack many = new ItemStack(Items.ENCHANTED_BOOK);
                for (var e : List.of(Enchantments.SHARPNESS, Enchantments.LOOTING, Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.FIRE_ASPECT,
                        Enchantments.KNOCKBACK, Enchantments.SWEEPING_EDGE, Enchantments.EFFICIENCY)) many.enchant(enchantment(server, e), 2);
                player.getInventory().setItem(2, many);
                ItemStack single = new ItemStack(Items.ENCHANTED_BOOK);
                single.enchant(enchantment(server, Enchantments.PROTECTION), 4);
                player.getInventory().setItem(3, single);
                player.getInventory().setItem(4, new ItemStack(ModItems.ENCHANTED_GRINDSTONE));
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(20);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] enchanted grindstone beside a vanilla one: " + ctx.takeScreenshot("nexus_enchanted_grindstone_block"));

            // The item in the inventory
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] enchanted grindstone item in the inventory: " + ctx.takeScreenshot("nexus_enchanted_grindstone_item"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // Open its menu the way a right-click does
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base[0].offset(1, 0, 3);
                var state = player.level().getBlockState(pos);
                state.useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof EnchantedGrindstoneScreen), "a right-click opens the Enchanted Grindstone's screen");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] enchanted grindstone, empty: " + shot(ctx, "nexus_enchanted_grindstone_empty"));

            // The sword goes in the item slot
            RealMouse.leftClick(ctx, inventorySlot(ctx, 0));
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.INPUT_SLOT));
            check(ctx.computeOnClient(mc -> menu(mc).entries().size()) == 5, "the sword's five enchantments are listed");
            // With no Book yet the rows hint at one
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(1)));
            ctx.waitTicks(10);
            System.out.println("[nexus] enchanted grindstone, no Book yet: " + shot(ctx, "nexus_enchanted_grindstone_no_book"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(1)));
            check(held(world, Items.ENCHANTED_BOOK) == 2, "a click with no Book in the slot pulls nothing (only the two books from the start): " + held(world, Items.ENCHANTED_BOOK));

            // Books in the Book slot: now the rows are live
            RealMouse.leftClick(ctx, inventorySlot(ctx, 1));
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.BOOK_SLOT));
            check(ctx.computeOnClient(mc -> menu(mc).hasBook()), "the Books sit in the Book slot");
            int sharpness = ctx.computeOnClient(mc -> indexOf(menu(mc), Enchantments.SHARPNESS));
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(sharpness)));
            ctx.waitTicks(10);
            System.out.println("[nexus] enchanted grindstone, ready: " + shot(ctx, "nexus_enchanted_grindstone_ready"));

            // Pull Sharpness with a real click
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(sharpness)));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> menu(mc).entries().size()) == 4, "the sword lost Sharpness and kept the other four");
            check(ctx.computeOnClient(mc -> menu(mc).entries().stream().noneMatch(e -> e.enchantment().is(Enchantments.SHARPNESS))), "Sharpness is the one that is gone");
            check(ctx.computeOnClient(mc -> menu(mc).inputStack().getDamageValue()) == 300, "and the sword kept its wear");
            check(ctx.computeOnClient(mc -> menu(mc).getSlot(EnchantedGrindstoneMenu.BOOK_SLOT).getItem().getCount()) == 2, "one Book was used up");
            check(bookWith(world, Enchantments.SHARPNESS, 5), "an Enchanted Book holding just Sharpness V is in the inventory");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] enchanted grindstone, after pulling Sharpness: " + shot(ctx, "nexus_enchanted_grindstone_pulled"));

            // A curse comes off too
            int curse = ctx.computeOnClient(mc -> indexOf(menu(mc), Enchantments.VANISHING_CURSE));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(curse)));
            ctx.waitTicks(5);
            check(bookWith(world, Enchantments.VANISHING_CURSE, 1), "Curse of Vanishing came off onto a book of its own");
            check(ctx.computeOnClient(mc -> menu(mc).entries().size()) == 3, "leaving three on the sword");

            // An enchanted book that holds one enchantment has nothing to separate
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.INPUT_SLOT));
            RealMouse.leftClick(ctx, inventorySlot(ctx, 0));
            RealMouse.leftClick(ctx, inventorySlot(ctx, 3));
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.INPUT_SLOT));
            check(ctx.computeOnClient(mc -> !menu(mc).hasPullable()), "a book holding one enchantment has nothing to pull");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] enchanted grindstone, a one-enchantment book: " + shot(ctx, "nexus_enchanted_grindstone_single"));

            // A long list scrolls with the mouse wheel
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.INPUT_SLOT));
            RealMouse.leftClick(ctx, inventorySlot(ctx, 3));
            RealMouse.leftClick(ctx, inventorySlot(ctx, 2));
            RealMouse.leftClick(ctx, menuSlot(ctx, EnchantedGrindstoneMenu.INPUT_SLOT));
            check(ctx.computeOnClient(mc -> menu(mc).entries().size()) == 8, "the long book lists its eight enchantments");
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> screen(mc).rowCentre(2)));
            check(ctx.computeOnClient(mc -> screen(mc).scroll()) == 0, "the list starts at the top");
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> screen(mc).scroll()) == 1, "the mouse wheel scrolls it down a row");
            ctx.getInput().scroll(-1);
            ctx.getInput().scroll(-1);
            ctx.getInput().scroll(-1);
            ctx.getInput().scroll(-1);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> screen(mc).scroll()) == 3, "and stops at the end of it (8 enchantments, 5 rows)");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] enchanted grindstone, a long list scrolled: " + shot(ctx, "nexus_enchanted_grindstone_scrolled"));

            // Closing the screen (Escape, as a player does: it tells the server) gives back what is in the slots
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "Escape closes the screen");
            check(held(world, Items.BOOK) == 1, "the Book left in the slot came back to the inventory: " + held(world, Items.BOOK));
            check(held(world, Items.ENCHANTED_BOOK) == 4, "and so did the long book, next to the one-enchantment book and the two pulled books: " + held(world, Items.ENCHANTED_BOOK));
            check(held(world, Items.DIAMOND_SWORD) == 1, "and the sword");
        }
    }

    private static Holder<Enchantment> enchantment(net.minecraft.server.MinecraftServer server, ResourceKey<Enchantment> key) {
        return server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static EnchantedGrindstoneScreen screen(net.minecraft.client.Minecraft mc) {
        return (EnchantedGrindstoneScreen) mc.gui.screen();
    }

    private static EnchantedGrindstoneMenu menu(net.minecraft.client.Minecraft mc) {
        return screen(mc).getMenu();
    }

    private static int indexOf(EnchantedGrindstoneMenu menu, ResourceKey<Enchantment> key) {
        List<EnchantmentPulling.Entry> entries = menu.entries();
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).enchantment().is(key)) return i;
        return -1;
    }

    /** The screen position of one of the menu's own slots (the item or the Book slot). */
    private static int[] menuSlot(ClientGameTestContext ctx, int index) {
        return ctx.computeOnClient(mc -> screen(mc).slotCentre(menu(mc).getSlot(index)));
    }

    /** The screen position of the player's inventory slot {@code n} (0 to 8 are the hotbar). */
    private static int[] inventorySlot(ClientGameTestContext ctx, int n) {
        return ctx.computeOnClient(mc -> {
            for (Slot slot : menu(mc).slots) if (slot.container instanceof Inventory && slot.getContainerSlot() == n) return screen(mc).slotCentre(slot);
            throw new AssertionError("no inventory slot " + n);
        });
    }

    private static int held(TestSingleplayerContext world, net.minecraft.world.item.Item item) {
        return world.getServer().computeOnServer(server -> {
            int n = 0;
            Inventory inv = server.getPlayerList().getPlayers().get(0).getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
            return n;
        });
    }

    /** Whether the inventory holds an Enchanted Book whose only enchantment is {@code key} at {@code level}. */
    private static boolean bookWith(TestSingleplayerContext world, ResourceKey<Enchantment> key, int level) {
        return world.getServer().computeOnServer(server -> {
            Inventory inv = server.getPlayerList().getPlayers().get(0).getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.is(Items.ENCHANTED_BOOK)) continue;
                ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                if (stored.size() == 1 && stored.keySet().iterator().next().is(key) && stored.getLevel(enchantment(server, key)) == level) return true;
            }
            return false;
        });
    }

    /** A screenshot with the recipe toasts cleared away, so they are not in the way. */
    private static java.nio.file.Path shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
        ctx.waitTicks(2);
        return ctx.takeScreenshot(name);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
