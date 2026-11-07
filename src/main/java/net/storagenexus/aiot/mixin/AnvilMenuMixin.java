package net.storagenexus.aiot.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.elytra.ElytraEnchanting;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An elytra on the anvil can take every protection enchantment at once, an AIOT both Silk Touch and Fortune, and nothing is
 * ever "Too Expensive!": the cost stops at {@code anvilMaxCost} (39) levels instead of the result vanishing at 40.
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;

    AnvilMenuMixin(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(menuType, containerId, inventory, access, slots);
    }

    @WrapOperation(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/Enchantment;areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z"))
    private boolean nexusessentials$elytraTakesAllProtections(Holder<Enchantment> enchantment, Holder<Enchantment> other, Operation<Boolean> original) {
        return original.call(enchantment, other) || ElytraEnchanting.mayStack(inputSlots.getItem(0), enchantment, other)
                || AiotEnchanting.mayHoldBoth(inputSlots.getItem(0), enchantment, other);
    }

    /** The "Too Expensive!" check (the third 40 in createResult): never true while the cost is capped. */
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
    private int storagenexus$noTooExpensive(int tooExpensive) {
        return NexusConfig.get().anvilMaxCost > 0 ? Integer.MAX_VALUE : tooExpensive;
    }

    /** ...and the cost is capped instead, which the anvil screen then shows (below 40, so without the warning). */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void storagenexus$capCost(CallbackInfo ci) {
        int max = NexusConfig.get().anvilMaxCost;
        if (max > 0 && cost.get() > max) cost.set(max);
    }

    /** An AIOT keeps both Silk Touch and Fortune, one of them inactive (a storage feature; this is the one mixin package). */
    @Inject(method = "createResult", at = @At("TAIL"))
    private void storagenexus$aiotKeepsSilkTouchAndFortune(CallbackInfo ci) {
        AiotEnchanting.afterAnvil(inputSlots.getItem(0), inputSlots.getItem(1), resultSlots.getItem(0));
    }
}
