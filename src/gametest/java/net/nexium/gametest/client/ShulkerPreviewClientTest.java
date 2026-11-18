package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * A shulker box with something in it shows a grid of its contents under its name, and none of the item list vanilla prints
 * ("Iron Ingot x64 ... and 3 more"); an empty shulker box and other containers (a chest carrying items) keep their normal
 * tooltip. Takes screenshots of the previews, a plain box and a red one.
 */
public class ShulkerPreviewClientTest implements FabricClientGameTest {
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
                List<ItemStack> stuff = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.APPLE, 7),
                        new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.TORCH, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.GOLDEN_CARROT, 12),
                        new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.BREAD, 3)));
                player.getInventory().setItem(0, withContents(Items.SHULKER_BOX, stuff));
                player.getInventory().setItem(1, withContents(Items.DYED_SHULKER_BOX.red(), List.of(new ItemStack(Items.EMERALD, 5), new ItemStack(Items.NETHERITE_INGOT, 2))));
                player.getInventory().setItem(2, new ItemStack(Items.DYED_SHULKER_BOX.blue()));
                player.getInventory().setItem(3, withContents(Items.CHEST, List.of(new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.GOLD_INGOT, 9))));
            });
            ctx.waitTicks(5);
            ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            ctx.waitTicks(5);

            // A full box: the grid, and no item list
            check(hasPreview(ctx, 0), "a shulker box with items has a preview grid");
            List<String> keys = containerKeys(ctx, 0);
            check(keys.isEmpty(), "and none of the vanilla item list lines: " + keys);
            hover(ctx, 0);
            System.out.println("[nexus] shulker preview: " + ctx.takeScreenshot("nexus_shulker_preview"));

            // A coloured one takes the colour
            check(hasPreview(ctx, 1), "a red shulker box has one too");
            check(containerKeys(ctx, 1).isEmpty(), "without the item list");
            hover(ctx, 1);
            System.out.println("[nexus] shulker preview red: " + ctx.takeScreenshot("nexus_shulker_preview_red"));

            // Empty: the normal tooltip, nothing to preview
            check(!hasPreview(ctx, 2), "an empty shulker box has no preview");
            hover(ctx, 2);

            // Not a shulker box: vanilla's list stays, and there is no grid
            check(!hasPreview(ctx, 3), "a chest carrying items has no preview");
            List<String> chest = containerKeys(ctx, 3);
            check(chest.contains("item.container.item_count"), "it keeps vanilla's item list: " + chest);
            hover(ctx, 3);
            System.out.println("[nexus] chest tooltip: " + ctx.takeScreenshot("nexus_chest_tooltip"));
        }
    }

    private static ItemStack withContents(Item item, List<ItemStack> contents) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return stack;
    }

    private static boolean hasPreview(ClientGameTestContext ctx, int slot) {
        return ctx.computeOnClient(mc -> mc.player.getInventory().getItem(slot).getTooltipImage().isPresent());
    }

    /** The translation keys of the tooltip's lines that are vanilla's list of a container's items. */
    private static List<String> containerKeys(ClientGameTestContext ctx, int slot) {
        return ctx.computeOnClient(mc -> {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            return stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL).stream()
                    .map(Component::getContents)
                    .filter(c -> c instanceof TranslatableContents t && t.getKey().startsWith("item.container."))
                    .map(c -> ((TranslatableContents) c).getKey())
                    .toList();
        });
    }

    /** Puts the mouse over a hotbar slot of the inventory screen. */
    private static void hover(ClientGameTestContext ctx, int slot) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = (AbstractContainerScreen<?>) mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(screen.leftPos + 8 + 18 * slot + 8) * scale, (screen.topPos + 142 + 8) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(4);
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
