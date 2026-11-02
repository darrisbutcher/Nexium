package net.storagenexus.essentials.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlotMenuSlot;
import net.storagenexus.essentials.slots.ExtraSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the trinket slot (menu index 46) and the wings slot (47) to the player's inventory, in a column
 * above the offhand slot, and the arrow (48) and totem (49) slots under the crafting grid. Shift-click sends a magnet,
 * elytra, arrows or a totem there first.
 */
@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends AbstractCraftingMenu {
    InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
        super(menuType, containerId, width, height);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nexusessentials$addExtraSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        ExtraSlots slots = ExtraSlots.of(owner);
        addSlot(new ExtraSlotMenuSlot(slots, owner, ExtraSlots.TRINKET, 77, 26));
        addSlot(new ExtraSlotMenuSlot(slots, owner, ExtraSlots.WINGS, 77, 44));
        // Under the crafting grid, in line with the last two columns of the inventory
        addSlot(new ExtraSlotMenuSlot(slots, owner, ExtraSlots.ARROWS, 135, 62));
        addSlot(new ExtraSlotMenuSlot(slots, owner, ExtraSlots.TOTEM, 153, 62));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void nexusessentials$quickMoveToExtraSlots(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        // Only from the main inventory, the hotbar and the offhand (9-45)
        if (slotIndex < InventoryMenu.INV_SLOT_START || slotIndex > InventoryMenu.SHIELD_SLOT) return;
        Slot from = slots.get(slotIndex);
        if (!from.hasItem()) return;
        ItemStack stack = from.getItem();
        for (int target = ExtraSlots.MENU_TRINKET; target <= ExtraSlots.MENU_TOTEM && target < slots.size(); target++) {
            Slot to = slots.get(target);
            if (!to.mayPlace(stack)) continue;
            // An empty slot, or one holding the same item with room left (arrows)
            if (to.hasItem() && (!ItemStack.isSameItemSameComponents(to.getItem(), stack) || to.getItem().getCount() >= to.getMaxStackSize())) continue;
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, target, target + 1, false)) return;
            if (stack.isEmpty()) {
                from.setByPlayer(ItemStack.EMPTY, original);
            } else {
                from.setChanged();
            }
            from.onTake(player, stack);
            cir.setReturnValue(original);
            return;
        }
    }
}
