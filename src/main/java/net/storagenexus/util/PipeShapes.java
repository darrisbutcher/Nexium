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
        this(min, max, min, max);
    }

    /** A core from {@code min} to {@code max} with thinner (or thicker) arms from {@code armMin} to {@code armMax}. */
    public PipeShapes(double min, double max, double armMin, double armMax) {
        core = Block.box(min, min, min, max, max, max);
        double aMin = armMin, aMax = armMax;
        arms[Direction.DOWN.get3DDataValue()] = Block.box(aMin, 0, aMin, aMax, min, aMax);
        arms[Direction.UP.get3DDataValue()] = Block.box(aMin, max, aMin, aMax, 16, aMax);
        arms[Direction.NORTH.get3DDataValue()] = Block.box(aMin, aMin, 0, aMax, aMax, min);
        arms[Direction.SOUTH.get3DDataValue()] = Block.box(aMin, aMin, max, aMax, aMax, 16);
        arms[Direction.WEST.get3DDataValue()] = Block.box(0, aMin, aMin, min, aMax, aMax);
        arms[Direction.EAST.get3DDataValue()] = Block.box(max, aMin, aMin, 16, aMax, aMax);
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
