package net.storagenexus.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.util.Menus;

/** Opening a Backpack's screen, on the server. */
public final class Backpacks {
    /** The menu's source: the Backpack worn in the backpack slot. Any other source is the inventory slot holding it. */
    public static final int EQUIPPED = -1;

    /** The Open Backpack key: opens the Backpack worn in the backpack slot, or says there is none. */
    public static void openEquipped(ServerPlayer player) {
        if (player.isSpectator()) return;
        if (!ExtraSlots.isBackpack(ExtraSlots.of(player).backpack())) {
            player.sendOverlayMessage(Component.translatable("message.nexusessentials.backpack_missing").withStyle(ChatFormatting.RED));
            return;
        }
        open(player, EQUIPPED);
    }

    /** Opens the Backpack at this source ({@link #EQUIPPED} or an inventory slot), if there is one. */
    public static void open(ServerPlayer player, int source) {
        if (player.isSpectator()) return;
        ItemStack backpack = BackpackMenu.stackAt(player, source);
        if (!(backpack.getItem() instanceof BackpackItem)) return;
        Menus.open(player, backpack.getHoverName(), source, (id, inventory, p) -> new BackpackMenu(id, inventory, source));
    }

    private Backpacks() {}
}
