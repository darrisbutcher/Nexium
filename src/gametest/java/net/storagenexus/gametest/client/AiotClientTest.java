package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.List;

/**
 * The five AIOTs on the hotbar and in the inventory, for a visual check of their sprites, and the tooltip of a
 * diamond AIOT with Fortune active and Silk Touch inactive.
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
                        ItemEnchantments.Mutable silk = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        silk.set(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
                        stack.set(ModComponents.INACTIVE_ENCHANTMENTS, silk.toImmutable());
                    }
                    player.getInventory().setItem(tier.ordinal(), stack);
                }
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
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
