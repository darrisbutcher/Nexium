package net.storagenexus.essentials.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.essentials.slots.ExtraSlotsHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives every player the trinket and wings slots, saves them, and drops them on death. */
@Mixin(Player.class)
abstract class PlayerMixin extends LivingEntity implements ExtraSlotsHolder {
    @Unique
    private ExtraSlots nexusessentials$extraSlots;

    PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public ExtraSlots nexusessentials$extraSlots() {
        // Created on first use: the inventory menu asks for it while the player is still being constructed
        if (nexusessentials$extraSlots == null) nexusessentials$extraSlots = new ExtraSlots();
        return nexusessentials$extraSlots;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void nexusessentials$saveExtraSlots(ValueOutput output, CallbackInfo ci) {
        nexusessentials$extraSlots().save(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void nexusessentials$loadExtraSlots(ValueInput input, CallbackInfo ci) {
        nexusessentials$extraSlots().load(input);
    }

    /** Same rules as the rest of the inventory: kept with keepInventory, Curse of Vanishing destroys, otherwise dropped. */
    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void nexusessentials$dropExtraSlots(ServerLevel level, CallbackInfo ci) {
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        ExtraSlots slots = nexusessentials$extraSlots();
        for (int i = 0; i < slots.getContainerSize(); i++) {
            ItemStack stack = slots.removeItemNoUpdate(i);
            if (stack.isEmpty() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;
            ItemEntity drop = createItemStackToDrop(stack, true, false);
            if (drop != null) level.addFreshEntity(drop);
        }
    }
}
