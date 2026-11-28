package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nexium.energy.ChargingStationBlockEntity;
import net.nexium.energy.NexusFurnaceBlockEntity;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.FarmKind;
import net.nexium.freezer.FreezerBlockEntity;

/** The On/Off button on the screens of a farm, the Freezer and the Nexus Furnace: a real click switches the machine off and back on. Takes screenshots. */
public class MachineSwitchClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[4];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                var level = server.overworld();
                BlockPos base = player.blockPosition();
                at[0] = base.offset(3, 0, 3);
                at[1] = base.offset(5, 0, 3);
                at[2] = base.offset(7, 0, 3);
                at[3] = base.offset(1, 0, 4);
                level.setBlockAndUpdate(at[0], net.nexium.essentials.registry.ModBlocks.FARMS.get(FarmKind.SUGAR_CANE).defaultBlockState());
                level.setBlockAndUpdate(at[1], net.nexium.registry.ModBlocks.FREEZER.defaultBlockState());
                level.setBlockAndUpdate(at[2], net.nexium.registry.ModBlocks.NEXUS_FURNACE.defaultBlockState());
                level.setBlockAndUpdate(at[3], net.nexium.registry.ModBlocks.CHARGING_STATION.defaultBlockState());
            });
            for (int i = 0; i < 4; i++) {
                final int which = i;
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    var be = server.overworld().getBlockEntity(at[which]);
                    if (be instanceof FarmBlockEntity f) player.openMenu(f);
                    else if (be instanceof FreezerBlockEntity f) player.openMenu(f);
                    else if (be instanceof NexusFurnaceBlockEntity f) player.openMenu(f);
                    else if (be instanceof ChargingStationBlockEntity f) player.openMenu(f);
                });
                ctx.waitTicks(15);
                ctx.runOnClient(mc -> mc.gui.toastManager().clear());
                ctx.getInput().setCursorPos(0, 0);
                ctx.waitTicks(3);
                String name = new String[] {"farm", "freezer", "furnace", "station"}[which];
                System.out.println("[nexus] " + name + " screen with the switch: " + ctx.takeScreenshot("nexus_switch_" + name + "_on"));
                double[] centre = ctx.computeOnClient(mc -> {
                    for (var child : mc.gui.screen().children()) {
                        if (child instanceof Button b && (b.getMessage().getString().equals("On") || b.getMessage().getString().equals("Off"))) {
                            double s = mc.getWindow().getGuiScale();
                            return new double[]{(b.getX() + b.getWidth() / 2.0) * s, (b.getY() + b.getHeight() / 2.0) * s};
                        }
                    }
                    throw new AssertionError("no On/Off button on the " + name + " screen");
                });
                ctx.getInput().setCursorPos(centre[0], centre[1]);
                ctx.waitTicks(3);
                ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                ctx.waitTicks(8);
                check(isOff(world, at[which]), "a real click switched the " + name + " off");
                ctx.runOnClient(mc -> mc.gui.toastManager().clear());
                System.out.println("[nexus] " + name + " screen, off: " + ctx.takeScreenshot("nexus_switch_" + name + "_off"));
                ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                ctx.waitTicks(8);
                check(!isOff(world, at[which]), "and another click switched it on again");
                ctx.getInput().pressKey(options -> options.keyInventory);
                ctx.waitTicks(10);
            }
        }
    }

    private static boolean isOff(TestSingleplayerContext world, BlockPos pos) {
        return world.getServer().computeOnServer(server -> {
            var be = server.overworld().getBlockEntity(pos);
            if (be instanceof FarmBlockEntity f) return f.isOff();
            if (be instanceof FreezerBlockEntity f) return f.isOff();
            if (be instanceof ChargingStationBlockEntity f) return f.isOff();
            return ((NexusFurnaceBlockEntity) be).isOff();
        });
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
