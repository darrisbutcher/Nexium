package net.storagenexus.essentials.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A trade is never out of stock: a villager (or wandering trader) never locks one up after a few uses, so every trade can be
 * made as often as you like. Prices still follow demand. Both sides run this, so the trading screen never crosses a trade out.
 */
@Mixin(MerchantOffer.class)
abstract class MerchantOfferMixin {
    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
    private void nexium$neverOutOfStock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
