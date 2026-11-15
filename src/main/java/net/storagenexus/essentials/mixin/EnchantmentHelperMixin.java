package net.storagenexus.essentials.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An elytra in the wings slot counts as worn on the chest for its enchantments: its protection adds to the armor's
 * (vanilla still caps the total), and Mending repairs it. Mending works on any damaged item in the inventory, not only what
 * is held or worn: experience orbs and everything else that repairs with experience pick from all of it.
 */
@Mixin(EnchantmentHelper.class)
abstract class EnchantmentHelperMixin {
    @Inject(method = "runIterationOnEquipment", at = @At("TAIL"))
    private static void nexusessentials$includeWingsSlot(LivingEntity owner, EnchantmentHelper.EnchantmentInSlotVisitor method, CallbackInfo ci) {
        if (owner instanceof Player player) {
            ItemStack wings = ExtraSlots.of(player).wings();
            if (!wings.isEmpty()) EnchantmentHelper.runIterationOnItem(wings, EquipmentSlot.CHEST, owner, method);
        }
    }

    @Inject(method = "getRandomItemWith", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Util;getRandomSafe(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private static void nexusessentials$moreItemsCanBePicked(DataComponentType<?> componentType, LivingEntity source, Predicate<ItemStack> predicate,
                                                             CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir, @Local List<EnchantedItemInUse> items) {
        if (!(source instanceof Player player)) return;
        // The elytra in the wings slot counts as worn on the chest
        nexusessentials$addIfMatching(items, ExtraSlots.of(player).wings(), EquipmentSlot.CHEST, componentType, source, predicate);
        // Mending (and anything else that repairs with experience) works on any damaged item in the inventory, held or not: hotbar,
        // main inventory, and the extra slots. What vanilla already listed (what is held or worn) is not listed twice.
        if (componentType == EnchantmentEffectComponents.REPAIR_WITH_XP) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                nexusessentials$addIfMatching(items, inventory.getItem(i), EquipmentSlot.MAINHAND, componentType, source, predicate);
            }
            var extra = ExtraSlots.of(player);
            for (int i = 0; i < extra.getContainerSize(); i++) {
                nexusessentials$addIfMatching(items, extra.getItem(i), EquipmentSlot.MAINHAND, componentType, source, predicate);
            }
        }
    }

    /** Lists the stack once per enchantment on it that has this effect and works in the slot, unless it is listed already. */
    private static void nexusessentials$addIfMatching(List<EnchantedItemInUse> items, ItemStack stack, EquipmentSlot slot, DataComponentType<?> componentType,
                                                      LivingEntity source, Predicate<ItemStack> predicate) {
        if (stack.isEmpty() || !predicate.test(stack)) return;
        for (EnchantedItemInUse listed : items) if (listed.itemStack() == stack) return;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            Enchantment enchantment = entry.getKey().value();
            if (enchantment.effects().has(componentType) && enchantment.matchingSlot(slot)) {
                items.add(new EnchantedItemInUse(stack, slot, source));
            }
        }
    }
}
