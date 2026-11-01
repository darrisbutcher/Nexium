package net.storagenexus.essentials.slots;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.storagenexus.essentials.registry.ModPayloads;

public final class ExtraSlotEvents {
    public static void init() {
        // Respawning (with keepInventory) and leaving the End keep the slots; on a normal death they were already dropped.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                ExtraSlots.of(newPlayer).copyFrom(ExtraSlots.of(oldPlayer)));

        // Elytra flight from the wings slot. Vanilla handles an elytra worn in the chest slot itself.
        EntityElytraEvents.CUSTOM.register(ExtraSlotEvents::glideWithWingsSlot);

        // Right-clicking an elytra puts it in the wings slot instead of the chest slot.
        UseItemCallback.EVENT.register((player, level, hand) -> equipWings(player, hand));

        // Other players need to see the elytra worn in the wings slot.
        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
            if (tracked instanceof Player player && !ExtraSlots.of(player).wings().isEmpty()) {
                ServerPlayNetworking.send(viewer, new ModPayloads.WingsSync(player.getId(), ExtraSlots.of(player).wings().copy()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ExtraSlots slots = ExtraSlots.of(player);
                if (slots.wingsNeedSending()) {
                    ModPayloads.WingsSync payload = new ModPayloads.WingsSync(player.getId(), slots.wings().copy());
                    for (ServerPlayer viewer : PlayerLookup.tracking(player)) ServerPlayNetworking.send(viewer, payload);
                }
            }
        });
    }

    /** True if an item in one of the entity's equipment slots already lets it glide, which vanilla handles. */
    private static boolean hasVanillaGlider(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(entity.getItemBySlot(slot), slot)) return true;
        }
        return false;
    }

    private static boolean glideWithWingsSlot(LivingEntity entity, boolean tickElytra) {
        if (!(entity instanceof Player player) || hasVanillaGlider(player)) return false;
        ItemStack wings = ExtraSlots.of(player).wings();
        if (!ExtraSlots.canGlideWith(wings)) return false;
        if (tickElytra && player.level() instanceof ServerLevel level) {
            // Called once a second while flying, like vanilla's elytra damage
            wings.hurtAndBreak(1, level, player instanceof ServerPlayer sp ? sp : null, item -> {});
            player.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
        return true;
    }

    private static InteractionResult equipWings(Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        if (!ExtraSlots.isWings(inHand) || player.isSpectator()) return InteractionResult.PASS;
        ExtraSlots slots = ExtraSlots.of(player);
        ItemStack worn = slots.wings();
        if (ExtraSlots.isBound(worn, player) || ItemStack.isSameItemSameComponents(inHand, worn)) return InteractionResult.PASS;

        if (!player.level().isClientSide()) player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
        if (inHand.getCount() <= 1) {
            ItemStack toHand = worn.isEmpty() ? ItemStack.EMPTY : worn.copy();
            ItemStack toSlot = player.isCreative() ? inHand.copy() : inHand.copyAndClear();
            slots.setItem(ExtraSlots.WINGS, toSlot);
            if (!player.isCreative() || !toHand.isEmpty()) player.setItemInHand(hand, toHand);
        } else {
            ItemStack toInventory = worn.copy();
            slots.setItem(ExtraSlots.WINGS, inHand.consumeAndReturn(1, player));
            if (!toInventory.isEmpty() && !player.getInventory().add(toInventory)) player.drop(toInventory, false, Prediction.PREDICTED);
        }
        player.onEquipItem(EquipmentSlot.CHEST, worn, slots.wings());
        return InteractionResult.SUCCESS;
    }

    private ExtraSlotEvents() {}
}
