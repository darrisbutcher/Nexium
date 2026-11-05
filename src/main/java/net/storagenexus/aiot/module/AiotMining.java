package net.storagenexus.aiot.module;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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
import net.storagenexus.essentials.config.NexusConfig;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The 3x3 and vein mining modules. When a player mines a block with an AIOT that has one on, the extra blocks are
 * mined as if the player had mined each one: same drops (Fortune, Silk Touch, a Nexus Link), 1 durability each, spawn
 * protection and the like respected. Sneaking mines just the one block.
 * <ul>
 *   <li><b>Vein mining</b>, for any block the AIOT is made for except the stone family ({@code #storagenexus:vein_excluded}:
 *       stone, cobblestone, granite, deepslate, netherrack and the like) and blocks with contents: every block of the
 *       same kind touching it, corners included, up to the size set in the tool menu (never above the config's limit)
 *       in all. Only while the player holds the Vein Mine key.</li>
 *   <li><b>3x3 mining</b>, for anything else the AIOT is made for: the 3x3 around it, facing the player (a wall when
 *       looking ahead, the floor or ceiling when looking down or up). Only blocks the AIOT is made for and no harder
 *       than about twice the mined block; never blocks with contents, such as chests. With a tunnel length set in the
 *       tool menu, the same 3x3 in each of that many layers going away from the player.</li>
 * </ul>
 * With both on, anything but stone is vein mined (key held) and stone is 3x3 mined.
 */
public final class AiotMining {
    /** The stone family: vein mining leaves it to the 3x3, since a vein of stone is the whole cave wall. */
    public static final TagKey<Block> VEIN_EXCLUDED = TagKey.create(Registries.BLOCK, StorageNexus.id("vein_excluded"));

    /** Set while mining the extra blocks, so they don't set off more of their own. */
    private static boolean busy;

    public static void init() {
        VeinKey.init();
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!busy && player instanceof ServerPlayer serverPlayer) afterBreak(level, serverPlayer, pos, state);
        });
    }

    private static void afterBreak(Level level, ServerPlayer player, BlockPos pos, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        List<BlockPos> extra = extraBlocks(level, player, tool, pos, state, VeinKey.held(player));
        if (extra.isEmpty()) return;
        busy = true;
        try {
            // A Nexus Link sends the whole swing's drops to the network in one go
            NexusLink.batch(player, () -> {
                for (BlockPos target : extra) {
                    // Stop if the tool breaks or is put away part of the way through
                    if (!(player.getMainHandItem().getItem() instanceof AiotItem)) break;
                    // Spawn protection and the world border, which vanilla checks before the first block only
                    if (!level.mayInteract(player, target)) continue;
                    player.gameMode.destroyBlock(target);
                }
            });
        } finally {
            busy = false;
        }
    }

    /**
     * The blocks mined along with {@code pos} when {@code player} mines it with {@code tool}: a vein, a 3x3 or a tunnel,
     * or none (an empty list). Shared by the server, which mines them, and the client, which outlines them as a preview.
     */
    public static List<BlockPos> extraBlocks(Level level, Player player, ItemStack tool, BlockPos pos, BlockState state, boolean veinKeyHeld) {
        if (!(tool.getItem() instanceof AiotItem) || player.isSecondaryUseActive()) return List.of();
        NexusConfig config = NexusConfig.get();
        if (config.aiotVeinMining && AiotModules.active(tool, ModuleType.VEIN_MINING) && veinKeyHeld && veinable(level, pos, state, tool)) {
            return vein(level, pos, state, AiotModules.veinSize(tool));
        }
        if (config.aiotAreaMining && AiotModules.active(tool, ModuleType.AREA_MINING) && tool.getDestroySpeed(state) > 1.0F) {
            return area(level, player, pos, state, tool, AiotModules.tunnelLength(tool));
        }
        return List.of();
    }

    /** Whether vein mining takes this block: not stone, not a block with contents or one that can't be broken, and one the tool is made for. */
    private static boolean veinable(Level level, BlockPos pos, BlockState state, ItemStack tool) {
        return !state.is(VEIN_EXCLUDED) && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0 && tool.getDestroySpeed(state) > 1.0F;
    }

    /** The ore group a block belongs to ({@code #c:ores/diamond} and the like), or null if it stands alone. */
    private static @Nullable TagKey<Block> oreGroup(BlockState state) {
        return state.tags()
                .filter(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("ores/"))
                .findFirst().orElse(null);
    }

    /**
     * The other blocks of this kind connected to {@code start} (corners count), nearest first, up to {@code size}
     * blocks in all. Blocks of the same ore group count as one kind, so the deepslate and stone variants of an ore join up.
     * Only loaded chunks are looked at, so a vein never makes the server load or generate one.
     */
    static List<BlockPos> vein(Level level, BlockPos start, BlockState startState, int size) {
        int limit = size - 1;
        Block block = startState.getBlock();
        TagKey<Block> group = oreGroup(startState);
        List<BlockPos> found = new ArrayList<>();
        LongSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        seen.add(start.asLong());
        queue.enqueue(start.asLong());
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty() && found.size() < limit) {
            long current = queue.dequeueLong();
            int cx = BlockPos.getX(current), cy = BlockPos.getY(current), cz = BlockPos.getZ(current);
            for (int dx = -1; dx <= 1 && found.size() < limit; dx++) {
                for (int dy = -1; dy <= 1 && found.size() < limit; dy++) {
                    for (int dz = -1; dz <= 1 && found.size() < limit; dz++) {
                        next.set(cx + dx, cy + dy, cz + dz);
                        if (!level.hasChunkAt(next) || !seen.add(next.asLong())) continue;
                        BlockState state = level.getBlockState(next);
                        if (!state.is(block) && (group == null || !state.is(group))) continue;
                        found.add(next.immutable());
                        queue.enqueue(next.asLong());
                    }
                }
            }
        }
        return found;
    }

    /**
     * The other blocks of the 3x3 around {@code center}, across the direction the player is looking, and with
     * {@code layers} above 1, of the same 3x3 in each of the next layers further in: a tunnel {@code layers} deep. Nearest
     * layer first. Only loaded chunks are looked at.
     */
    static List<BlockPos> area(Level level, Player player, BlockPos center, BlockState centerState, ItemStack tool, int layers) {
        Direction facing = Direction.orderedByNearest(player)[0];
        float centerHardness = centerState.getDestroySpeed(level, center);
        List<BlockPos> found = new ArrayList<>();
        for (int layer = 0; layer < layers; layer++) {
            BlockPos middle = center.relative(facing, layer);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (layer == 0 && a == 0 && b == 0) continue;
                    BlockPos pos = switch (facing.getAxis()) {
                        case X -> middle.offset(0, a, b);
                        case Y -> middle.offset(a, 0, b);
                        case Z -> middle.offset(a, b, 0);
                    };
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    float hardness = state.getDestroySpeed(level, pos);
                    if (state.isAir() || hardness < 0 || hardness > centerHardness * 2 + 0.5F) continue;
                    if (state.hasBlockEntity() || tool.getDestroySpeed(state) <= 1.0F) continue;
                    found.add(pos);
                }
            }
        }
        return found;
    }

    private AiotMining() {}
}
