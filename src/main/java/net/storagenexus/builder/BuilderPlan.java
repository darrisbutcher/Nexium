package net.storagenexus.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a Construction Builder click puts blocks. The same rules run on the client for the outline and on the server
 * for the real thing, so what is outlined is what gets built (as long as there are blocks to place).
 *
 * <ul>
 *   <li><b>Extend</b>: every block of the clicked kind connected to it across the clicked face (and not covered there)
 *       gets a new block on that face, nearest first.</li>
 *   <li><b>Line</b>: the same, along only the face's axis you are looking along.</li>
 *   <li><b>Exchange</b>: the clicked surface itself (connected blocks of the clicked kind with that face open) is swapped
 *       for the block in your offhand.</li>
 * </ul>
 * The block placed is the one in your offhand if you hold one there, otherwise the clicked block, facing the same way.
 *
 * @param targets where blocks go (Exchange: the blocks it replaces)
 * @param state   what goes there
 * @param item    the item each one uses up
 */
public record BuilderPlan(List<BlockPos> targets, BlockState state, Item item, boolean exchange) {
    public static final BuilderPlan NONE = new BuilderPlan(List.of(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Items.AIR, false);

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public static BuilderPlan of(Level level, Player player, ItemStack builder, BlockPos clicked, Direction face) {
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.isAir() || !level.getFluidState(clicked).isEmpty() && clickedState.canBeReplaced()) return NONE;
        if (multiBlock(clickedState)) return NONE;
        Block clickedBlock = clickedState.getBlock();
        ItemStack offhand = player.getOffhandItem();
        Block offBlock = offhand.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        BuilderSettings.Mode mode = BuilderSettings.mode(builder);
        int max = BuilderSettings.size(builder);
        List<Direction> steps = steps(face, BuilderSettings.plane(builder), mode == BuilderSettings.Mode.LINE ? player.getLookAngle() : null);

        if (mode == BuilderSettings.Mode.EXCHANGE) {
            if (offBlock == null || offBlock == clickedBlock) return NONE;
            BlockState state = placementState(level, player, offhand, offBlock, clicked, face);
            if (multiBlock(state)) return NONE;
            List<BlockPos> targets = spread(level, clicked, steps, max, q -> {
                BlockState s = level.getBlockState(q);
                // Never swaps out containers or unbreakable blocks (bedrock, portals...)
                return s.getBlock() == clickedBlock && level.getBlockEntity(q) == null && s.getDestroySpeed(level, q) >= 0
                        && level.getBlockState(q.relative(face)).canBeReplaced() && state.canSurvive(level, q);
            });
            return targets.isEmpty() ? NONE : new BuilderPlan(targets, state, offBlock.asItem(), true);
        }

        Block block = offBlock != null ? offBlock : clickedBlock;
        Item item = block.asItem();
        if (item == Items.AIR) return NONE;
        BlockState state = offBlock != null ? placementState(level, player, offhand, offBlock, clicked, face) : dry(clickedState);
        if (multiBlock(state)) return NONE;
        List<BlockPos> bases = spread(level, clicked, steps, max, q -> {
            if (level.getBlockState(q).getBlock() != clickedBlock) return false;
            BlockPos target = q.relative(face);
            return canPlace(level, player, target, state);
        });
        List<BlockPos> targets = new ArrayList<>(bases.size());
        for (BlockPos q : bases) targets.add(q.relative(face));
        return targets.isEmpty() ? NONE : new BuilderPlan(targets, state, item, false);
    }

    /** Whether {@code state} can go at {@code pos}: the space is free (air, water, grass...), it can stand there, and no one is in the way. */
    public static boolean canPlace(Level level, Player player, BlockPos pos, BlockState state) {
        return level.isInWorldBounds(pos) && level.getBlockState(pos).canBeReplaced() && state.canSurvive(level, pos)
                && level.isUnobstructed(state, pos, CollisionContext.of(player));
    }

    /** Connected positions from {@code start} in the given directions that pass {@code ok}, nearest first, at most {@code max}. */
    private static List<BlockPos> spread(Level level, BlockPos start, List<Direction> steps, int max, java.util.function.Predicate<BlockPos> ok) {
        List<BlockPos> out = new ArrayList<>();
        if (!ok.test(start)) return out;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos q = queue.poll();
            out.add(q);
            for (Direction d : steps) {
                BlockPos n = q.relative(d);
                if (seen.add(n) && ok.test(n)) queue.add(n);
            }
        }
        return out;
    }

    /**
     * The directions it may spread in across the clicked face: the face's two axes, less those the plane lock rules out;
     * for Line, only the one of them most along {@code look}.
     */
    static List<Direction> steps(Direction face, BuilderSettings.Plane plane, @Nullable Vec3 look) {
        List<Direction.Axis> axes = new ArrayList<>();
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis == face.getAxis()) continue;
            if (plane == BuilderSettings.Plane.HORIZONTAL && axis == Direction.Axis.Y) continue;
            if (plane == BuilderSettings.Plane.VERTICAL && axis != Direction.Axis.Y) continue;
            axes.add(axis);
        }
        if (look != null && axes.size() > 1) {
            Direction.Axis best = axes.getFirst();
            for (Direction.Axis axis : axes) if (Math.abs(look.get(axis)) > Math.abs(look.get(best))) best = axis;
            axes = List.of(best);
        }
        List<Direction> steps = new ArrayList<>();
        for (Direction.Axis axis : axes) {
            steps.add(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
            steps.add(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        }
        return steps;
    }

    /** Doors, beds, tall plants and chests that join up: blocks made of two parts, which the builder leaves alone. */
    private static boolean multiBlock(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE);
    }

    /** A copy of the clicked block, facing the same way, but never placed full of water. */
    private static BlockState dry(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state;
    }

    /** How the offhand block would sit if placed by hand against the clicked face. */
    private static BlockState placementState(Level level, Player player, ItemStack stack, Block block, BlockPos clicked, Direction face) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).relative(face, 0.5), face, clicked, false);
        BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.OFF_HAND, stack, hit));
        return dry(state == null ? block.defaultBlockState() : state);
    }
}
