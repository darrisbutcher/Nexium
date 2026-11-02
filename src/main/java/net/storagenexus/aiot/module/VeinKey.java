package net.storagenexus.aiot.module;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Which players are holding the Vein Mine key right now. Vein mining only happens while it is held. */
public final class VeinKey {
    private static final Set<UUID> HELD = ConcurrentHashMap.newKeySet();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(VeinKeyPayload.TYPE,
                (payload, context) -> set(context.player(), payload.held()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HELD.remove(handler.getPlayer().getUUID()));
    }

    public static void set(ServerPlayer player, boolean held) {
        if (held) HELD.add(player.getUUID());
        else HELD.remove(player.getUUID());
    }

    public static boolean held(ServerPlayer player) {
        return HELD.contains(player.getUUID());
    }

    private VeinKey() {}
}
