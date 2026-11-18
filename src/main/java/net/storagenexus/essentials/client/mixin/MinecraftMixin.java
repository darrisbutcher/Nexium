package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.Minecraft;
import net.storagenexus.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While the free camera is away from the player, nothing you click reaches the world: no attacking, breaking, placing or picking. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void nexium$noAttackWhileFreeCamera(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCamera.active()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void nexium$noBreakingWhileFreeCamera(boolean down, CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void nexium$noUseWhileFreeCamera(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void nexium$noPickWhileFreeCamera(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }
}
