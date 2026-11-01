package net.storagenexus.essentials.elytra;

import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.slots.ExtraSlots;

/**
 * Elytra accept Protection, Fire Protection, Blast Protection and Projectile Protection from enchanted books
 * at an anvil, and unlike armor they can hold all four at once. Chestplates keep the vanilla one-of-four rule.
 */
public final class ElytraEnchanting {
    public static void init() {
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) ->
                isProtection(enchantment) && appliesTo(target) ? TriState.TRUE : TriState.DEFAULT);
    }

    /** The armor-exclusive set: the four protection enchantments. */
    public static boolean isProtection(Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE);
    }

    public static boolean appliesTo(ItemStack stack) {
        return NexusConfig.get().elytraProtectionEnchantments && ExtraSlots.isWings(stack);
    }

    /** Two protection enchantments that may sit together on this item even though vanilla says they exclude each other. */
    public static boolean mayStack(ItemStack target, Holder<Enchantment> a, Holder<Enchantment> b) {
        return appliesTo(target) && isProtection(a) && isProtection(b) && !a.equals(b);
    }

    private ElytraEnchanting() {}
}
