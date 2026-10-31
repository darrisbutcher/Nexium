package net.storagenexus.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.util.SyncedBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Shared link id + display name (synced to clients for the floating label). */
public abstract class LinkedBlockEntity extends SyncedBlockEntity {
    protected @Nullable UUID linkId;
    protected String displayName = "";

    protected LinkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public @Nullable UUID linkId() { return linkId; }
    public String displayName() { return displayName; }

    public GlobalPos globalPos() {
        return GlobalPos.of(level.dimension(), worldPosition);
    }

    protected @Nullable LinkRegistry registry() {
        return level instanceof ServerLevel s ? LinkRegistry.get(s.getServer()) : null;
    }

    public void setLink(@Nullable UUID id) {
        linkId = id;
        refreshName();
        sync();
    }

    /** Re-reads the name from the registry; returns true if it changed. */
    protected boolean refreshName() {
        LinkRegistry reg = registry();
        if (reg == null) return false;
        String name = reg.displayName(linkId, this instanceof DimensionalOutputBlockEntity ? globalPos() : null);
        if (name.equals(displayName)) return false;
        displayName = name;
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (linkId != null) output.store("link", UUIDUtil.CODEC, linkId);
        output.putString("name", displayName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        linkId = input.read("link", UUIDUtil.CODEC).orElse(null);
        displayName = input.getStringOr("name", "");
    }
}
