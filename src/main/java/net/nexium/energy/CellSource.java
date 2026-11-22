package net.nexium.energy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.slots.ExtraSlots;

/** Where the item whose cell screen is open sits: the number goes in the opening data. */
public final class CellSource {
    public static final int MAIN_HAND = 0, OFF_HAND = 1, WINGS_SLOT = 2, CHEST_SLOT = 3;

    /** The stack at this source on this player (the real one, not a copy), or empty. */
    public static ItemStack find(Player player, int source) {
        return switch (source) {
            case MAIN_HAND -> player.getItemInHand(InteractionHand.MAIN_HAND);
            case OFF_HAND -> player.getItemInHand(InteractionHand.OFF_HAND);
            case WINGS_SLOT -> ExtraSlots.of(player).wings();
            case CHEST_SLOT -> player.getItemBySlot(EquipmentSlot.CHEST);
            default -> ItemStack.EMPTY;
        };
    }

    public static int of(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
    }

    private CellSource() {}
}
