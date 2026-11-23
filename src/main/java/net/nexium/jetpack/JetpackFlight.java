package net.nexium.jetpack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.nexium.energy.ItemEnergy;
import net.nexium.essentials.slots.ExtraSlots;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Flying with a Jetpack, on the game's own flight (the creative kind), so the client moves smoothly and a server does not kick for it:
 * with a Jetpack that has charge worn (wings or chest slot), a player in survival or adventure may fly. A jump while in the air starts it;
 * then jump rises, nothing hovers and sneak sinks. It costs {@link #THRUST} NE a tick while jump is held and {@link #HOVER} the rest of
 * the time, and when the cell is empty the flight ends and you fall.
 */
public final class JetpackFlight {
    public static final long THRUST = 8, HOVER = 4;

    /** Players whose flight this module switched on, so only that is switched off again. */
    private static final Set<UUID> GRANTED = new HashSet<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Flight saved with the player from an earlier Jetpack is not kept: it is granted again below while one is worn
            if (survivalLike(player) && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GRANTED.remove(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> GRANTED.clear());
    }

    private static boolean survivalLike(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }

    /** The Jetpack this player wears: in the wings slot, else in the chest slot; empty if none. The real stack. */
    public static ItemStack worn(ServerPlayer player) {
        ItemStack wings = ExtraSlots.of(player).wings();
        if (wings.getItem() instanceof JetpackItem) return wings;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof JetpackItem ? chest : ItemStack.EMPTY;
    }

    /** One tick for this player (public for the game tests). */
    public static void tick(ServerPlayer player) {
        if (!survivalLike(player)) {
            GRANTED.remove(player.getUUID());
            return;
        }
        ItemStack jetpack = worn(player);
        boolean powered = !jetpack.isEmpty() && player.isAlive() && ItemEnergy.stored(jetpack) >= HOVER;
        var abilities = player.getAbilities();
        if (!powered) {
            if (GRANTED.remove(player.getUUID())) {
                abilities.mayfly = false;
                abilities.flying = false;
                player.onUpdateAbilities();
            }
            return;
        }
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            GRANTED.add(player.getUUID());
            player.onUpdateAbilities();
        }
        Input input = player.getLastClientInput();
        // A jump in the air starts the flight (a jump from the ground, held on, does too once it is off the ground)
        if (!abilities.flying && input.jump() && !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.isFallFlying()) {
            abilities.flying = true;
            player.onUpdateAbilities();
        }
        if (!abilities.flying) return;
        long cost = input.jump() ? THRUST : HOVER;
        if (ItemEnergy.extract(jetpack, cost, false) < cost) {
            // Out of charge: the flight ends here
            abilities.mayfly = false;
            abilities.flying = false;
            GRANTED.remove(player.getUUID());
            player.onUpdateAbilities();
            return;
        }
        if (player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.7, player.getZ(), 1, 0.12, 0.0, 0.12, 0.01);
            if (input.jump()) level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.6, player.getZ(), 1, 0.12, 0.0, 0.12, 0.01);
        }
    }

    /** Opens the screen of the Jetpack this player wears, if any (the Open Jetpack key). */
    public static void openWorn(ServerPlayer player) {
        if (ExtraSlots.of(player).wings().getItem() instanceof JetpackItem) {
            JetpackItem.open(player, net.nexium.energy.CellSource.WINGS_SLOT, ExtraSlots.of(player).wings());
        } else if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof JetpackItem) {
            JetpackItem.open(player, net.nexium.energy.CellSource.CHEST_SLOT, player.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    public static boolean isGranted(ServerPlayer player) {
        return GRANTED.contains(player.getUUID());
    }

    private JetpackFlight() {}
}
