package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws frames for the trinket and wings slots, in the column above the offhand slot. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
        super(menu, recipeBook, inventory, title);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void nexusessentials$drawExtraSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + 76, topPos + 25, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + 76, topPos + 43, 18, 18);
    }
}
