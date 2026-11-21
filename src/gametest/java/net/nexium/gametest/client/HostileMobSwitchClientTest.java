package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.assembler.AssemblyTarget;
import net.nexium.essentials.assembler.FarmAssemblerBlockEntity;
import net.nexium.essentials.assembler.FarmAssemblerMenu;
import net.nexium.essentials.client.FarmAssemblerScreen;
import net.nexium.essentials.client.HostileMobSwitchScreen;
import net.nexium.essentials.hostile.HostileMobSwitch;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;
import net.nexium.util.Menus;

/**
 * The Hostile Mob Switch in a real game: the block stands in the world and its item shows in the inventory; a right-click opens its
 * screen; a real click on its button turns the world's hostile spawning off and the screen says so, and a second click turns it on
 * again; Escape closes it. The Farm Assembler's list has the switch with its 140 hostile mobs. Takes screenshots of each.
 */
public class HostileMobSwitchClientTest implements FabricClientGameTest {
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
                player.level().setBlockAndUpdate(base[0].offset(-1, 0, 3), ModBlocks.FARM_ASSEMBLER.defaultBlockState());
                player.level().setBlockAndUpdate(base[0].offset(1, 0, 3), ModBlocks.HOSTILE_MOB_SWITCH.defaultBlockState());
                player.getInventory().setItem(0, new ItemStack(ModItems.HOSTILE_MOB_SWITCH));
                player.getInventory().setSelectedSlot(8);
                player.snapTo(base[0].getX() + 0.5, base[0].getY(), base[0].getZ() + 0.5, 0.0F, 25.0F);
            });
            ctx.waitTicks(20);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(true));
            ctx.waitTicks(2);
            System.out.println("[nexus] hostile mob switch beside a farm assembler: " + ctx.takeScreenshot("nexus_hostile_switch_block"));

            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            System.out.println("[nexus] hostile mob switch item in the inventory: " + ctx.takeScreenshot("nexus_hostile_switch_item"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // Right-click opens its screen
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base[0].offset(1, 0, 3);
                player.level().getBlockState(pos).useWithoutItem(player.level(), player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof HostileMobSwitchScreen), "a right-click opens the Hostile Mob Switch's screen");
            check(!ctx.computeOnClient(mc -> ((HostileMobSwitchScreen) mc.gui.screen()).showsBlocking()), "it starts with hostile spawning allowed");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] hostile mob switch screen, spawning: " + shot(ctx, "nexus_hostile_switch_off"));

            // A real click turns hostile spawning off for the world
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((HostileMobSwitchScreen) mc.gui.screen()).buttonCentre()));
            ctx.waitTicks(5);
            check(world.getServer().computeOnServer(server -> HostileMobSwitch.get(server).isBlocking()), "clicking the button turns hostile spawning off in the world");
            check(ctx.computeOnClient(mc -> ((HostileMobSwitchScreen) mc.gui.screen()).showsBlocking()), "and the screen says so");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] hostile mob switch screen, blocked: " + shot(ctx, "nexus_hostile_switch_on"));
            RealMouse.leftClick(ctx, ctx.computeOnClient(mc -> ((HostileMobSwitchScreen) mc.gui.screen()).buttonCentre()));
            ctx.waitTicks(5);
            check(!world.getServer().computeOnServer(server -> HostileMobSwitch.get(server).isBlocking()), "a second click lets them spawn again");
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "Escape closes the screen");

            // The Farm Assembler lists it
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base[0].offset(-1, 0, 3);
                FarmAssemblerBlockEntity assembler = (FarmAssemblerBlockEntity) player.level().getBlockEntity(pos);
                assembler.select(AssemblyTarget.HOSTILE_SWITCH, null);
                assembler.accept(new ItemStack(Items.ZOMBIE_SPAWN_EGG, 64));
                assembler.accept(new ItemStack(Items.CREEPER_SPAWN_EGG, 30));
                Menus.open(player, net.minecraft.network.chat.Component.translatable("block.nexium.farm_assembler"), pos,
                        (id, inventory, p) -> new FarmAssemblerMenu(id, inventory, assembler));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmAssemblerScreen), "the Farm Assembler's screen opens");
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().target() == AssemblyTarget.HOSTILE_SWITCH), "on the Hostile Mob Switch");
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().needed(0)) == 140, "it needs 140 hostile mobs");
            RealMouse.moveTo(ctx, 4, 4);
            System.out.println("[nexus] farm assembler, hostile mob switch: " + shot(ctx, "nexus_hostile_switch_assembler"));
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
        }
    }

    /** A screenshot with the recipe toasts cleared away, so they are not in the way. */
    private static java.nio.file.Path shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(true); });
        ctx.waitTicks(2);
        return ctx.takeScreenshot(name);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
