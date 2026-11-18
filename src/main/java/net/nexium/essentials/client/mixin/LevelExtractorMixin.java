package net.nexium.essentials.client.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.nexium.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The game does not draw the local player when the camera is on some other entity (that is how spectating a mob avoids drawing you
 * inside it). With the free camera away from the player, the player is drawn: you see your own body where you left it (not while the
 * camera is still inside it, right after switching it on).
 */
@Mixin(LevelExtractor.class)
abstract class LevelExtractorMixin {
    @Definition(id = "LocalPlayer", type = LocalPlayer.class)
    @Expression("? instanceof LocalPlayer")
    @ModifyExpressionValue(method = "extractVisibleEntities", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nexium$drawThePlayerFromTheFreeCamera(boolean isLocalPlayer) {
        return isLocalPlayer && !FreeCamera.showsPlayer();
    }
}
