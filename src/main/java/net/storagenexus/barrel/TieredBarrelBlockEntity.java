package net.storagenexus.barrel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.registry.ModBlockEntities;

public class TieredBarrelBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items;

    public TieredBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIERED_BARREL, pos, state);
        int slots = state.getBlock() instanceof TieredBarrelBlock b ? b.tier().slots() : BarrelTier.IRON.slots();
        items = NonNullList.withSize(slots, ItemStack.EMPTY);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BarrelMenu(containerId, inventory, this, items.size());
    }
}
