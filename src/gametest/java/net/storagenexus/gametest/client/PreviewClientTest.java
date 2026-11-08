package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.client.AiotHud;
import net.storagenexus.client.AiotPreview;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/**
 * Looking at a stone wall with a diamond AIOT whose 3x3 Mining module has a tunnel of 8: the blocks it would mine
 * (8 layers of 3x3, less the one looked at) are outlined, and a screenshot shows the outline. With a tunnel of 1 it
 * is the plain 3x3, with the Vein Mine key held on dirt the vein is outlined, and a plain pickaxe outlines nothing.
 */
public class PreviewClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                BlockPos base = player.blockPosition();
                // A stone wall starting three blocks ahead (south), 9 deep, with the player looking at its middle
                for (int x = -4; x <= 4; x++) for (int y = -1; y <= 5; y++) for (int z = 3; z <= 12; z++) {
                    player.level().setBlockAndUpdate(base.offset(x, y, z), Blocks.STONE.defaultBlockState());
                }
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0.0F, 0.0F);
                ItemStack aiot = new ItemStack(ModItems.AIOTS.get(AiotTier.DIAMOND));
                var slots = AiotModules.slots(aiot);
                ItemStack area = new ItemStack(ModItems.MODULES.get(ModuleType.AREA_MINING));
                area.set(ModComponents.TUNNEL_LENGTH, 8);
                slots.set(0, area);
                AiotModules.setSlots(aiot, slots);
                player.setItemInHand(InteractionHand.MAIN_HAND, aiot);
            });
            ctx.waitTicks(20);
            int tunnel = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(tunnel == 8 * 9 - 1, "a tunnel of 8 outlines 71 blocks, got " + tunnel);
            String hudTunnel = ctx.computeOnClient(mc -> hud(mc));
            check(hudTunnel.startsWith("3x3 tunnel: 8 deep"), "the HUD reads the tunnel length, got: " + hudTunnel);
            System.out.println("[nexus] tunnel preview: " + ctx.takeScreenshot("nexus_preview_tunnel"));

            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ItemStack aiot = player.getMainHandItem();
                var slots = AiotModules.slots(aiot);
                slots.get(0).remove(ModComponents.TUNNEL_LENGTH);
                AiotModules.setSlots(aiot, slots);
            });
            ctx.waitTicks(10);
            int plain = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            String hudPlain = ctx.computeOnClient(mc -> hud(mc));
            check(hudPlain.startsWith("3x3: on"), "the HUD reads 3x3 on, got: " + hudPlain);
            check(plain == 8, "a plain 3x3 outlines the 8 around the one looked at, got " + plain);
            System.out.println("[nexus] 3x3 preview: " + ctx.takeScreenshot("nexus_preview_3x3"));

            // With the key held on a wall of dirt, the vein is outlined, up to its size
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos base = player.blockPosition();
                for (int x = -4; x <= 4; x++) for (int y = -1; y <= 5; y++) for (int z = 3; z <= 12; z++) {
                    player.level().setBlockAndUpdate(base.offset(x, y, z), Blocks.DIRT.defaultBlockState());
                }
            });
            ctx.getInput().holdKey(ToolMenuClient.VEIN_MINE);
            ctx.waitTicks(10);
            int vein = ctx.computeOnClient(mc -> AiotPreview.outlined().size());
            check(vein == 255, "a vein in a wall of dirt outlines up to the size, 255 more blocks, got " + vein);
            String hudVein = ctx.computeOnClient(mc -> hud(mc));
            check(hudVein.startsWith("3x3: on | Vein: up to 256 (hold V"), "the HUD lists the 3x3 module and the vein size, got: " + hudVein);
            System.out.println("[nexus] vein preview: " + ctx.takeScreenshot("nexus_preview_vein"));
            ctx.getInput().releaseKey(ToolMenuClient.VEIN_MINE);

            // The readout shows for a while (1 second here instead of 20), then hides; the 3x3 key shows it again
            check(ctx.computeOnClient(mc -> AiotHud.visible()), "the readout is up while an AIOT is in use");
            ctx.runOnClient(mc -> {
                net.storagenexus.essentials.config.NexusConfig.get().aiotHudSeconds = 1;
                AiotHud.show();
            });
            ctx.waitTicks(30);
            check(!ctx.computeOnClient(mc -> AiotHud.visible()), "the readout hides after its time");
            check(ctx.computeOnClient(mc -> AiotPreview.outlined().isEmpty()), "and so does the outline");
            ctx.getInput().pressKey(ToolMenuClient.TOGGLE_AREA);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> AiotHud.visible()), "the 3x3 key shows the readout again");
            String hudOff = ctx.computeOnClient(mc -> hud(mc));
            check(hudOff.startsWith("3x3: off"), "and it reads the 3x3 switched off, got: " + hudOff);
            ctx.waitTicks(30);
            check(!ctx.computeOnClient(mc -> AiotHud.visible()), "hidden again");
            ctx.getInput().holdKey(ToolMenuClient.VEIN_MINE);
            ctx.waitTicks(40);
            check(ctx.computeOnClient(mc -> AiotHud.visible()), "the readout stays up while the vein key is held");
            check(!ctx.computeOnClient(mc -> AiotPreview.outlined().isEmpty()), "the vein is outlined while the key is held");
            ctx.getInput().releaseKey(ToolMenuClient.VEIN_MINE);
            ctx.runOnClient(mc -> net.storagenexus.essentials.config.NexusConfig.get().aiotHudSeconds = 20);

            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0)
                    .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE)));
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> AiotPreview.outlined().isEmpty()), "a plain pickaxe outlines nothing");
            check(ctx.computeOnClient(mc -> hud(mc).isEmpty()), "a plain pickaxe shows no HUD");
        }
    }

    private static String hud(net.minecraft.client.Minecraft mc) {
        return AiotHud.lines(mc, mc.player.getMainHandItem()).stream().map(l -> l.text().getString())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
