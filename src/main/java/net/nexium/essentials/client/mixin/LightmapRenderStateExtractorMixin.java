package net.nexium.essentials.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.nexium.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night vision while the free camera is away from the player: the lighting is drawn as if the player had the Night Vision effect at
 * full strength, so caves, nights and unlit rooms are as bright as day. Only how it looks changes (no effect is given to the player).
 */
@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void nexium$nightVisionInFreeCamera(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (FreeCamera.active()) state.nightVisionEffectIntensity = 1.0F;
    }
}
