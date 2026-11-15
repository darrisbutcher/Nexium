package net.storagenexus.essentials.client.mixin;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.client.ShulkerPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** A shulker box with something in it gets a tooltip image: the grid of what is inside (the way a bundle has its own). */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void storagenexus$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ShulkerPreview preview = ShulkerPreview.of((ItemStack) (Object) this);
        if (preview != null) cir.setReturnValue(Optional.of(preview));
    }
}
