package net.nexium.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A trade is never out of stock: a villager (or wandering trader) never locks one up after a few uses, so every trade can be
 * made as often as you like. And using a trade never makes it dearer: the price no longer follows demand, which the game raises
 * every time a villager restocks according to how much a trade was used. Both sides run this, so the trading screen never crosses
 * a trade out and shows the same price the server charges.
 */
@Mixin(MerchantOffer.class)
abstract class MerchantOfferMixin {
    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
    private void nexium$neverOutOfStock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /** Demand stops growing: with trades that never run out, uses climb without limit and so would the demand, and the price with it. */
    @Inject(method = "updateDemand", at = @At("HEAD"), cancellable = true)
    private void nexium$demandNeverGrows(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * The price ignores any demand that is there already (a trade saved with a raised price goes back to its base price). Demand only
     * ever raised a price (a lower one made no difference), so it counts as none.
     */
    @ModifyExpressionValue(method = "getModifiedCostCount", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/trading/MerchantOffer;demand:I"))
    private int nexium$priceIgnoresDemand(int demand) {
        return Math.min(demand, 0);
    }
}
