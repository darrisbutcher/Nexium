package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.filter.FilterData;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.util.StorageEvents;
import org.jetbrains.annotations.Nullable;

/** Priority (both connectors) and filter slot (Filtered Connector only). */
public class ConnectorBlockEntity extends BlockEntity {
    public static final int MIN_PRIORITY = -100;
    public static final int MAX_PRIORITY = 100;

    private int priority;
    private final SimpleContainer filterSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ConnectorBlockEntity.this.setChanged();
            StorageEvents.storageChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterItem;
        }
    };

    public ConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONNECTOR, pos, state);
    }

    public int priority() { return priority; }

    public void setPriority(int value) {
        priority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, value));
        setChanged();
        StorageEvents.storageChanged();
    }

    public SimpleContainer filterContainer() { return filterSlot; }

    public @Nullable FilterData filterData() {
        ItemStack s = filterSlot.getItem(0);
        return s.getItem() instanceof FilterItem fi ? fi.data(s) : null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("priority", priority);
        output.store("filter", ItemStack.OPTIONAL_CODEC, filterSlot.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        priority = input.getIntOr("priority", 0);
        filterSlot.setItem(0, input.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, filterSlot);
    }
}
