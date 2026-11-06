package net.storagenexus.elevator;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The server side of elevators. An elevator links to every other elevator in the same column (same x and z), anywhere
 * from the bottom of the world to the top; the column is always inside one chunk, so every floor is loaded whenever the
 * one you stand on is. Right-click an elevator to pick a floor from a list.
 */
public final class Elevators {
    /** How far the player may be from the elevator whose screen is open. */
    private static final double REACH_SQR = 8.0 * 8.0;

    public static boolean isElevator(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ElevatorBlock;
    }

    /** Room to stand on the elevator: the two blocks above it have no collision and are not lava. */
    public static boolean hasRoom(Level level, BlockPos elevator) {
        for (int i = 1; i <= 2; i++) {
            BlockPos p = elevator.above(i);
            BlockState state = level.getBlockState(p);
            if (!state.getCollisionShape(level, p).isEmpty() || state.getFluidState().is(FluidTags.LAVA)) return false;
        }
        return true;
    }

    /** Every elevator in the column of pos, lowest first. Unnamed ones are "Floor N", counted from the bottom. */
    public static List<Floor> floors(Level level, BlockPos pos) {
        List<Floor> floors = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), 0, pos.getZ());
        for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
            cursor.setY(y);
            if (!isElevator(level, cursor)) continue;
            String name = level.getBlockEntity(cursor) instanceof ElevatorBlockEntity be ? be.floorName() : "";
            if (name.isEmpty()) name = Component.translatable("screen.storagenexus.elevator.floor", floors.size() + 1).getString();
            floors.add(new Floor(y, name, !hasRoom(level, cursor)));
        }
        return floors;
    }

    /** Opens the elevator screen for this player. */
    public static void open(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        if (!isElevator(level, pos)) return;
        List<Floor> floors = floors(level, pos);
        String own = level.getBlockEntity(pos) instanceof ElevatorBlockEntity be ? be.floorName() : "";
        ServerPlayNetworking.send(player, new ElevatorOpenPayload(pos, own, floors));
    }

    private static boolean valid(ServerPlayer player, BlockPos pos) {
        return isElevator(player.level(), pos) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= REACH_SQR;
    }

    /** Takes the player from the elevator at pos to the one at height y in the same column. Returns whether it worked. */
    public static boolean go(ServerPlayer player, BlockPos pos, int y) {
        if (!valid(player, pos) || y == pos.getY()) return false;
        ServerLevel level = player.level();
        BlockPos target = new BlockPos(pos.getX(), y, pos.getZ());
        if (!isElevator(level, target)) return false;
        if (!hasRoom(level, target)) {
            player.sendOverlayMessage(Component.translatable("message.storagenexus.elevator.blocked").withStyle(ChatFormatting.RED));
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.3, 0.6, 0.3, 0.2);
        // The same spot in the column, or the nearest spot on the platform if the player was beside it; standing on the
        // quarter-block platform, with no fall damage and no leftover speed
        double x = net.minecraft.util.Mth.clamp(player.getX(), pos.getX() + 0.15, pos.getX() + 0.85);
        double z = net.minecraft.util.Mth.clamp(player.getZ(), pos.getZ() + 0.15, pos.getZ() + 0.85);
        player.teleportTo(level, x, y + 0.25, z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        player.resetFallDistance();
        player.setDeltaMovement(0, 0, 0);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        level.playSound(null, target, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.6F);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.3, 0.6, 0.3, 0.2);
        return true;
    }

    /** Names the floor of the elevator at pos. */
    public static void rename(ServerPlayer player, BlockPos pos, String name) {
        if (!valid(player, pos)) return;
        if (player.level().getBlockEntity(pos) instanceof ElevatorBlockEntity be) be.setFloorName(name);
    }

    private Elevators() {}
}
