package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.nexium.client.Tooltips;
import net.nexium.client.screen.BackpackScreen;
import net.nexium.essentials.backpack.BackpackContents;
import net.nexium.essentials.backpack.BackpackFeeding;
import net.nexium.essentials.backpack.BackpackModuleType;
import net.nexium.essentials.backpack.BackpackModules;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.slots.ExtraSlots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tooltips in a real game: no Nexium item's tooltip has a line wider than the limit (the first line, the name, aside) or shows a line
 * twice (the Backpack modules showed their description twice); the Backpack's crafting panel is reported to JEI as an area its item list
 * keeps out of (when JEI is installed: {@code -PwithJei}); and the Feeding module's tooltip, which has the longest description, is
 * shown wrapped. Takes a screenshot of it.
 */
public class TooltipsClientTest implements FabricClientGameTest {
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
                ExtraSlots.of(player).clearContent();
                ItemStack worn = new ItemStack(ModItems.BACKPACK);
                List<ItemStack> modules = new ArrayList<>();
                modules.add(configuredFeeding());
                modules.add(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.CRAFTING)));
                for (int i = modules.size(); i < BackpackModules.SLOTS; i++) modules.add(ItemStack.EMPTY);
                BackpackModules.setSlots(worn, modules);
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < BackpackContents.SIZE; i++) contents.add(ItemStack.EMPTY);
                contents.set(0, new ItemStack(Items.OAK_PLANKS, 14));
                BackpackContents.write(worn, contents);
                ExtraSlots.of(player).setItem(ExtraSlots.BACKPACK, worn);
            });
            ctx.waitTicks(20);

            // Every Nexium item's tooltip: narrow, and no line twice
            List<String> problems = ctx.computeOnClient(TooltipsClientTest::audit);
            check(problems.isEmpty(), "every Nexium tooltip is narrow and has no repeated line:\n" + String.join("\n", problems));

            // The Backpack's crafting panel, and what JEI is told about it
            ctx.getInput().pressKey(NexiumEssentialsClient.OPEN_BACKPACK);
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen), "B opens the Backpack's screen");
            int[] panel = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).craftingPanelBounds());
            check(panel != null, "with a Crafting module on, the screen reports the crafting panel's area");
            int mainRight = ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).slotCentre(((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0))[0]);
            check(panel[0] > mainRight && panel[2] > 0 && panel[3] > 0, "it is to the right of the first slot, with a size: " + java.util.Arrays.toString(panel));
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
                System.out.println("[nexus] JEI is not installed in this run (-PwithJei to check it)");
            } else {
                List<int[]> jei = ctx.computeOnClient(JeiCheck::exclusionAreas);
                check(jei != null && jei.stream().anyMatch(a -> java.util.Arrays.equals(a, panel)), "JEI keeps its item list out of the crafting panel: JEI says "
                        + (jei == null ? "nothing (no runtime)" : jei.stream().map(java.util.Arrays::toString).toList()) + ", the panel is " + java.util.Arrays.toString(panel));
                // With the panel gone (the module taken out) it is no longer reserved
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player.containerMenu instanceof net.nexium.essentials.backpack.BackpackMenu menu) menu.getSlot(1).set(ItemStack.EMPTY);
                });
                ctx.waitTicks(10);
                List<int[]> after = ctx.computeOnClient(JeiCheck::exclusionAreas);
                check(after != null && after.stream().noneMatch(a -> java.util.Arrays.equals(a, panel)), "and not once the Crafting module is out");
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player.containerMenu instanceof net.nexium.essentials.backpack.BackpackMenu menu) menu.getSlot(1).set(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.CRAFTING)));
                });
                ctx.waitTicks(10);
            }

            // The Feeding module's tooltip, wrapped
            RealMouse.moveTo(ctx, ctx.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).slotCentre(((BackpackScreen) mc.gui.screen()).getMenu().getSlot(0))));
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(true); });
            ctx.waitTicks(2);
            System.out.println("[nexus] feeding module tooltip: " + ctx.takeScreenshot("nexus_tooltip_feeding_module"));
            ctx.setScreen(() -> null);
        }
    }

    /** A Feeding module set to eat at 12 and allowed to waste, so its tooltip has every line it can have. */
    private static ItemStack configuredFeeding() {
        return BackpackFeeding.withWaste(BackpackFeeding.withLevel(new ItemStack(ModItems.BACKPACK_MODULES.get(BackpackModuleType.FEEDING)), 12), true);
    }

    /** One line per problem across every Nexium item, plain and in the states that add tooltip lines. */
    private static List<String> audit(Minecraft mc) {
        List<String> problems = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("nexium")) stacks.add(new ItemStack(item));
        }
        // Items whose tooltip changes with what they hold or how they are set
        net.minecraft.core.GlobalPos link = new net.minecraft.core.GlobalPos(mc.level.dimension(), new net.minecraft.core.BlockPos(10, 64, 10));
        stacks.add(configuredFeeding());
        ItemStack tablet = new ItemStack(net.nexium.registry.ModItems.CRAFTING_TABLET);
        tablet.set(net.nexium.registry.ModComponents.TERMINAL_LINK, link);
        stacks.add(tablet);
        ItemStack card = new ItemStack(net.nexium.registry.ModItems.LINK_CARD);
        card.set(net.nexium.registry.ModComponents.LINK_NAME, "Home");
        stacks.add(card);
        ItemStack chest = new ItemStack(net.nexium.registry.ModItems.NEXIUM_CHESTPLATE);
        chest.set(net.nexium.registry.ModComponents.TERMINAL_LINK, link);
        stacks.add(chest);
        for (var module : net.nexium.registry.ModItems.MODULES.values()) {
            ItemStack linked = new ItemStack(module);
            linked.set(net.nexium.registry.ModComponents.TERMINAL_LINK, link);
            linked.set(net.nexium.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
            stacks.add(linked);
        }
        for (BackpackModuleType type : BackpackModuleType.values()) {
            ItemStack off = new ItemStack(ModItems.BACKPACK_MODULES.get(type));
            off.set(net.nexium.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
            stacks.add(off);
        }
        stacks.add(net.nexium.essentials.capture.MobCapture.toItem(new net.nexium.essentials.capture.CapturedMob(
                net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.nbt.CompoundTag(), java.util.Optional.empty(), 12.0F, 20.0F, true)));

        for (ItemStack stack : stacks) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL);
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < lines.size(); i++) {
                String text = lines.get(i).getString();
                int width = mc.font.width(lines.get(i));
                if (i > 0 && width > Tooltips.MAX_WIDTH) problems.add(id + ", line " + i + " is " + width + " wide: " + text);
                if (!text.isBlank() && !seen.add(text)) problems.add(id + " shows this line twice: " + text);
            }
        }
        return problems;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
