package net.storagenexus.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/** Keeps the 3x3 chunks around it loaded, always or only while a tablet is using them. */
public class DimensionalAnchorBlockEntity extends BlockEntity {
    private boolean onDemand;
    private boolean active;

    public DimensionalAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_ANCHOR, pos, state);
    }

    public boolean isOnDemand() { return onDemand; }

    /** Called when placed. */
    public void activate() {
        if (!(level instanceof ServerLevel server)) return;
        active = true;
        AnchorRegistry reg = AnchorRegistry.get(server.getServer());
        reg.put(GlobalPos.of(server.dimension(), worldPosition), onDemand);
        reg.refreshArea(server, worldPosition);
        setChanged();
    }

    public void toggleMode() {
        if (!(level instanceof ServerLevel server)) return;
        onDemand = !onDemand;
        AnchorRegistry reg = AnchorRegistry.get(server.getServer());
        reg.put(GlobalPos.of(server.dimension(), worldPosition), onDemand);
        reg.refreshArea(server, worldPosition);
        setChanged();
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
        output.putBoolean("on_demand", onDemand);
        output.putBoolean("active", active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        onDemand = input.getBooleanOr("on_demand", false);
        active = input.getBooleanOr("active", false);
    }
}
