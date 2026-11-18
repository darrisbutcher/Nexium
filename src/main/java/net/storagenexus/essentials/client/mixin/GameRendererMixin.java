package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.storagenexus.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The hand and the item in it are not drawn while the free camera is away from the player: they belong to a view the camera is not in. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void nexium$noHandWhileFreeCamera(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }
}
