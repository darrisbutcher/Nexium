package net.storagenexus.essentials.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A Totem of Undying in the totem slot saves a player who has none in a hand. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    /**
     * Vanilla looks at the main hand, then the offhand, for something that gives death protection. Once the offhand
     * has come up empty, hand it the totem slot's stack instead: vanilla then uses it up exactly as if it were held.
     */
    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack nexusessentials$totemSlot(LivingEntity self, InteractionHand hand, Operation<ItemStack> original) {
        ItemStack inHand = original.call(self, hand);
        if (hand == InteractionHand.OFF_HAND && self instanceof Player player && !ExtraSlots.isTotem(inHand)) {
            ItemStack totem = ExtraSlots.of(player).totem();
            if (ExtraSlots.isTotem(totem)) return totem;
        }
        return inHand;
    }
}
