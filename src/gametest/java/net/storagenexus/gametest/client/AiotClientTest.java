package net.storagenexus.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.MiningMode;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.client.screen.ToolMenuScreen;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.List;

/**
 * The AIOTs and Nexium gear on the hotbar and in the inventory, for a visual check of their sprites (a diamond AIOT
 * with a green Fortune gem and one with a blue Silk Touch gem), the tooltip of a diamond AIOT holding both, the tool
 * menu (opened with its key, switching to Silk Touch), a Nexium pickaxe's Haste II and the Nexium armor worn.
 */
public class AiotClientTest implements FabricClientGameTest {
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
                var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                for (AiotTier tier : AiotTier.values()) {
                    ItemStack stack = new ItemStack(ModItems.AIOTS.get(tier));
                    if (tier == AiotTier.DIAMOND) {
                        stack.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
                        stack.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
                        AiotEnchanting.normalize(stack, MiningMode.FORTUNE); // green gem
                        ItemStack silk = stack.copy();
                        AiotEnchanting.normalize(silk, MiningMode.SILK_TOUCH); // blue gem
                        player.getInventory().setItem(SILK_SLOT, silk);
                    }
                    player.getInventory().setItem(tier.ordinal(), stack);
                }
                player.getInventory().setItem(NEXIUM_PICKAXE_SLOT, new ItemStack(ModItems.NEXIUM_PICKAXE));
                player.getInventory().setItem(8, new ItemStack(ModItems.NEXIUM_SPEAR));
                for (int i = 0; i < 6; i++) player.getInventory().setItem(9 + i, new ItemStack(List.of(ModItems.NEXIUM_INGOT,
                        ModItems.NEXIUM_BLOCK, ModItems.NEXIUM_SWORD, ModItems.NEXIUM_AXE, ModItems.NEXIUM_SHOVEL, ModItems.NEXIUM_HOE).get(i)));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NEXIUM_HELMET));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NEXIUM_CHESTPLATE));
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NEXIUM_LEGGINGS));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NEXIUM_BOOTS));
            });
            ctx.waitTicks(20);
            // Hold the diamond AIOT, the way a player picks a hotbar slot
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[AiotTier.DIAMOND.ordinal()]);
            ctx.waitTicks(20); // long enough for the hand to finish raising the new item

            List<String> tooltip = ctx.computeOnClient(mc -> mc.player.getInventory().getItem(AiotTier.DIAMOND.ordinal())
                    .getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
                    .stream().map(Component::getString).toList());
            System.out.println("[nexus] diamond AIOT tooltip: " + tooltip);
            check(tooltip.contains("Diamond AIOT"), "the name is Diamond AIOT: " + tooltip);
            check(tooltip.contains("Fortune III"), "Fortune III is listed: " + tooltip);
            check(tooltip.contains("Silk Touch (inactive)"), "the inactive Silk Touch is listed: " + tooltip);
            check(tooltip.contains("Pickaxe, axe, shovel, hoe and sword in one"), "the description is shown: " + tooltip);
            check(tooltip.stream().anyMatch(line -> line.contains("9 Attack Damage")), "9 attack damage, like a diamond axe: " + tooltip);
            check(tooltip.stream().anyMatch(line -> line.contains("1.6 Attack Speed")), "1.6 attack speed, like a sword: " + tooltip);

            System.out.println("[nexus] AIOT in hand: " + ctx.takeScreenshot("nexus_aiot_in_hand"));
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            System.out.println("[nexus] AIOTs in the inventory: " + ctx.takeScreenshot("nexus_aiot_inventory"));
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // The tool menu opens with its key while holding the AIOT, and switches it to Silk Touch
            ctx.getInput().pressKey(options -> ToolMenuClient.TOOL_MENU);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof ToolMenuScreen), "G opens the tool menu");
            System.out.println("[nexus] tool menu: " + ctx.takeScreenshot("nexus_tool_menu"));
            // Click the Mining button, the menu's first widget
            double[] button = ctx.computeOnClient(mc -> {
                var widget = (AbstractWidget) mc.gui.screen().children().getFirst();
                double scale = mc.getWindow().getGuiScale();
                return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
            });
            ctx.getInput().setCursorPos(button[0], button[1]);
            ctx.waitTicks(2);
            ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.player.getMainHandItem().get(ModComponents.MINING_MODE) == MiningMode.SILK_TOUCH),
                    "the menu switched the AIOT to Silk Touch");
            System.out.println("[nexus] tool menu after switching: " + ctx.takeScreenshot("nexus_tool_menu_silk_touch"));
            ctx.getInput().pressKey(options -> ToolMenuClient.TOOL_MENU);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "the same key closes it");

            // A Nexium pickaxe gives Haste II (its icon shows at the top right)
            ctx.getInput().pressKey(options -> options.keyHotbarSlots[NEXIUM_PICKAXE_SLOT]);
            ctx.waitTicks(20);
            check(ctx.computeOnClient(mc -> mc.player.hasEffect(MobEffects.HASTE)), "holding the Nexium pickaxe gives Haste II");
            System.out.println("[nexus] Nexium pickaxe with Haste II: " + ctx.takeScreenshot("nexus_nexium_haste"));

            // The Nexium armor, worn, seen from the front
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            ctx.waitTicks(10);
            System.out.println("[nexus] Nexium armor: " + ctx.takeScreenshot("nexus_nexium_armor"));
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        }
    }

    /** Hotbar slots besides the six AIOTs (slots 0 to 5). */
    private static final int SILK_SLOT = 6, NEXIUM_PICKAXE_SLOT = 7;

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
