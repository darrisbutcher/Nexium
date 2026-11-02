package net.storagenexus.aiot.module;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.StorageNexus;
import net.storagenexus.aiot.AiotItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The 3x3 and vein mining modules. When a player mines a block with an AIOT that has one on, the extra blocks are
 * mined as if the player had mined each one: same drops (Fortune, Silk Touch, a Nexus Link), 1 durability each, spawn
 * protection and the like respected. Sneaking mines just the one block.
 * <ul>
 *   <li><b>Vein mining</b>, for blocks in {@code #storagenexus:vein_mineable} (ores and logs): every block of the same
 *       kind touching it, corners included, up to {@value #MAX_VEIN} in all.</li>
 *   <li><b>3x3 mining</b>, for anything else the AIOT is made for: the 3x3 around it, facing the player (a wall when
 *       looking ahead, the floor or ceiling when looking down or up). Only blocks the AIOT is made for and no harder
 *       than about twice the mined block; never blocks with contents, such as chests.</li>
 * </ul>
 * With both on, an ore or log is vein mined and anything else 3x3 mined.
 */
public final class AiotMining {
    public static final TagKey<Block> VEIN_MINEABLE = TagKey.create(Registries.BLOCK, StorageNexus.id("vein_mineable"));
    public static final int MAX_VEIN = 64;

    /** Set while mining the extra blocks, so they don't set off more of their own. */
    private static boolean busy;

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!busy && player instanceof ServerPlayer serverPlayer) afterBreak(level, serverPlayer, pos, state);
        });
    }

    private static void afterBreak(Level level, ServerPlayer player, BlockPos pos, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AiotItem) || player.isSecondaryUseActive()) return;
        List<BlockPos> extra;
        if (AiotModules.active(tool, ModuleType.VEIN_MINING) && state.is(VEIN_MINEABLE)) {
            extra = vein(level, pos, state.getBlock());
        } else if (AiotModules.active(tool, ModuleType.AREA_MINING) && tool.getDestroySpeed(state) > 1.0F) {
            extra = area(level, player, pos, state, tool);
        } else {
            return;
        }
        busy = true;
        try {
            for (BlockPos target : extra) {
                // Stop if the tool breaks or is put away part of the way through
                if (!(player.getMainHandItem().getItem() instanceof AiotItem)) break;
                // Spawn protection and the world border, which vanilla checks before the first block only
                if (!level.mayInteract(player, target)) continue;
                player.gameMode.destroyBlock(target);
            }
        } finally {
            busy = false;
        }
    }

    /** The other blocks of this kind connected to {@code start} (corners count), nearest first, up to the vein limit. */
    static List<BlockPos> vein(Level level, BlockPos start, Block block) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < MAX_VEIN - 1) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (found.size() >= MAX_VEIN - 1) break;
                BlockPos immutable = next.immutable();
                if (!seen.add(immutable) || !level.getBlockState(immutable).is(block)) continue;
                found.add(immutable);
                queue.add(immutable);
            }
        }
        return found;
    }

    /** The other 8 blocks of the 3x3 around {@code center}, across the direction the player is looking. */
    static List<BlockPos> area(Level level, Player player, BlockPos center, BlockState centerState, ItemStack tool) {
        Direction facing = Direction.orderedByNearest(player)[0];
        float centerHardness = centerState.getDestroySpeed(level, center);
        List<BlockPos> found = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos pos = switch (facing.getAxis()) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || hardness < 0 || hardness > centerHardness * 2 + 0.5F) continue;
                if (state.hasBlockEntity() || tool.getDestroySpeed(state) <= 1.0F) continue;
                found.add(pos);
            }
        }
        return found;
    }

    private AiotMining() {}
}
