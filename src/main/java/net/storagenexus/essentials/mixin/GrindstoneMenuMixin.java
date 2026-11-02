package net.storagenexus.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A grindstone takes an AIOT's inactive Silk Touch or Fortune off along with its other enchantments.
 * (This mixin belongs to the AIOT, a storage feature; it lives here because this is the mod's one mixin package.)
 */
@Mixin(GrindstoneMenu.class)
abstract class GrindstoneMenuMixin {
    @ModifyReturnValue(method = "removeNonCursesFrom", at = @At("RETURN"))
    private ItemStack storagenexus$removeInactiveEnchantments(ItemStack result) {
        result.remove(ModComponents.INACTIVE_ENCHANTMENTS);
        return result;
    }
}
