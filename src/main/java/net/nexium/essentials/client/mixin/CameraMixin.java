package net.nexium.essentials.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.nexium.essentials.client.FreeCamera;
import net.nexium.essentials.client.Zoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The zoom narrows the field of view of the world; the hand's own field of view is left alone. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void nexium$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Zoom.fov(cir.getReturnValueF(), partialTick));
    }

    /**
     * Cave culling is off while the free camera is away. The game only draws the sections it can reach from the camera through open
     * space (so it does not draw the rock around a cave you are not in), and which ones those are depends on the direction it is
     * facing. From inside rock, or from below the world, that leaves most caves undrawn until you turn or move: the game turns it off
     * itself for a spectator inside a block, and the free camera is the same. Everything in view is drawn instead.
     */
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void nexium$noCaveCullingInFreeCamera(CameraRenderState state, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (FreeCamera.active()) state.smartCull = false;
    }
}
