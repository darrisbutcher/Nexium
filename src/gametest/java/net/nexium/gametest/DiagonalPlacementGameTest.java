package net.nexium.gametest;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.placement.DiagonalPlacePayload;
import net.nexium.essentials.placement.DiagonalPlacement;

/**
 * Diagonal placement: the cell it picks for a click on any face (one out from the face and one toward the nearest edge of it), the
 * block going there with the item used up (or not, in creative), landing in a replaceable cell and refusing a taken one, keeping the
 * aimed half of a slab and the axis of a log, using the offhand, and every refusal the server makes of a bad packet.
 */
public class DiagonalPlacementGameTest {
    /** The block clicked in the tests. */
    private static final BlockPos P = new BlockPos(3, 2, 3);

    private static ServerPlayer player(GameTestHelper h, ItemStack held) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 6))));
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /** A point on the clicked block, given as how far along each axis (0 to 1). */
    private static Vec3 at(GameTestHelper h, double fx, double fy, double fz) {
        BlockPos abs = h.absolutePos(P);
        return new Vec3(abs.getX() + fx, abs.getY() + fy, abs.getZ() + fz);
    }

    private static boolean place(GameTestHelper h, ServerPlayer player, Direction face, Vec3 hit) {
        return DiagonalPlacement.place(player, InteractionHand.MAIN_HAND, h.absolutePos(P), face, hit);
    }

    // ---------------------------------------------------------------- which cell

    private static void expect(GameTestHelper h, BlockPos clicked, Direction face, double fx, double fy, double fz, BlockPos expected, String what) {
        Vec3 hit = new Vec3(clicked.getX() + fx, clicked.getY() + fy, clicked.getZ() + fz);
        BlockPos got = DiagonalPlacement.target(clicked, face, hit);
        h.assertTrue(got.equals(expected), what + ": expected " + expected.toShortString() + ", got " + got.toShortString());
    }

    @GameTest
    public void theCellIsOneOutAndOneTowardTheNearestEdge(GameTestHelper h) {
        BlockPos c = new BlockPos(10, 20, 30);
        // North face (the -Z side): up and down, west and east
        expect(h, c, Direction.NORTH, 0.5, 0.9, 0.0, c.offset(0, 1, -1), "aiming high on a north face goes up and out");
        expect(h, c, Direction.NORTH, 0.5, 0.1, 0.0, c.offset(0, -1, -1), "low goes down and out");
        expect(h, c, Direction.NORTH, 0.05, 0.5, 0.0, c.offset(-1, 0, -1), "the left edge goes west and out");
        expect(h, c, Direction.NORTH, 0.95, 0.55, 0.0, c.offset(1, 0, -1), "the right edge goes east and out");
        // South, east and west faces
        expect(h, c, Direction.SOUTH, 0.5, 0.9, 1.0, c.offset(0, 1, 1), "aiming high on a south face");
        expect(h, c, Direction.SOUTH, 0.1, 0.5, 1.0, c.offset(-1, 0, 1), "the west edge of a south face");
        expect(h, c, Direction.EAST, 1.0, 0.9, 0.5, c.offset(1, 1, 0), "aiming high on an east face");
        expect(h, c, Direction.EAST, 1.0, 0.5, 0.1, c.offset(1, 0, -1), "the north edge of an east face");
        expect(h, c, Direction.WEST, 0.0, 0.1, 0.5, c.offset(-1, -1, 0), "aiming low on a west face");
        expect(h, c, Direction.WEST, 0.0, 0.5, 0.9, c.offset(-1, 0, 1), "the south edge of a west face");
        // Top and bottom faces slide sideways
        expect(h, c, Direction.UP, 0.9, 1.0, 0.5, c.offset(1, 1, 0), "the east edge of a top face");
        expect(h, c, Direction.UP, 0.5, 1.0, 0.1, c.offset(0, 1, -1), "the north edge of a top face");
        expect(h, c, Direction.DOWN, 0.5, 0.0, 0.9, c.offset(0, -1, 1), "the south edge of a bottom face");
        expect(h, c, Direction.DOWN, 0.1, 0.0, 0.5, c.offset(-1, -1, 0), "the west edge of a bottom face");
        // The nearer edge wins: this is nearer the top than the right
        expect(h, c, Direction.NORTH, 0.8, 0.95, 0.0, c.offset(0, 1, -1), "closer to the top than the right");
        expect(h, c, Direction.NORTH, 0.97, 0.8, 0.0, c.offset(1, 0, -1), "closer to the right than the top");
        // A tie still picks a side, always the same one
        expect(h, c, Direction.NORTH, 0.75, 0.75, 0.0, c.offset(1, 0, -1), "a tie picks the X side");
        h.succeed();
    }

    @GameTest
    public void theMiddleOfAFaceIsStraightAcrossWithAOneBlockGap(GameTestHelper h) {
        BlockPos c = new BlockPos(10, 20, 30);
        expect(h, c, Direction.NORTH, 0.5, 0.5, 0.0, c.offset(0, 0, -2), "the centre of a north face: two out, no sideways step");
        expect(h, c, Direction.SOUTH, 0.5, 0.5, 1.0, c.offset(0, 0, 2), "a south face");
        expect(h, c, Direction.EAST, 1.0, 0.5, 0.5, c.offset(2, 0, 0), "an east face");
        expect(h, c, Direction.WEST, 0.0, 0.5, 0.5, c.offset(-2, 0, 0), "a west face");
        expect(h, c, Direction.UP, 0.5, 1.0, 0.5, c.offset(0, 2, 0), "a top face: two up");
        expect(h, c, Direction.DOWN, 0.5, 0.0, 0.5, c.offset(0, -2, 0), "a bottom face: two down");
        // The middle is the central half of the face each way: just inside is still the middle, the edge of that is not
        expect(h, c, Direction.NORTH, 0.26, 0.74, 0.0, c.offset(0, 0, -2), "just inside the middle zone");
        expect(h, c, Direction.NORTH, 0.74, 0.5, 0.0, c.offset(0, 0, -2), "just inside on the other side");
        expect(h, c, Direction.NORTH, 0.8, 0.5, 0.0, c.offset(1, 0, -1), "past the middle zone it is diagonal again, toward the east edge");
        expect(h, c, Direction.NORTH, 0.5, 0.2, 0.0, c.offset(0, -1, -1), "or toward the bottom edge");
        h.succeed();
    }

    @GameTest
    public void thePlanSaysWhichEdgeIsUsedOrThatItIsTheGap(GameTestHelper h) {
        BlockPos c = new BlockPos(10, 20, 30);
        DiagonalPlacement.Plan high = DiagonalPlacement.plan(c, Direction.NORTH, new Vec3(10.5, 20.9, 30.0));
        h.assertTrue(high.edge() == Direction.UP && !high.gap(), "aiming high on a north face uses the top edge");
        h.assertTrue(DiagonalPlacement.plan(c, Direction.NORTH, new Vec3(10.05, 20.5, 30.0)).edge() == Direction.WEST, "the left edge is west");
        h.assertTrue(DiagonalPlacement.plan(c, Direction.UP, new Vec3(10.5, 21.0, 30.95)).edge() == Direction.SOUTH, "a top face aimed at its south side uses the south edge");
        DiagonalPlacement.Plan middle = DiagonalPlacement.plan(c, Direction.EAST, new Vec3(11.0, 20.5, 30.5));
        h.assertTrue(middle.gap() && middle.edge() == null, "the middle has no edge: it is the gap placement");
        h.assertTrue(middle.spot().equals(c.east(2)), "and goes two out");
        h.succeed();
    }

    // ---------------------------------------------------------------- placing

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theBlockGoesDiagonallyAndOneIsUsedUp(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE, 5));
        // High on the north face: one out (north) and one up
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "it places");
        h.assertTrue(h.getBlockState(P.north().above()).is(Blocks.COBBLESTONE), "the block is diagonally up and out");
        h.assertTrue(h.getBlockState(P.north()).isAir(), "and not in the cell vanilla would use");
        h.assertTrue(h.getBlockState(P.above()).isAir(), "or above the clicked block");
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "one cobblestone is used up");
        // Low and to the side goes down and west
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.05, 0.4, 0.0)), "the west edge places too");
        h.assertTrue(h.getBlockState(P.north().west()).is(Blocks.COBBLESTONE), "diagonally west and out");
        h.assertValueEqual(player.getMainHandItem().getCount(), 3, "another is used up");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aimedAtTheMiddleTheBlockGoesStraightAcrossWithAGap(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE, 5));
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.5, 0.0)), "it places");
        h.assertTrue(h.getBlockState(P.north(2)).is(Blocks.COBBLESTONE), "the block is two out from the face, straight across");
        h.assertTrue(h.getBlockState(P.north()).isAir(), "with the cell between left empty (the gap)");
        h.assertTrue(h.getBlockState(P.north().above()).isAir() && h.getBlockState(P.north().west()).isAir(), "and nothing diagonal");
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "one cobblestone is used up");
        // The same on a top face
        h.assertTrue(place(h, player, Direction.UP, at(h, 0.5, 1.0, 0.5)), "off a top face too");
        h.assertTrue(h.getBlockState(P.above(2)).is(Blocks.COBBLESTONE) && h.getBlockState(P.above()).isAir(), "two up with a gap");
        // A taken cell refuses, as for the diagonal
        h.setBlock(P.east(2), Blocks.DIRT);
        h.assertTrue(!place(h, player, Direction.EAST, at(h, 1.0, 0.5, 0.5)), "a taken cell refuses");
        h.assertValueEqual(player.getMainHandItem().getCount(), 3, "and nothing more was used up");
        // The gap may be anything: even a solid block between does not stop it
        h.setBlock(P.west(), Blocks.STONE);
        h.assertTrue(place(h, player, Direction.WEST, at(h, 0.0, 0.5, 0.5)), "a block in the gap does not matter");
        h.assertTrue(h.getBlockState(P.west(2)).is(Blocks.COBBLESTONE), "it still goes two out");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void creativeKeepsTheStack(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE, 5));
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "it places");
        h.assertValueEqual(player.getMainHandItem().getCount(), 5, "in creative nothing is used up");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aDiagonalBlockMayFloat(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE, 2));
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "it places");
        BlockPos spot = P.north().above();
        for (Direction d : Direction.values()) {
            h.assertTrue(d != Direction.SOUTH && d != Direction.DOWN ? h.getBlockState(spot.relative(d)).isAir() : true, "nothing else touches the new block on its " + d + " side");
        }
        h.assertTrue(h.getBlockState(spot).is(Blocks.COBBLESTONE), "yet it stands there");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aReplaceableCellIsPlacedInAndATakenOneRefused(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        BlockPos spot = P.north().above();
        // Tall grass is replaced, as in vanilla
        h.setBlock(spot, Blocks.SHORT_GRASS);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE, 3));
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "it places over grass");
        h.assertTrue(h.getBlockState(spot).is(Blocks.COBBLESTONE), "the grass was replaced");
        // A solid block in the cell is not: vanilla would put the block beside it, so we do nothing
        h.setBlock(spot, Blocks.DIRT);
        h.assertTrue(!place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "a taken cell refuses");
        h.assertTrue(h.getBlockState(spot).is(Blocks.DIRT), "the dirt is still there");
        h.assertTrue(h.getBlockState(spot.north()).isAir() && h.getBlockState(P.north()).isAir(), "and nothing was put beside it");
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "only the first placement used one up");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aSlabKeepsTheHalfYouAimedAt(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.COBBLESTONE_SLAB, 4));
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "aiming high places");
        BlockState high = h.getBlockState(P.north().above());
        h.assertTrue(high.is(Blocks.COBBLESTONE_SLAB) && high.getValue(SlabBlock.TYPE) == SlabType.TOP, "a high aim gives a top slab, as in vanilla");
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.1, 0.0)), "aiming low places");
        BlockState low = h.getBlockState(P.north().below());
        h.assertTrue(low.is(Blocks.COBBLESTONE_SLAB) && low.getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "a low aim gives a bottom slab");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aLogFollowsTheFaceYouClicked(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, new ItemStack(Items.OAK_LOG, 4));
        h.assertTrue(place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "it places");
        h.assertTrue(h.getBlockState(P.north().above()).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z, "a log placed off a north face points north-south, as in vanilla");
        h.assertTrue(place(h, player, Direction.EAST, at(h, 1.0, 0.5, 0.95)), "off an east face too");
        h.assertTrue(h.getBlockState(P.east().south()).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "that one points east-west");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theOffhandWorksToo(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        ServerPlayer player = player(h, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 3));
        h.assertTrue(!place(h, player, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "the main hand is empty: nothing");
        h.assertTrue(DiagonalPlacement.place(player, InteractionHand.OFF_HAND, h.absolutePos(P), Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "the offhand places");
        h.assertValueEqual(player.getOffhandItem().getCount(), 2, "from the offhand's stack");
        h.succeed();
    }

    // ---------------------------------------------------------------- refusals

    @GameTest(environment = EssentialsEnvironment.ID)
    public void badRequestsPlaceNothing(GameTestHelper h) {
        h.setBlock(P, Blocks.STONE);
        BlockPos spot = P.north().above();

        ServerPlayer holdingTool = player(h, new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(!place(h, holdingTool, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "a tool is not a block");
        ServerPlayer holdingStick = player(h, new ItemStack(Items.STICK, 4));
        h.assertTrue(!place(h, holdingStick, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "neither is a stick");
        h.assertTrue(h.getBlockState(spot).isAir(), "so nothing is there");

        ServerPlayer adventurer = player(h, new ItemStack(Items.COBBLESTONE, 4));
        adventurer.setGameMode(GameType.ADVENTURE);
        h.assertTrue(!place(h, adventurer, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "adventure mode may not build");
        adventurer.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!place(h, adventurer, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "nor a spectator");
        h.assertValueEqual(adventurer.getMainHandItem().getCount(), 4, "and nothing is used up");

        ServerPlayer far = player(h, new ItemStack(Items.COBBLESTONE, 4));
        far.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 3)).offset(0, 0, 40)));
        h.assertTrue(!place(h, far, Direction.NORTH, at(h, 0.5, 0.9, 0.0)), "a click out of reach is refused");

        ServerPlayer near = player(h, new ItemStack(Items.COBBLESTONE, 4));
        h.assertTrue(!place(h, near, Direction.NORTH, at(h, 4.5, 0.9, 0.0)), "a hit point that is not on the clicked block is refused");
        h.assertTrue(!DiagonalPlacement.place(near, InteractionHand.MAIN_HAND, h.absolutePos(P.above(3)), Direction.NORTH,
                new Vec3(h.absolutePos(P.above(3)).getX() + 0.5, h.absolutePos(P.above(3)).getY() + 0.9, h.absolutePos(P.above(3)).getZ())), "clicking air is refused");
        h.assertTrue(!DiagonalPlacement.place(near, InteractionHand.MAIN_HAND, h.absolutePos(P).atY(2000), Direction.NORTH,
                new Vec3(h.absolutePos(P).getX() + 0.5, 2000.9, h.absolutePos(P).getZ())), "a click above the world is refused");
        h.assertValueEqual(near.getMainHandItem().getCount(), 4, "nothing was used up");
        h.assertTrue(h.getBlockState(spot).isAir() && h.getBlockState(P.north()).isAir(), "and nothing was placed");
        h.succeed();
    }

    @GameTest
    public void thePacketSurvivesTheWire(GameTestHelper h) {
        DiagonalPlacePayload sent = new DiagonalPlacePayload(new BlockPos(-5, 70, 12345), Direction.WEST, new Vec3(-4.0, 70.75, 12345.5), InteractionHand.OFF_HAND);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        DiagonalPlacePayload.STREAM_CODEC.encode(buf, sent);
        DiagonalPlacePayload got = DiagonalPlacePayload.STREAM_CODEC.decode(buf);
        h.assertTrue(got.equals(sent), "what the client sends is what the server reads: " + got);
        h.succeed();
    }
}
