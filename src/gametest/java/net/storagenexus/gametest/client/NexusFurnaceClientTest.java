package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.client.screen.NexusFurnaceScreen;
import net.storagenexus.energy.NexusFurnaceBlockEntity;
import net.storagenexus.energy.NexusFurnaceMenu;
import net.storagenexus.registry.ModBlocks;

/** The Nexus Furnace's screen in a real game: shift-click shares a stack out, the bars fill as it cooks, the XP button collects. */
public class NexusFurnaceClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            BlockPos[] at = new BlockPos[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                player.getInventory().setItem(9, new ItemStack(Items.RAW_IRON, 64));
                at[0] = player.blockPosition().offset(2, 0, 2);
                player.level().setBlockAndUpdate(at[0], ModBlocks.NEXUS_FURNACE.defaultBlockState());
                var furnace = (NexusFurnaceBlockEntity) player.level().getBlockEntity(at[0]);
                furnace.energy().set(NexusFurnaceBlockEntity.CAPACITY / 2);
                player.openMenu(furnace);
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof NexusFurnaceScreen), "the furnace screen opens");

            // Shift-click the raw iron in the inventory: it is shared out over the eight slots
            ctx.runOnClient(mc -> {
                var menu = mc.player.containerMenu;
                int slot = -1;
                for (var s : menu.slots) if (s.container == mc.player.getInventory() && s.getContainerSlot() == 9) slot = s.index;
                mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);
            });
            ctx.waitTicks(8);
            int[] counts = world.getServer().computeOnServer(server -> {
                var furnace = (NexusFurnaceBlockEntity) server.overworld().getBlockEntity(at[0]);
                int[] c = new int[8];
                for (int i = 0; i < 8; i++) c[i] = furnace.getItem(i).getCount();
                return c;
            });
            for (int i = 0; i < 8; i++) check(counts[i] == 8, "shift-click shared the stack out evenly: slot " + i + " has " + counts[i]);

            // Let it cook for a moment and look at it
            ctx.waitTicks(30);
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());
            ctx.getInput().setCursorPos(0, 0);
            ctx.waitTicks(3);
            System.out.println("[nexus] furnace screen: " + ctx.takeScreenshot("nexus_furnace_screen"));

            // After the first batch there is experience; the button collects it
            ctx.waitTicks(60);
            double banked = world.getServer().computeOnServer(server -> ((NexusFurnaceBlockEntity) server.overworld().getBlockEntity(at[0])).experience());
            check(banked > 4, "experience is banked: " + banked);
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, NexusFurnaceMenu.BTN_COLLECT));
            ctx.waitTicks(5);
            double left = world.getServer().computeOnServer(server -> ((NexusFurnaceBlockEntity) server.overworld().getBlockEntity(at[0])).experience());
            check(left < 57.0, "the button collected the experience, only what cooked since is left (at most one round of eight items): " + left);
            int xp = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).totalExperience);
            check(xp >= 4, "the player has it: " + xp);

            // A bank far past the 16-bit limit of a screen value shows in full, and Collect hands over exactly what it shows
            world.getServer().runOnServer(server -> {
                var furnace = (NexusFurnaceBlockEntity) server.overworld().getBlockEntity(at[0]);
                furnace.energy().set(0); // idle, so the bank holds still
                furnace.takeExperience(server.getPlayerList().getPlayers().get(0));
                furnace.addExperience(40_000.5);
            });
            ctx.waitTicks(10);
            int shown = ctx.computeOnClient(mc -> ((NexusFurnaceMenu) mc.player.containerMenu).experience());
            check(shown == 40_000, "the screen shows all 40,000 points of the bank, not a capped number: " + shown);
            int before = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).totalExperience);
            ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, NexusFurnaceMenu.BTN_COLLECT));
            ctx.waitTicks(5);
            int after = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).totalExperience);
            check(after - before == shown, "Collect handed over exactly the " + shown + " shown: " + (after - before));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
