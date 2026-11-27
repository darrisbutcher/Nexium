package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.autocraft.AutocrafterBlockEntity;
import net.nexium.autocraft.AutocraftPayloads;
import net.nexium.autocraft.Pattern;
import net.nexium.client.screen.AutocraftMonitorScreen;
import net.nexium.client.screen.AutocraftRequestScreen;
import net.nexium.client.screen.AutocrafterScreen;
import net.nexium.client.screen.PatternEncoderScreen;
import net.nexium.client.screen.CraftingTerminalScreen;
import net.nexium.disk.DiskDriveBlockEntity;
import net.nexium.disk.DiskTier;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Autocrafting in a real game: the Pattern Encoder and Autocrafter windows, the Crafting Terminal's Craft button, the request
 * window with its plan, and the Autocrafting Monitor while the request runs. Takes screenshots of each.
 */
public class AutocraftClientTest implements FabricClientGameTest {
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
                for (int x = -3; x <= 5; x++) for (int z = 1; z <= 6; z++) for (int y = 0; y <= 3; y++) {
                    player.level().setBlockAndUpdate(base[0].offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                var level = player.level();
                level.setBlockAndUpdate(base[0].offset(-2, 0, 3), ModBlocks.PATTERN_ENCODER.defaultBlockState());
                level.setBlockAndUpdate(base[0].offset(0, 0, 3), ModBlocks.CRAFTING_TERMINAL.defaultBlockState());
                level.setBlockAndUpdate(base[0].offset(1, 0, 3), ModBlocks.AUTOCRAFTER.defaultBlockState());
                level.setBlockAndUpdate(base[0].offset(2, 0, 3), ModBlocks.DISK_DRIVE.defaultBlockState());
                level.setBlockAndUpdate(base[0].offset(3, 0, 3), ModBlocks.AUTOCRAFT_MONITOR.defaultBlockState());
                AutocrafterBlockEntity crafter = (AutocrafterBlockEntity) level.getBlockEntity(base[0].offset(1, 0, 3));
                crafter.patterns().setItem(0, pattern(grid(Items.OAK_LOG), new ItemStack(Items.OAK_PLANKS, 4)));
                crafter.patterns().setItem(1, pattern(grid(Items.OAK_PLANKS, null, null, Items.OAK_PLANKS), new ItemStack(Items.STICK, 4)));
                crafter.patterns().setItem(2, pattern(grid(Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE, null, Items.COBBLESTONE,
                        Items.COBBLESTONE, Items.COBBLESTONE, Items.COBBLESTONE), new ItemStack(Items.FURNACE)));
                crafter.energy().set(AutocrafterBlockEntity.BUFFER);
                DiskDriveBlockEntity drive = (DiskDriveBlockEntity) level.getBlockEntity(base[0].offset(2, 0, 3));
                drive.disks().setItem(0, new ItemStack(ModItems.STORAGE_DISKS.get(DiskTier.K16)));
                player.getInventory().setItem(0, new ItemStack(ModItems.BLANK_PATTERN, 16));
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(10);
            world.getServer().runOnServer(server -> {
                var network = net.nexium.network.StorageNetwork.find(server.overworld(), base[0].offset(0, 0, 3));
                try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    network.insertItem(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.OAK_LOG), 4, tx);
                    tx.commit();
                }
            });
            ctx.waitTicks(5);
            System.out.println("[nexium] autocraft blocks: " + shot(ctx, "autocraft_blocks"));

            // The Pattern Encoder
            use(world, base[0].offset(-2, 0, 3));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof PatternEncoderScreen), "the Pattern Encoder opens its window");
            System.out.println("[nexium] pattern encoder: " + shot(ctx, "autocraft_encoder"));
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(10);

            // The Autocrafter
            use(world, base[0].offset(1, 0, 3));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AutocrafterScreen), "the Autocrafter opens its window");
            System.out.println("[nexium] autocrafter: " + shot(ctx, "autocraft_autocrafter"));
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(10);

            // The Crafting Terminal's Craft button
            use(world, base[0].offset(0, 0, 3));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof CraftingTerminalScreen), "the Crafting Terminal opens");
            System.out.println("[nexium] terminal with the Craft button: " + shot(ctx, "autocraft_terminal"));
            ctx.runOnClient(mc -> ClientPlayNetworking.send(new AutocraftPayloads.RequestCraftables(BlockPos.ZERO)));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AutocraftRequestScreen), "Craft opens the request window");
            System.out.println("[nexium] request window: " + shot(ctx, "autocraft_request"));

            // Pick the sticks: the plan crafts planks first
            ctx.runOnClient(mc -> ((AutocraftRequestScreen) mc.gui.screen()).select(1));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> ((AutocraftRequestScreen) mc.gui.screen()).planReady()), "four sticks can be made from the logs");
            System.out.println("[nexium] request plan: " + shot(ctx, "autocraft_plan"));

            // Furnace needs cobblestone the network does not have: the plan says what is missing
            ctx.runOnClient(mc -> ((AutocraftRequestScreen) mc.gui.screen()).select(2));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> !((AutocraftRequestScreen) mc.gui.screen()).planReady()), "a furnace cannot be made without cobblestone");
            System.out.println("[nexium] request plan, missing items: " + shot(ctx, "autocraft_missing"));

            // Start the sticks: the Monitor window opens and shows the request
            ctx.runOnClient(mc -> ((AutocraftRequestScreen) mc.gui.screen()).select(1));
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> ((AutocraftRequestScreen) mc.gui.screen()).pressStart());
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof AutocraftMonitorScreen), "Start opens the Monitor");
            ctx.waitTicks(15);
            System.out.println("[nexium] monitor, running: " + shot(ctx, "autocraft_monitor_running"));
            ctx.waitTicks(60);
            check(ctx.computeOnClient(mc -> ((AutocraftMonitorScreen) mc.gui.screen()).jobs().stream().anyMatch(j -> j.finished() && j.status().equals("done"))),
                    "the request finishes");
            System.out.println("[nexium] monitor, done: " + shot(ctx, "autocraft_monitor_done"));
            long sticks = world.getServer().computeOnServer(server -> net.nexium.network.StorageNetwork.find(server.overworld(), base[0].offset(0, 0, 3))
                    .countItem(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.STICK)));
            check(sticks >= 4, "the network holds the sticks: " + sticks);
        }
    }

    private static Item[] grid(Item... items) {
        Item[] out = new Item[9];
        System.arraycopy(items, 0, out, 0, items.length);
        return out;
    }

    private static ItemStack pattern(Item[] grid, ItemStack result) {
        List<ItemStack> inputs = new ArrayList<>();
        for (Item item : grid) inputs.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        ItemStack stack = new ItemStack(ModItems.PATTERN);
        stack.set(ModComponents.PATTERN, new Pattern(false, inputs, List.of(result)));
        return stack;
    }

    private static void use(TestSingleplayerContext world, BlockPos pos) {
        world.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.level().getBlockState(pos).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        });
    }

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
