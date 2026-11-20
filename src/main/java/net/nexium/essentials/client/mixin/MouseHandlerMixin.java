package net.nexium.essentials.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.nexium.client.ToolMenuClient;
import net.nexium.essentials.client.FreeCamera;
import net.nexium.essentials.client.Zoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The scroll wheel, held with the tunnel key and an AIOT in hand, steps the tunnel length instead of the hotbar slot. With the free
 * camera away, Left Ctrl and the scroll wheel change the camera's speed (the plain wheel does nothing), and the mouse turns the camera instead of the player.
 * With the zoom key held the wheel changes the zoom level instead, and while zoomed the mouse turns the view less.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void nexium$tunnelScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (Zoom.scroll(yOffset) || FreeCamera.scroll(yOffset) || ToolMenuClient.tunnelScroll(yOffset)) ci.cancel();
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void nexium$freeCameraLook(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        // Zoomed in, the mouse turns the view less, so aiming stays steady
        double scale = Zoom.lookScale();
        yaw *= scale;
        pitch *= scale;
        Entity camera = FreeCamera.camera();
        if (camera != null) camera.turn(yaw, pitch);
        else original.call(player, yaw, pitch);
    }
}
