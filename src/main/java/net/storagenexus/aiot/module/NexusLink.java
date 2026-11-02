package net.storagenexus.aiot.module;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.dimension.AnchorRegistry;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.network.TerminalBlock;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Nexus Link module: what an AIOT with an active, linked Nexus Link mines goes straight into the linked Crafting
 * Terminal's storage network, in any dimension. The terminal's chunk has to be loaded, as for tablets: a Dimensional
 * Anchor set to Always, or one set to On-demand, which the link wakes while the player is mining and lets sleep again
 * 30 seconds after the last block. Whatever the network can't take drops as usual, and so does everything when the
 * network can't be reached; a message above the hotbar says which.
 */
public final class NexusLink {
    private static final int KEEP_AWAKE_TICKS = 30 * 20;
    private static final int MESSAGE_EVERY_TICKS = 5 * 20;

    /** An on-demand anchor kept awake for a player's link, until {@code until} (game time). */
    private record Hold(GlobalPos link, Runnable release, long until) {}

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            HOLDS.values().removeIf(hold -> {
                if (hold.until() > now) return false;
                hold.release().run();
                return true;
            });
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            HOLDS.values().forEach(hold -> hold.release().run());
            HOLDS.clear();
            LAST_MESSAGE.clear();
        });
    }

    /**
     * Drops a block a player mined into the network their tool's Nexus Link points at, instead of onto the ground.
     * Called in place of vanilla's drop code; returns false (vanilla goes ahead) if the tool has no active, linked
     * Nexus Link.
     */
    public static boolean dropInto(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity,
                                   ServerPlayer player, ItemStack tool) {
        GlobalPos link = AiotModules.activeLink(tool);
        if (link == null || Transaction.isOpen()) return false;
        StorageNetwork network = network(player, link);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
        boolean leftOver = false;
        for (ItemStack drop : drops) {
            ItemStack rest = network == null ? drop : insert(network, drop);
            if (!rest.isEmpty()) {
                Block.popResource(level, pos, rest);
                leftOver = true;
            }
        }
        state.spawnAfterBreak(level, pos, tool, true);
        if (network == null) message(player, "message.storagenexus.nexus_link.offline");
        else if (leftOver) message(player, "message.storagenexus.nexus_link.full");
        return true;
    }

    private static ItemStack insert(StorageNetwork network, ItemStack drop) {
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = network.insertItem(ItemVariant.of(drop), drop.getCount(), tx);
            tx.commit();
            ItemStack rest = drop.copy();
            rest.shrink((int) inserted);
            return rest;
        }
    }

    /**
     * The linked terminal's network, waking an on-demand anchor if one covers it (kept awake 30 s after the last use, per
     * player); null if it can't be reached. Also used by the Nexium armor's Nexus Restock.
     */
    public static @Nullable StorageNetwork network(ServerPlayer player, GlobalPos link) {
        MinecraftServer server = player.level().getServer();
        ServerLevel target = server == null ? null : server.getLevel(link.dimension());
        if (target == null) return null;
        UUID id = player.getUUID();
        long until = server.overworld().getGameTime() + KEEP_AWAKE_TICKS;
        Hold hold = HOLDS.get(id);
        if (hold != null && !hold.link().equals(link)) {
            hold.release().run();
            HOLDS.remove(id);
            hold = null;
        }
        if (hold != null) {
            HOLDS.put(id, new Hold(link, hold.release(), until));
        } else if (!target.isLoaded(link.pos())) {
            Runnable release = AnchorRegistry.get(server).loadOnDemand(target, link.pos());
            if (release != null) HOLDS.put(id, new Hold(link, release, until));
        }
        if (!target.isLoaded(link.pos()) || !(target.getBlockState(link.pos()).getBlock() instanceof TerminalBlock)) return null;
        return StorageNetwork.find(target, link.pos());
    }

    private static void message(ServerPlayer player, String key) {
        long now = player.level().getGameTime();
        Long last = LAST_MESSAGE.get(player.getUUID());
        if (last != null && now - last < MESSAGE_EVERY_TICKS && now >= last) return;
        LAST_MESSAGE.put(player.getUUID(), now);
        player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    private NexusLink() {}
}
