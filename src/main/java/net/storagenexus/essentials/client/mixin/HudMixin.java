package net.storagenexus.essentials.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.storagenexus.essentials.client.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The experience level number over the hotbar is not drawn while the free camera is away from the player (the bar itself is hidden in ExperienceBarMixin). */
@Mixin(Hud.class)
abstract class HudMixin {
    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void nexium$noLevelNumberInFreeCamera(GuiGraphicsExtractor graphics, Font font, int level, Operation<Void> original) {
        if (!FreeCamera.active()) original.call(graphics, font, level);
    }
}
