package net.storagenexus.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;

/** Holds the floor name typed into the elevator screen; empty means "Floor N". The name is lost when the block is broken. */
public class ElevatorBlockEntity extends BlockEntity {
    public static final int MAX_NAME = 32;

    private String name = "";

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELEVATOR, pos, state);
    }

    public String floorName() {
        return name;
    }

    public void setFloorName(String value) {
        String trimmed = value.trim();
        name = trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        name = input.getStringOr("name", "");
    }
}
