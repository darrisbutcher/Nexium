package net.nexium.essentials.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.nexium.autocraft.Pattern;
import net.nexium.registry.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An encoded Pattern shows what it makes: the item it is for is drawn small, in the middle of the pattern card, so the card
 * stays visible around it as the mark that this is a pattern and not the item itself.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class PatternIconMixin {
    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void nexium$patternIcon(Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
        Pattern pattern = stack.get(ModComponents.PATTERN);
        if (pattern == null) return;
        ItemStack made = pattern.primary();
        if (made.isEmpty()) return;
        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        self.pose().pushMatrix();
        self.pose().translate(x + 3.2f, y + 3.2f);
        self.pose().scale(0.6f, 0.6f);
        self.item(made.copyWithCount(1), 0, 0);
        self.pose().popMatrix();
    }
}
