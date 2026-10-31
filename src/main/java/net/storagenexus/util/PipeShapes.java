package net.storagenexus.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Core-plus-arms shapes for cables and conduits, cached by connection bitmask. */
public final class PipeShapes {
    private final VoxelShape core;
    private final VoxelShape[] arms = new VoxelShape[6];
    private final VoxelShape[] cache = new VoxelShape[64];

    public PipeShapes(double min, double max) {
        core = Block.box(min, min, min, max, max, max);
        arms[Direction.DOWN.get3DDataValue()] = Block.box(min, 0, min, max, min, max);
        arms[Direction.UP.get3DDataValue()] = Block.box(min, max, min, max, 16, max);
        arms[Direction.NORTH.get3DDataValue()] = Block.box(min, min, 0, max, max, min);
        arms[Direction.SOUTH.get3DDataValue()] = Block.box(min, min, max, max, max, 16);
        arms[Direction.WEST.get3DDataValue()] = Block.box(0, min, min, min, max, max);
        arms[Direction.EAST.get3DDataValue()] = Block.box(max, min, min, 16, max, max);
    }

    public VoxelShape get(int mask) {
        VoxelShape s = cache[mask];
        if (s == null) {
            s = core;
            for (Direction d : Direction.values()) {
                if ((mask & (1 << d.get3DDataValue())) != 0) s = Shapes.or(s, arms[d.get3DDataValue()]);
            }
            cache[mask] = s;
        }
        return s;
    }

    public VoxelShape arm(Direction d) {
        return arms[d.get3DDataValue()];
    }
}
