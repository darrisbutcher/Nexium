package net.storagenexus.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/**
 * Keeps the 3x3 chunks around it loaded, all the time. When placed, and when right-clicked, it outlines those chunks for a
 * minute (right-click again to hide it), like the Drawer Controller shows its reach.
 */
public class DimensionalAnchorBlockEntity extends BlockEntity {
    /** How long the chunk outline stays up. */
    public static final int DISPLAY_TICKS = 60 * 20;
    /** Particles last about 3 seconds; redraw a little sooner so the outline stays solid. */
    private static final int REDRAW_TICKS = 40;
    /** The corner posts reach this far below and above the anchor. */
    private static final int POST_DOWN = 4, POST_UP = 12;

    private boolean active;
    private long displayUntil = -1;

    public DimensionalAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_ANCHOR, pos, state);
    }

    /** Called when placed: starts loading its chunks. */
    public void activate() {
        if (!(level instanceof ServerLevel server)) return;
        active = true;
        AnchorRegistry reg = AnchorRegistry.get(server.getServer());
        reg.put(GlobalPos.of(server.dimension(), worldPosition));
        reg.refreshArea(server, worldPosition);
        setChanged();
    }

    /** Starts showing the loaded chunks for a minute, or stops if they are showing. Returns whether they are now shown. */
    public boolean toggleDisplay(ServerLevel level) {
        long now = level.getGameTime();
        if (displayUntil > now) {
            displayUntil = -1;
            return false;
        }
        displayUntil = now + DISPLAY_TICKS;
        showChunks(level);
        return true;
    }

    public boolean isShowing(Level level) {
        return displayUntil > level.getGameTime();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalAnchorBlockEntity be) {
        long now = level.getGameTime();
        if (be.displayUntil > now && (be.displayUntil - now) % REDRAW_TICKS == 0 && level instanceof ServerLevel server) {
            be.showChunks(server);
        }
    }

    /**
     * Outlines the 3x3 chunks with particles (about 3 seconds per call): the chunk borders as a grid at the anchor's height,
     * and posts at the four outer corners so the area can be found from below or above.
     */
    public void showChunks(ServerLevel level) {
        ChunkPos center = ChunkPos.containing(worldPosition);
        int minX = (center.x() - 1) * 16, minZ = (center.z() - 1) * 16;
        double y = worldPosition.getY() + 0.5;
        for (int line = 0; line <= 3; line++) {
            int fixed = line * 16;
            for (int t = 0; t <= 48; t++) {
                particle(level, minX + fixed, y, minZ + t);
                particle(level, minX + t, y, minZ + fixed);
            }
        }
        for (int cx = 0; cx <= 48; cx += 48) {
            for (int cz = 0; cz <= 48; cz += 48) {
                for (int dy = -POST_DOWN; dy <= POST_UP; dy++) particle(level, minX + cx, y + dy, minZ + cz);
            }
        }
    }

    private static void particle(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.END_ROD, true, true, x, y, z, 1, 0, 0, 0, 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && active) {
            AnchorRegistry reg = AnchorRegistry.get(server.getServer());
            reg.remove(GlobalPos.of(server.dimension(), pos));
            reg.refreshArea(server, pos);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("active", active);
    }

    /** Anchors saved before 1.13.0 also have an {@code on_demand} flag; it is ignored, so they now always load. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        active = input.getBooleanOr("active", false);
    }
}
