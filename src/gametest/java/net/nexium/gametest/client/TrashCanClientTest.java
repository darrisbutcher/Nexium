package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.client.TrashCanScreen;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.trash.TrashCanMenu;

/**
 * The Trash Can in a real game, with the real mouse: the block stands under a hopper and beside a cauldron (what it is made from);
 * its screen opens on a right-click; a stack dropped on its slot is destroyed (the slot stays empty, and the stack does not come back
 * when the screen closes); a right click destroys one item; a shift-click moves nothing. Takes screenshots of each.
 */
public class TrashCanClientTest implements FabricClientGameTest {
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
                player.level().setBlockAndUpdate(base[0].offset(-1, 0, 3), Blocks.CAULDRON.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(1, 0, 3), ModBlocks.TRASH_CAN.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(1, 1, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));

                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
                player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
                player.getInventory().setItem(2, new ItemStack(Items.GOLD_INGOT, 12));
                player.getInventory().setItem(3, new ItemStack(ModItems.TRASH_CAN));
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(20);
            System.out.println("[nexus] trash can beside a cauldron, under a hopper: " + shot(ctx, "nexus_trash_can_block"));

            // The item in the inventory
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            System.out.println("[nexus] trash can item in the inventory: " + shot(ctx, "nexus_trash_can_item"));
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(10);

            // Open its screen the way a right-click does
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base[0].offset(1, 0, 3);
                player.level().getBlockState(pos).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof TrashCanScreen), "a right-click opens the Trash Can's screen");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] trash can screen: " + shot(ctx, "nexus_trash_can_screen"));

            // Pick up the diamonds and hold them over the slot
            RealMouse.leftClick(ctx, inventorySlot(ctx, 0));
            check(ctx.computeOnClient(mc -> menu(mc).getCarried().getCount()) == 64, "the cursor carries the 64 diamonds");
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> screen(mc).trashSlotCentre()));
            ctx.waitTicks(5);
            System.out.println("[nexus] trash can, a stack held over the slot: " + shot(ctx, "nexus_trash_can_holding"));

            // Drop them on the slot: gone, and the slot stays empty
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> screen(mc).trashSlotCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> menu(mc).getCarried().isEmpty()), "the whole stack was destroyed");
            check(ctx.computeOnClient(mc -> menu(mc).getSlot(TrashCanMenu.TRASH_SLOT).getItem().isEmpty()), "the slot is empty again");
            check(held(world, Items.DIAMOND) == 0, "the server has no diamonds left: " + held(world, Items.DIAMOND));
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] trash can, after destroying a stack: " + shot(ctx, "nexus_trash_can_destroyed"));

            // A right click destroys one item
            RealMouse.leftClick(ctx, inventorySlot(ctx, 1));
            RealMouse.rightClick(ctx, ctx.computeOnClient(mc -> screen(mc).trashSlotCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> menu(mc).getCarried().getCount()) == 63, "a right click destroyed one cobblestone and kept 63 on the cursor");
            RealMouse.leftClick(ctx, inventorySlot(ctx, 1));
            check(held(world, Items.COBBLESTONE) == 63, "which went back to its slot: " + held(world, Items.COBBLESTONE));

            // A shift-click moves nothing
            ctx.getInput().holdShift();
            RealMouse.leftClick(ctx, inventorySlot(ctx, 2));
            ctx.getInput().releaseShift();
            ctx.waitTicks(5);
            check(held(world, Items.GOLD_INGOT) == 12, "a shift-click did not destroy the gold: " + held(world, Items.GOLD_INGOT));
            check(ctx.computeOnClient(mc -> menu(mc).getSlot(TrashCanMenu.TRASH_SLOT).getItem().isEmpty()), "and the slot is still empty");

            // Closing it (Escape, as a player does: it tells the server) brings nothing back
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "Escape closes the screen");
            check(held(world, Items.DIAMOND) == 0, "the destroyed diamonds did not come back");
        }
    }

    private static TrashCanScreen screen(net.minecraft.client.Minecraft mc) {
        return (TrashCanScreen) mc.gui.screen();
    }

    private static TrashCanMenu menu(net.minecraft.client.Minecraft mc) {
        return screen(mc).getMenu();
    }

    /** The screen position of the player's inventory slot {@code n} (0 to 8 are the hotbar). */
    private static int[] inventorySlot(ClientGameTestContext ctx, int n) {
        return ctx.computeOnClient(mc -> {
            for (Slot slot : menu(mc).slots) {
                if (slot.container instanceof Inventory && slot.getContainerSlot() == n) return screen(mc).slotCentre(slot);
            }
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

    /** A screenshot with the recipe toasts cleared away, so they are not in the way. */
    private static java.nio.file.Path shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> {
            mc.gui.hud.getChat().clearMessages(true);
            mc.gui.toastManager().clear();
        });
        ctx.waitTicks(2);
        return ctx.takeScreenshot(name);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
