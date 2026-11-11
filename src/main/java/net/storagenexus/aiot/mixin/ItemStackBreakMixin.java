package net.storagenexus.aiot.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.registry.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * An AIOT or a Construction Builder that wears out loses its modules with it in vanilla, since they live in the tool's
 * own data. This hands them back instead: just before the broken stack is used up, every installed module drops at the player's feet, switch
 * and Nexus Link terminal intact. (A tool broken by something other than a player has nowhere to drop them.)
 */
@Mixin(ItemStack.class)
abstract class ItemStackBreakMixin {
    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void storagenexus$dropModules(int damage, ServerPlayer player, Consumer<ItemStack> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (player == null || !self.has(ModComponents.MODULES)) return; // an AIOT, or a Construction Builder
        for (ItemStack module : AiotModules.stored(self)) {
            player.spawnAtLocation(player.level(), module.copy());
        }
        self.remove(ModComponents.MODULES);
    }
}
