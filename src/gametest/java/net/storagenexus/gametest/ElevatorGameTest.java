package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.elevator.ElevatorBlockEntity;
import net.storagenexus.elevator.Elevators;
import net.storagenexus.elevator.Floor;
import net.storagenexus.registry.ModBlocks;

import java.util.List;

/** Elevators: floors in a column, names, going between them, and what stops a ride. */
public class ElevatorGameTest {
    /** Three elevators in one column, 3 and 6 blocks above it. Returns the lowest one's absolute position. */
    private static BlockPos build(GameTestHelper h) {
        BlockPos low = h.absolutePos(new BlockPos(1, 1, 1));
        for (int i = 0; i < 3; i++) h.getLevel().setBlockAndUpdate(low.above(i * 3), ModBlocks.ELEVATOR.defaultBlockState());
        return low;
    }

    private static ServerPlayer rider(GameTestHelper h, BlockPos at) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.snapTo(at.getX() + 0.5, at.getY() + 0.25, at.getZ() + 0.5, 0.0F, 0.0F);
        return player;
    }

    @GameTest
    public void floorsAreNumberedFromTheBottomAndCanBeNamed(GameTestHelper h) {
        BlockPos low = build(h);
        List<Floor> floors = Elevators.floors(h.getLevel(), low.above(3));
        h.assertValueEqual(floors.size(), 3, "three floors in the column");
        h.assertValueEqual(floors.get(0).y(), low.getY(), "lowest first");
        h.assertValueEqual(floors.get(0).name(), "Floor 1", "numbered from the bottom");
        h.assertValueEqual(floors.get(2).name(), "Floor 3", "numbered from the bottom");
        ServerPlayer player = rider(h, low.above(3));
        Elevators.rename(player, low.above(3), "  Mine entrance  ");
        h.assertValueEqual(Elevators.floors(h.getLevel(), low).get(1).name(), "Mine entrance", "the typed name, trimmed");
        Elevators.rename(player, low.above(3), "");
        h.assertValueEqual(Elevators.floors(h.getLevel(), low).get(1).name(), "Floor 2", "an empty name goes back to the number");
        // A name survives saving
        ElevatorBlockEntity be = (ElevatorBlockEntity) h.getLevel().getBlockEntity(low.above(6));
        be.setFloorName("Roof");
        h.assertValueEqual(be.floorName(), "Roof", "name stored");
        h.succeed();
    }

    @GameTest
    public void goingToAnotherFloorMovesThePlayerUpAndDown(GameTestHelper h) {
        BlockPos low = build(h);
        ServerPlayer player = rider(h, low);
        h.assertTrue(Elevators.go(player, low, low.getY() + 3), "up to the next floor");
        h.assertTrue(Math.abs(player.getY() - (low.getY() + 3.25)) < 0.01, "standing on that platform, got y " + player.getY());
        h.assertTrue(Math.abs(player.getX() - (low.getX() + 0.5)) < 0.01 && Math.abs(player.getZ() - (low.getZ() + 0.5)) < 0.01, "same spot in the column");
        h.assertTrue(Elevators.go(player, low.above(3), low.getY()), "and back down");
        player.snapTo(low.getX() + 2.5, low.getY() + 0.25, low.getZ() + 0.5, 0.0F, 0.0F); // standing two blocks beside the platform
        h.assertTrue(Elevators.go(player, low, low.getY() + 3), "from beside it");
        h.assertTrue(player.getX() < low.getX() + 1.0, "landed on the platform, not beside it, got x " + player.getX());
        h.assertTrue(Elevators.go(player, low.above(3), low.getY()), "down again");
        h.assertTrue(Math.abs(player.getY() - (low.getY() + 0.25)) < 0.01, "standing on the bottom platform");
        h.succeed();
    }

    @GameTest
    public void aFloorWithNoRoomIsSkippedAndShownBlocked(GameTestHelper h) {
        BlockPos low = build(h);
        h.getLevel().setBlockAndUpdate(low.above(3 + 2), Blocks.STONE.defaultBlockState()); // two above the middle floor
        h.assertTrue(Elevators.floors(h.getLevel(), low).get(1).blocked(), "the middle floor is marked blocked");
        h.assertFalse(Elevators.floors(h.getLevel(), low).get(0).blocked(), "the others are fine");
        ServerPlayer player = rider(h, low);
        h.assertFalse(Elevators.go(player, low, low.getY() + 3), "cannot go where there is no room");
        h.assertTrue(player.getY() < low.getY() + 1, "the player stayed put");
        h.succeed();
    }

    @GameTest
    public void aRideNeedsAnElevatorAndAPlayerNearby(GameTestHelper h) {
        BlockPos low = build(h);
        ServerPlayer player = rider(h, low);
        h.assertFalse(Elevators.go(player, low, low.getY() + 1), "no elevator at that height");
        h.assertFalse(Elevators.go(player, low, low.getY()), "not to the floor you are on");
        ServerPlayer far = rider(h, low.offset(20, 0, 0));
        h.assertFalse(Elevators.go(far, low, low.getY() + 3), "too far from the elevator whose screen is open");
        h.assertFalse(Elevators.go(player, low.above(1), low.getY() + 3), "the position must be an elevator");
        h.succeed();
    }
}
