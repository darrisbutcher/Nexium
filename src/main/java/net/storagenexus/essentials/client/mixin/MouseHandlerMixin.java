package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.MouseHandler;
import net.storagenexus.client.ToolMenuClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The scroll wheel, held with the tunnel key and an AIOT in hand, steps the tunnel length instead of the hotbar slot. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void storagenexus$tunnelScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ToolMenuClient.tunnelScroll(yOffset)) ci.cancel();
    }
}
