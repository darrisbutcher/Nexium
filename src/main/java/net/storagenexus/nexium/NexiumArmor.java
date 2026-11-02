package net.storagenexus.nexium;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.StorageNexus;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Nexium armor's bonuses ({@link ArmorBonus}). Set bonuses need all four pieces; Night Vision only the helmet. Each is
 * on unless switched off in the armor menu; the setting is saved on the piece that carries the bonus.
 * <ul>
 *   <li>Nether-forged: Fire Resistance, Night Vision: Night Vision (both via {@link HeldEffects});</li>
 *   <li>No knockback: a +1 knockback resistance modifier on top of the armor's own 0.6;</li>
 *   <li>Miner's set: in the player's mining speed (see the Player mixin);</li>
 *   <li>Nexus restock: {@link NexusRestock}.</li>
 * </ul>
 */
public final class NexiumArmor {
    private static final int CHECK_EVERY_TICKS = 5;
    private static final net.minecraft.resources.Identifier NO_KNOCKBACK_ID = StorageNexus.id("nexium_no_knockback");

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % CHECK_EVERY_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) update(player);
        });
    }

    private static Map<EquipmentSlot, Item> pieces() {
        return Map.of(EquipmentSlot.HEAD, ModItems.NEXIUM_HELMET, EquipmentSlot.CHEST, ModItems.NEXIUM_CHESTPLATE,
                EquipmentSlot.LEGS, ModItems.NEXIUM_LEGGINGS, EquipmentSlot.FEET, ModItems.NEXIUM_BOOTS);
    }

    /** How many Nexium pieces the entity wears (0 to 4). */
    public static int piecesWorn(LivingEntity entity) {
        int worn = 0;
        for (Map.Entry<EquipmentSlot, Item> e : pieces().entrySet()) if (entity.getItemBySlot(e.getKey()).is(e.getValue())) worn++;
        return worn;
    }

    public static boolean fullSet(LivingEntity entity) {
        return piecesWorn(entity) == 4;
    }

    /** Whether the entity wears what the bonus needs (all four pieces, or the helmet for Night Vision). */
    public static boolean available(LivingEntity entity, ArmorBonus bonus) {
        return bonus.needsFullSet() ? fullSet(entity) : entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NEXIUM_HELMET);
    }

    /** Whether the bonus is switched on, on the piece that carries it. */
    public static boolean switchedOn(LivingEntity entity, ArmorBonus bonus) {
        return !entity.getItemBySlot(bonus.slot()).getOrDefault(ModComponents.BONUSES_OFF, List.of()).contains(bonus);
    }

    /** Whether the entity has the bonus right now: wears what it needs and it is switched on. */
    public static boolean active(LivingEntity entity, ArmorBonus bonus) {
        return available(entity, bonus) && switchedOn(entity, bonus);
    }

    /** Switches a bonus on or off (armor menu). Does nothing unless the entity wears what it needs. */
    public static void toggle(ServerPlayer player, ArmorBonus bonus) {
        if (!available(player, bonus)) return;
        ItemStack piece = player.getItemBySlot(bonus.slot());
        List<ArmorBonus> off = new ArrayList<>(piece.getOrDefault(ModComponents.BONUSES_OFF, List.of()));
        boolean nowOn = off.remove(bonus);
        if (!nowOn) off.add(bonus);
        if (off.isEmpty()) piece.remove(ModComponents.BONUSES_OFF);
        else piece.set(ModComponents.BONUSES_OFF, List.copyOf(off));
        update(player);
        player.sendOverlayMessage(Component.translatable(nowOn ? "message.storagenexus.armor_menu.on" : "message.storagenexus.armor_menu.off",
                Component.translatable("armor_bonus.storagenexus." + bonus.id())).withStyle(ChatFormatting.WHITE));
    }

    /** Gives or takes the effects and the knockback modifier for what the player wears now. */
    public static void update(ServerPlayer player) {
        boolean alive = !player.isSpectator();
        HeldEffects.set(player, MobEffects.FIRE_RESISTANCE, 0, alive && active(player, ArmorBonus.NETHER_FORGED));
        HeldEffects.set(player, MobEffects.NIGHT_VISION, 0, alive && active(player, ArmorBonus.NIGHT_VISION));
        AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            if (active(player, ArmorBonus.NO_KNOCKBACK)) {
                knockback.addOrUpdateTransientModifier(new AttributeModifier(NO_KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else {
                knockback.removeModifier(NO_KNOCKBACK_ID);
            }
        }
    }

    private NexiumArmor() {}
}
