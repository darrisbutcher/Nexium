package net.storagenexus.aiot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.nexium.NexiumHaste;
import net.storagenexus.nexium.NexiumMaterials;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.util.Menus;

/**
 * The tool menu (G by default), on the server: opening it for the tool in the main hand, and what its switches do.
 * It is for any AIOT and any Nexium tool that gives Haste; switches that don't apply to the held tool are greyed out.
 */
public final class ToolMenu {
    /** Whether the tool menu has anything to show for this stack. */
    public static boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof AiotItem || stack.is(NexiumMaterials.GRANTS_HASTE);
    }

    /** Opens the tool menu for the tool in the main hand, if it is one the menu is for. */
    public static void open(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        if (!appliesTo(tool)) return;
        int slot = player.getInventory().getSelectedSlot();
        Menus.open(player, tool.getHoverName(), slot, (id, inventory, p) -> new ToolMenuMenu(id, inventory, slot));
    }

    public static void swapSilkTouchFortune(ServerPlayer player, ItemStack stack) {
        MiningMode mode = AiotEnchanting.swap(stack);
        if (mode == null) {
            player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.needs_both").withStyle(ChatFormatting.RED));
            return;
        }
        var enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
        player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.mode",
                Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(stack, mode))));
    }

    public static void toggleHaste(ServerPlayer player, ItemStack stack) {
        if (!stack.is(NexiumMaterials.GRANTS_HASTE)) return;
        boolean nowOff = !stack.has(ModComponents.HASTE_OFF);
        if (nowOff) stack.set(ModComponents.HASTE_OFF, Unit.INSTANCE);
        else stack.remove(ModComponents.HASTE_OFF);
        NexiumHaste.update(player);
        player.sendOverlayMessage(Component.translatable(nowOff ? "message.storagenexus.tool_menu.haste_off"
                : "message.storagenexus.tool_menu.haste_on"));
    }

    /** Steps the Vein Mining module to its next size. Returns false if the tool has none. */
    public static boolean cycleVeinSize(ServerPlayer player, ItemStack stack) {
        int size = AiotModules.cycleVeinSize(stack);
        if (size == 0) return false;
        player.sendOverlayMessage(Component.translatable("message.storagenexus.tool_menu.vein_size", size));
        return true;
    }

    /** Switches the module in this slot on or off. Returns false if the slot is empty. */
    public static boolean toggleModule(ServerPlayer player, ItemStack stack, int slot) {
        ItemStack module = AiotModules.slots(stack).get(slot);
        Boolean nowOn = AiotModules.toggle(stack, slot);
        if (nowOn == null) return false;
        player.sendOverlayMessage(Component.translatable(nowOn ? "message.storagenexus.tool_menu.module_on"
                : "message.storagenexus.tool_menu.module_off", module.getHoverName()));
        return true;
    }

    private ToolMenu() {}
}
