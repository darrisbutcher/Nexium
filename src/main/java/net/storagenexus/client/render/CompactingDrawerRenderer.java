package net.storagenexus.client.render;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.util.Fmt;

public class CompactingDrawerRenderer extends AbstractFrontRenderer<CompactingDrawerBlockEntity> {
    public CompactingDrawerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void fill(CompactingDrawerBlockEntity be, FrontRenderState state) {
        state.slots = 3;
        for (int i = 0; i < 3; i++) {
            ItemVariant v = be.tier(i);
            setItem(be, state, i, v.isBlank() ? ItemStack.EMPTY : v.toStack(), Fmt.compact(be.amountOf(i)));
        }
    }
}
