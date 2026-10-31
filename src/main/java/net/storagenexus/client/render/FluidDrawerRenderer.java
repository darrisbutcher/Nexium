package net.storagenexus.client.render;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.util.Fmt;

/** Shows the fluid's bucket and the amount on each fluid drawer slot. */
public class FluidDrawerRenderer extends AbstractFrontRenderer<FluidDrawerBlockEntity> {
    public FluidDrawerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void fill(FluidDrawerBlockEntity be, FrontRenderState state) {
        state.slots = be.slotCount();
        for (int i = 0; i < be.slotCount(); i++) {
            FluidDrawerBlockEntity.FluidSlot slot = be.slot(i);
            FluidVariant v = slot.displayVariant();
            ItemStack icon = v.isBlank() || v.getFluid().getBucket() == Items.AIR ? ItemStack.EMPTY : new ItemStack(v.getFluid().getBucket());
            setItem(be, state, i, icon, v.isBlank() ? "" : Fmt.fluid(slot.getAmount()));
            if (!v.isBlank()) state.fill[i] = fraction(slot.getAmount(), slot.capacity());
        }
    }
}
