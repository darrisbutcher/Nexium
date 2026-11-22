package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.registry.ModBlocks;
import net.nexium.terminal.TerminalMenu;

/**
 * Clicking the crafting terminal's result slot quickly crafts every time. Two clicks on a slot within a quarter of a second are a
 * double-click, which the game turns into a "pick up all" that crafts nothing, unless the result slot is left out of it (as in vanilla's
 * crafting table). The real mouse clicks the result many times with only a tick or three between clicks, and each one must craft.
 */
public class TerminalRapidCraftClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition().offset(0, 0, 3);
                level.setBlockAndUpdate(base, ModBlocks.CRAFTING_TERMINAL.defaultBlockState());
                level.setBlockAndUpdate(base.east(), ModBlocks.STORAGE_CONNECTOR.defaultBlockState());
                level.setBlockAndUpdate(base.east(2), Blocks.CHEST.defaultBlockState());
                ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(base.east(2));
                for (int i = 0; i < 27; i++) chest.setItem(i, new ItemStack(Items.OAK_PLANKS, 64));
                player.getInventory().clearContent();
            });
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                BlockPos pos = mc.player.blockPosition().offset(0, 0, 3);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            ctx.waitFor(mc -> mc.gui.screen() instanceof AbstractContainerScreen<?> s && s.getMenu() instanceof TerminalMenu);
            ctx.waitTicks(40);
            // Planks in the grid for sticks (4 sticks a craft), refilled from the network after every craft
            ctx.runOnClient(mc -> net.nexium.client.compat.RecipeTransfer.fill(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.withDefaultNamespace("stick")), false));
            ctx.waitTicks(10);
            double[] result = ctx.computeOnClient(mc -> {
                var screen = (AbstractContainerScreen<?>) mc.gui.screen();
                var slot = ((TerminalMenu) screen.getMenu()).getResultSlot();
                double scale = mc.getWindow().getGuiScale();
                return new double[]{(field(screen, "leftPos") + slot.x + 8) * scale, (field(screen, "topPos") + slot.y + 8) * scale};
            });
            ctx.getInput().setCursorPos(result[0], result[1]);
            ctx.waitTicks(3);
            // Slow clicks, then clicks a few ticks apart, then a tick apart: every one of them crafts 4 sticks
            int clicks = 0;
            for (int gap : new int[]{20, 20, 3, 3, 3, 1, 1, 1}) {
                ctx.getInput().holdMouseFor(1, 1);
                ctx.waitTicks(gap);
                clicks++;
            }
            ctx.waitTicks(10);
            int client = ctx.computeOnClient(mc -> mc.player.containerMenu.getCarried().getCount());
            int server = world.getServer().computeOnServer(sv -> sv.getPlayerList().getPlayers().get(0).containerMenu.getCarried().getCount());
            if (client != 4 * clicks || server != 4 * clicks) {
                throw new AssertionError("Expected: " + clicks + " quick clicks on the result craft " + 4 * clicks + " sticks, the client holds " + client + " and the server " + server);
            }
        }
    }

    private static int field(Object o, String name) {
        try {
            var f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
