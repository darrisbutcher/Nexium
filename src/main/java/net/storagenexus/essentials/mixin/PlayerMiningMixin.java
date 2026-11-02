package net.storagenexus.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.storagenexus.nexium.ArmorBonus;
import net.storagenexus.nexium.NexiumArmor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Nexium armor's Miner's set bonus: no mining slowdown underwater or off the ground. Vanilla multiplies the mining
 * speed by the submerged mining speed attribute (0.2) with the eyes in water and divides it by 5 off the ground; this
 * undoes both. It runs on both sides, so the crack animation matches the server. (A Nexium feature; it lives here
 * because this is the mod's one mixin package.)
 */
@Mixin(Player.class)
abstract class PlayerMiningMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float storagenexus$minersSet(float speed) {
        Player player = (Player) (Object) this;
        if (!NexiumArmor.active(player, ArmorBonus.MINERS_SET)) return speed;
        if (player.isEyeInFluid(FluidTags.WATER)) {
            double submerged = player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if (submerged > 0 && submerged < 1) speed /= (float) submerged;
        }
        if (!player.onGround()) speed *= 5.0F;
        return speed;
    }
}
