package net.storagenexus.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FluidItems {
    /** The fluid a bucket (or any fluid container item) holds, or null. */
    public static @Nullable FluidVariant fluidIn(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Storage<FluidVariant> storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        if (storage == null) return null;
        for (StorageView<FluidVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0) return view.getResource();
        }
        return null;
    }

    private FluidItems() {}
}
