package net.storagenexus.essentials.client.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.storagenexus.essentials.config.NexusConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The anvil screen shows the full cost ("Enchantment Cost: 52") instead of "Too Expensive!" when the limit is off. */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int storagenexus$noTooExpensive(int tooExpensive) {
        return NexusConfig.get().anvilNoCostLimit ? Integer.MAX_VALUE : tooExpensive;
    }
}
