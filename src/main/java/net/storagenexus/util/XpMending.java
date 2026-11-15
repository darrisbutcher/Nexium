package net.storagenexus.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

/**
 * Hands experience to a player the way an experience orb does: before it becomes levels, it repairs. A random damaged item
 * the player carries or wears that has Mending (or anything else with the repair-with-experience effect, the elytra in the
 * wings slot included) is repaired first, at the rate the item's enchantments give (2 durability a point for Mending); points
 * the repair does not use go on to another damaged item, and what is left after that is the player's.
 *
 * <p>The Experience Farm and the Nexus Furnace hand out what they banked through this, so what they hold can mend tools too.
 */
public final class XpMending {
    /** Gives these points as an orb would: repairs first, the rest as experience. Returns how many points were left for the player. */
    public static int give(Player player, int points) {
        if (points <= 0) return 0;
        int left = player instanceof ServerPlayer server ? repair(server, points) : points;
        if (left > 0) player.giveExperiencePoints(left);
        return left;
    }

    /** What an orb of this many points would do to the player's items: returns the points that were not used up. */
    private static int repair(ServerPlayer player, int points) {
        Optional<EnchantedItemInUse> found = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        if (found.isEmpty()) return points;
        ItemStack stack = found.get().itemStack();
        int canRepair = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), stack, points);
        int repaired = Math.min(canRepair, stack.getDamageValue());
        stack.setDamageValue(stack.getDamageValue() - repaired);
        if (repaired <= 0) return 0;
        int rest = points - repaired * points / canRepair;
        return rest > 0 ? repair(player, rest) : 0;
    }

    private XpMending() {}
}
