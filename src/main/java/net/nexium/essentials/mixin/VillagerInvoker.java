package net.nexium.essentials.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reaches the villager's private price update, so refreshed trades get the same discounts (reputation, Hero of the Village) as the ones they replace. */
@Mixin(Villager.class)
public interface VillagerInvoker {
    @Invoker("updateSpecialPrices")
    void nexium$updateSpecialPrices(Player player);
}
