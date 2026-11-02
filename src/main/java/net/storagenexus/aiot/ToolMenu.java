package net.storagenexus.aiot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.nexium.NexiumHaste;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModComponents;

/**
 * What the tool menu (G by default) does, on the server, to the tool in the player's main hand. The tool menu is for
 * any AIOT and any Nexium tool that gives Haste: the switches that don't apply to the held tool are greyed out.
 */
public final class ToolMenu {
    /** Whether the tool menu has anything to show for this stack. */
    public static boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof AiotItem || stack.is(NexiumMaterials.GRANTS_HASTE);
    }

    public static void handle(ServerPlayer player, ToolMenuPayload.Action action) {
        ItemStack stack = player.getMainHandItem();
        switch (action) {
            case SWAP_SILK_TOUCH_FORTUNE -> {
                MiningMode mode = AiotEnchanting.swap(stack);
                if (mode == null) {
                    player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.needs_both").withStyle(ChatFormatting.RED));
                    return;
                }
                var enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
                player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.mode",
                        Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(stack, mode))));
            }
            case TOGGLE_HASTE -> {
                if (!stack.is(NexiumMaterials.GRANTS_HASTE)) return;
                boolean nowOff = !stack.has(ModComponents.HASTE_OFF);
                if (nowOff) stack.set(ModComponents.HASTE_OFF, Unit.INSTANCE);
                else stack.remove(ModComponents.HASTE_OFF);
                NexiumHaste.update(player);
                player.sendOverlayMessage(Component.translatable(nowOff ? "message.storagenexus.tool_menu.haste_off"
                        : "message.storagenexus.tool_menu.haste_on"));
            }
        }
    }

    private ToolMenu() {}
}
