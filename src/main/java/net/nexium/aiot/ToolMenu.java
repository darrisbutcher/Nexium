package net.nexium.aiot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.nexium.aiot.module.AiotModules;
import net.nexium.aiot.module.ModuleItem;
import net.nexium.aiot.module.ModuleType;
import net.nexium.gear.NexiumHaste;
import net.nexium.gear.NexiumMaterials;
import net.nexium.registry.ModComponents;
import net.nexium.util.Menus;

/**
 * The tool menu (G by default), on the server: opening it for the tool in the main hand, and what its switches do.
 * It is for any AIOT and any Nexium tool that gives Haste (and opens the Construction Builder's own menu); switches that don't apply to the held tool are greyed out.
 */
public final class ToolMenu {
    /** Whether the tool menu has anything to show for this stack. */
    public static boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof AiotItem || stack.getItem() instanceof net.nexium.builder.BuilderItem
                || stack.is(NexiumMaterials.GRANTS_HASTE);
    }

    /** Opens the tool menu for the tool in the main hand, if it is one the menu is for. */
    public static void open(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        if (!appliesTo(tool)) return;
        int slot = player.getInventory().getSelectedSlot();
        if (tool.getItem() instanceof net.nexium.builder.BuilderItem builder) {
            // The Construction Builder has its own menu
            // A short title that fits the panel ("Nexium Builder"), unless it was renamed
            Component title = tool.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? tool.getHoverName()
                    : Component.translatable("screen.nexium.builder.title." + builder.tier().id());
            Menus.open(player, title, slot, (id, inventory, p) -> new net.nexium.builder.BuilderMenu(id, inventory, slot));
            return;
        }
        Menus.open(player, tool.getHoverName(), slot, (id, inventory, p) -> new ToolMenuMenu(id, inventory, slot));
    }

    public static void swapSilkTouchFortune(ServerPlayer player, ItemStack stack) {
        MiningMode mode = AiotEnchanting.swap(stack);
        if (mode == null) {
            player.sendOverlayMessage(Component.translatable("message.nexium.tool_menu.needs_both").withStyle(ChatFormatting.RED));
            return;
        }
        var enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
        player.sendOverlayMessage(Component.translatable("message.nexium.tool_menu.mode",
                Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(stack, mode))));
    }

    public static void toggleHaste(ServerPlayer player, ItemStack stack) {
        if (!stack.is(NexiumMaterials.GRANTS_HASTE)) return;
        boolean nowOff = !stack.has(ModComponents.HASTE_OFF);
        if (nowOff) stack.set(ModComponents.HASTE_OFF, Unit.INSTANCE);
        else stack.remove(ModComponents.HASTE_OFF);
        NexiumHaste.update(player);
        player.sendOverlayMessage(Component.translatable(nowOff ? "message.nexium.tool_menu.haste_off"
                : "message.nexium.tool_menu.haste_on"));
    }

    /** Steps the AIOT's vein size to the next one. Returns false if the tool is not an AIOT. */
    public static boolean cycleVeinSize(ServerPlayer player, ItemStack stack) {
        int size = AiotModules.cycleVeinSize(stack);
        if (size == 0) return false;
        player.sendOverlayMessage(Component.translatable("message.nexium.tool_menu.vein_size", size));
        return true;
    }

    /** Steps the AIOT's tunnel length. Returns false if the tool is not an AIOT. */
    public static boolean cycleTunnelLength(ServerPlayer player, ItemStack stack) {
        int length = AiotModules.cycleTunnelLength(stack);
        if (length == 0) return false;
        player.sendOverlayMessage(length == 1 ? Component.translatable("message.nexium.tool_menu.tunnel_off")
                : Component.translatable("message.nexium.tool_menu.tunnel", length));
        return true;
    }

    /** Switches the module in this slot on or off. Returns false if the slot is empty. */
    public static boolean toggleModule(ServerPlayer player, ItemStack stack, int slot) {
        ItemStack module = AiotModules.slots(stack).get(slot);
        Boolean nowOn = AiotModules.toggle(stack, slot);
        if (nowOn == null) return false;
        player.sendOverlayMessage(Component.translatable(nowOn ? "message.nexium.tool_menu.module_on"
                : "message.nexium.tool_menu.module_off", module.getHoverName()));
        return true;
    }

    /**
     * The 3x3 key (and the tool menu's 3x3 button): switches 3x3 mining of the AIOT in the main hand on or off, with a message
     * above the hotbar. Does nothing when the main hand holds something else.
     */
    public static void toggleArea(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AiotItem)) return;
        toggleArea(player, tool);
    }

    /** Switches 3x3 mining of this AIOT on or off. Returns false if it is not an AIOT. */
    public static boolean toggleArea(ServerPlayer player, ItemStack tool) {
        Boolean nowOn = AiotModules.toggleArea(tool);
        if (nowOn == null) return false;
        player.sendOverlayMessage(Component.translatable(nowOn ? "message.nexium.tool_menu.area_on" : "message.nexium.tool_menu.area_off"));
        return true;
    }

    /**
     * The tunnel key held with the scroll wheel: moves the tunnel length of the AIOT in the main hand one step up (a
     * positive direction) or down along off, 2, 3, 8, 16, 24, 32 (up to the server's limit), with a message above the
     * hotbar. It stops at the ends. Does nothing when the main hand holds something else.
     */
    public static void stepTunnel(ServerPlayer player, int direction) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AiotItem)) return;
        int length = AiotModules.stepTunnelLength(tool, direction);
        if (length == 0) return;
        player.sendOverlayMessage(length == 1 ? Component.translatable("message.nexium.tool_menu.tunnel_off")
                : Component.translatable("message.nexium.tool_menu.tunnel", length));
    }

    private ToolMenu() {}
}
