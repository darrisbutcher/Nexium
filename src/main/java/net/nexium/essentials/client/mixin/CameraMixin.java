package net.nexium.essentials.client.mixin;

import net.minecraft.client.Camera;
import net.nexium.essentials.client.Zoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom narrows the field of view of the world; the hand's own field of view is left alone. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void nexium$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Zoom.fov(cir.getReturnValueF(), partialTick));
    }
}
