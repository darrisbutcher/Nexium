package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.nexium.essentials.assembler.AssemblyTarget;
import net.nexium.essentials.assembler.FarmAssemblerBlockEntity;
import net.nexium.essentials.assembler.FarmAssemblerMenu;
import net.nexium.essentials.client.FarmAssemblerScreen;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.util.Menus;

/**
 * The Farm Assembler's "From network" button with the real mouse: with nothing touching the Assembler the button is greyed out and
 * says why; once a Storage Connector against it (beside a chest holding what the target needs) links it, the button is live, and a
 * click fills the Assembler from the network. Takes screenshots of each.
 */
public class AssemblerNetworkClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            ctx.waitTicks(20);
            BlockPos[] base = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                base[0] = player.blockPosition();
                for (int x = 0; x <= 4; x++) for (int y = 0; y <= 2; y++) player.level().setBlockAndUpdate(base[0].offset(x, y, 3), Blocks.AIR.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(0, 0, 3), Blocks.CHEST.defaultBlockState());
                ChestBlockEntity chest = (ChestBlockEntity) player.level().getBlockEntity(base[0].offset(0, 0, 3));
                chest.setItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
                chest.setItem(1, new ItemStack(Items.EXPERIENCE_BOTTLE, 40));
                chest.setItem(2, new ItemStack(Items.STICK, 30));
                player.level().setBlockAndUpdate(base[0].offset(2, 0, 3), ModBlocks.FARM_ASSEMBLER.defaultBlockState());
                FarmAssemblerBlockEntity assembler = (FarmAssemblerBlockEntity) player.level().getBlockEntity(base[0].offset(2, 0, 3));
                assembler.select(AssemblyTarget.XP_FARM, null);
                Menus.open(player, net.minecraft.network.chat.Component.translatable("block.nexium.farm_assembler"), base[0].offset(2, 0, 3),
                        (id, inventory, p) -> new FarmAssemblerMenu(id, inventory, assembler));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmAssemblerScreen), "the Farm Assembler's screen opens");
            check(!ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().linked()), "with nothing touching it, it is not linked");
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).networkButtonCentre()));
            ctx.waitTicks(10);
            System.out.println("[nexus] assembler, not linked: " + shot(ctx, "nexus_assembler_network_none"));
            // A click on the greyed-out button does nothing
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).networkButtonCentre()));
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().count(0)) == 0, "a click on it while not linked takes nothing");

            // A Storage Connector between the chest and the Assembler links it
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).level()
                    .setBlockAndUpdate(base[0].offset(1, 0, 3), net.nexium.registry.ModBlocks.STORAGE_CONNECTOR.defaultBlockState()));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().linked()), "a Storage Connector against it links it");
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).networkButtonCentre()));
            ctx.waitTicks(10);
            System.out.println("[nexus] assembler, linked: " + shot(ctx, "nexus_assembler_network_linked"));

            // A real click fills it from the network
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).networkButtonCentre()));
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().count(0)) == 64, "the click took the 64 bottles it needs");
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().complete()), "and it is complete");
            int left = world.getServer().computeOnServer(server -> {
                ChestBlockEntity chest = (ChestBlockEntity) server.getPlayerList().getPlayers().get(0).level().getBlockEntity(base[0].offset(0, 0, 3));
                int n = 0;
                for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.EXPERIENCE_BOTTLE)) n += chest.getItem(i).getCount();
                return n;
            });
            check(left == 40, "40 bottles are left in the network: " + left);
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] assembler, filled from the network: " + shot(ctx, "nexus_assembler_network_filled"));
        }
    }

    /** A screenshot with the recipe toasts and chat cleared away. */
    private static java.nio.file.Path shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(true); });
        ctx.waitTicks(2);
        return ctx.takeScreenshot(name);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
