package net.storagenexus.drawer;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Hit testing for drawer fronts, double-click detection and left-click extraction. */
public final class DrawerInteraction {
    private static final int DOUBLE_CLICK_TICKS = 10;
    /**
     * While the attack button is held, the client re-sends the attack every tick, so a
     * single click arrives 2-3 times. Attacks on the same block closer together than this
     * count as the same click.
     */
    private static final int HELD_ATTACK_TICKS = 3;
    private static final Map<UUID, Click> LAST_CLICK = new HashMap<>();
    private static final Map<UUID, Click> LAST_ATTACK = new HashMap<>();

    private record Click(BlockPos pos, long time) {}

    public static void init() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FacingDrawerBlock)) return InteractionResult.PASS;
            if (direction != state.getValue(BlockStateProperties.HORIZONTAL_FACING)) return InteractionResult.PASS;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ItemDrawer drawer)) return InteractionResult.PASS;
            if (!level.isClientSide() && isNewAttack(player, pos, level.getGameTime())) {
                int slot = slotFromLook(player, state, pos, drawer.slotCount());
                if (slot >= 0) drawer.extractToPlayer(slot, player, player.isShiftKeyDown());
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** False for the repeats the client sends while the attack button stays held. Records the attack. */
    static boolean isNewAttack(Player player, BlockPos pos, long gameTime) {
        Click last = LAST_ATTACK.put(player.getUUID(), new Click(pos.immutable(), gameTime));
        return last == null || !last.pos.equals(pos) || gameTime - last.time > HELD_ATTACK_TICKS;
    }

    /** True when the same player clicked the same block within half a second. Records the click. */
    public static boolean isDoubleClick(Player player, BlockPos pos, long gameTime) {
        Click last = LAST_CLICK.get(player.getUUID());
        LAST_CLICK.put(player.getUUID(), new Click(pos.immutable(), gameTime));
        return last != null && last.pos.equals(pos) && gameTime - last.time <= DOUBLE_CLICK_TICKS;
    }

    public static int hitSlot(BlockState state, BlockHitResult hit, int slots) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (hit.getDirection() != facing) return -1;
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        return slotAt(slots, horizontal(facing, local), local.y);
    }

    private static int slotFromLook(Player player, BlockState state, BlockPos pos, int slots) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, false);
        if (hit instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos)) {
            return hitSlot(state, bhr, slots);
        }
        return 0;
    }

    /** 0..1 across the drawer front, left to right as the player sees it. */
    public static double horizontal(Direction facing, Vec3 local) {
        Direction right = facing.getCounterClockWise();
        if (right.getStepX() != 0) return right.getStepX() > 0 ? local.x : 1 - local.x;
        return right.getStepZ() > 0 ? local.z : 1 - local.z;
    }

    /**
     * Slot layout (as seen from the front):
     * 1 slot: whole face. 2 slots: top 0, bottom 1. 4 slots: 0 1 / 2 3.
     * 3 slots (compacting): top 0 (largest form), bottom-left 1, bottom-right 2 (smallest).
     */
    public static int slotAt(int slots, double u, double v) {
        boolean top = v >= 0.5;
        boolean right = u >= 0.5;
        return switch (slots) {
            case 2 -> top ? 0 : 1;
            case 3 -> top ? 0 : (right ? 2 : 1);
            case 4 -> (top ? 0 : 2) + (right ? 1 : 0);
            default -> 0;
        };
    }

    /** Gives a stack to the player, dropping whatever does not fit. */
    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    private DrawerInteraction() {}
}
