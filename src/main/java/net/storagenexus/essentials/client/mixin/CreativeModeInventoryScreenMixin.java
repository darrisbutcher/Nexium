package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.storagenexus.essentials.slots.ExtraSlotMenuSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In the creative inventory tab, puts the trinket, wings, arrow and totem slots right of the armor (vanilla would stack them
 * on the hotbar row) and draws their frames.
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final String SLOT_WRAPPER_INIT =
            "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V";
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int X = 127;
    /** Down the column right of the armor, 27 apart like the armor slots: trinket, wings, arrows, totem. */
    private static final int[] Y = {6, 33, 60, 87};

    @Shadow
    private static CreativeModeTab selectedTab;

    CreativeModeInventoryScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = SLOT_WRAPPER_INIT), index = 2)
    private int nexusessentials$extraSlotX(Slot target, int index, int x, int y) {
        return target instanceof ExtraSlotMenuSlot ? X : x;
    }

    @ModifyArg(method = "selectTab", at = @At(value = "INVOKE", target = SLOT_WRAPPER_INIT), index = 3)
    private int nexusessentials$extraSlotY(Slot target, int index, int x, int y) {
        return target instanceof ExtraSlotMenuSlot slot ? Y[slot.getContainerSlot()] : y;
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void nexusessentials$drawExtraSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
        for (int y : Y) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + X - 1, topPos + y - 1, 18, 18);
    }
}
