package net.nexium.essentials.placement;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Diagonal placement, on the server: with the Diagonal Placement key held, a right-click on a block face puts the held block
 * one step out from that face and one step over toward the nearest edge of the face you were aiming at (aim near the top of a
 * wall and it lands diagonally up and out). The new block touches the one you clicked along an edge only, and may float.
 *
 * <p>Aim at the middle of the face instead (within {@value #CENTER} of its centre each way) and the block goes straight across
 * from it with one block of gap: two out from the face, nothing between.
 *
 * <p>Vanilla only places a block against a face, so the client sends this as its own packet instead of a normal click and the
 * server does the placing: it checks the click as vanilla would (reach, the hit being on the clicked block), that the cell is in
 * the world and replaceable and that the player may build there, then places through the item's own {@code useOn} so orientation,
 * sounds, game events and consuming the item all work as for any placement.
 */
public final class DiagonalPlacement {
    /** How far from the middle of a face (as a fraction of a block, each way) a click still counts as aimed at the middle. */
    public static final double CENTER = 0.25;

    /**
     * Where a placement goes: {@code spot}, and the {@code edge} of the clicked face it was worked out from, or null for the
     * straight-across placement with a gap (aimed at the middle of the face).
     */
    public record Plan(BlockPos spot, @Nullable Direction edge) {
        public boolean gap() {
            return edge == null;
        }
    }

    /**
     * What a click on {@code face} of {@code clicked} at {@code hit} places: aimed at the middle of the face, the cell straight
     * across from it with one block of gap (two out from the face); otherwise the cell one out and one toward the nearest edge
     * ({@link #target}).
     */
    public static Plan plan(BlockPos clicked, Direction face, Vec3 hit) {
        double[] offset = {hit.x - (clicked.getX() + 0.5), hit.y - (clicked.getY() + 0.5), hit.z - (clicked.getZ() + 0.5)};
        boolean middle = true;
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != face.getAxis() && Math.abs(offset[axis.ordinal()]) >= CENTER) middle = false;
        }
        if (middle) return new Plan(clicked.relative(face, 2), null);
        Direction edge = edge(offset, face);
        return new Plan(clicked.relative(face).relative(edge), edge);
    }

    /** The side of the face the hit point is farthest from the face's centre toward (a tie goes to the X axis, then Y, then Z). */
    private static Direction edge(double[] offset, Direction face) {
        Direction.Axis best = null;
        double bestDistance = -1.0;
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis == face.getAxis()) continue;
            double distance = Math.abs(offset[axis.ordinal()]);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = axis;
            }
        }
        return Direction.fromAxisAndDirection(best, offset[best.ordinal()] < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
    }

    /** The cell a placement goes in for this click: the spot of its {@link #plan}. */
    public static BlockPos target(BlockPos clicked, Direction face, Vec3 hit) {
        return plan(clicked, face, hit).spot();
    }

    /**
     * Places the block in this hand, diagonally or straight across with a gap (see {@link #plan}), for a click on {@code face} of {@code clicked} at {@code hit}. Returns whether
     * something was placed. Nothing is when the player can't build or the hand holds no block, the click is out of reach or
     * not on a real block, or the cell is outside the world, taken by another block, protected or refused by the block.
     */
    public static boolean place(ServerPlayer player, InteractionHand hand, BlockPos clicked, Direction face, Vec3 hit) {
        if (player.isSpectator() || !player.mayBuild()) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return false;
        ServerLevel level = player.level();

        // The click has to be one vanilla would take: close enough, on a real block, with the hit point on that block
        if (!level.isInWorldBounds(clicked) || !level.hasChunkAt(clicked) || !player.isWithinBlockInteractionRange(clicked, 1.0)) return false;
        if (level.getBlockState(clicked).isAir()) return false;
        Vec3 local = hit.subtract(clicked.getX(), clicked.getY(), clicked.getZ());
        double slack = 1.0E-4;
        if (local.x < -slack || local.x > 1 + slack || local.y < -slack || local.y > 1 + slack || local.z < -slack || local.z > 1 + slack) return false;

        BlockPos spot = target(clicked, face, hit);
        if (!level.isInWorldBounds(spot) || !level.hasChunkAt(spot)) return false;
        // A replaceable cell (air, water, tall grass) is placed in; vanilla would put the block next to any other
        if (!level.getBlockState(spot).canBeReplaced()) return false;
        if (!level.mayInteract(player, spot) || !player.mayUseItemAt(spot, face, stack)) return false;

        // As a normal click, keeping where in the block it was aimed (so a slab or stair goes top or bottom to match)
        Vec3 click = new Vec3(spot.getX() + local.x, spot.getY() + local.y, spot.getZ() + local.z);
        BlockHitResult hitResult = new BlockHitResult(click, face, spot, false);
        // Other mods that guard blocks (claims and the like) listen here; if one says no, so do we
        if (UseBlockCallback.EVENT.invoker().interact(player, level, hand, hitResult) != InteractionResult.PASS) return false;
        // Read before placing: placing the last item of a stack leaves it empty
        SwingAnimation swing = stack.getInteractAnimation();
        InteractionResult result = stack.useOn(new UseOnContext(player, hand, hitResult));
        if (!result.consumesAction()) return false;
        player.swing(hand, swing, true);
        return true;
    }

    private DiagonalPlacement() {}
}
