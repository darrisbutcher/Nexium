package net.nexium.essentials.client.mixin;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.nexium.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No fog while the free camera is away from the player: the camera sees clearly to the edge of the loaded world, underwater, in lava and through effects. */
@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void nexium$noFogInFreeCamera(CallbackInfoReturnable<FogData> cir) {
        if (FreeCamera.active()) FreeCamera.removeFog(cir.getReturnValue());
    }
}
