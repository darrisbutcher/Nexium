package net.storagenexus.essentials.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.storagenexus.client.ToolMenuClient;
import net.storagenexus.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The scroll wheel, held with the tunnel key and an AIOT in hand, steps the tunnel length instead of the hotbar slot. With the free
 * camera away it changes the camera's speed, and the mouse turns the camera instead of the player.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void storagenexus$tunnelScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (FreeCamera.scroll(yOffset) || ToolMenuClient.tunnelScroll(yOffset)) ci.cancel();
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void storagenexus$freeCameraLook(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        Entity camera = FreeCamera.camera();
        if (camera != null) camera.turn(yaw, pitch);
        else original.call(player, yaw, pitch);
    }
}
