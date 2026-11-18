package net.nexium.client.render;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.nexium.drawer.CompactingDrawerBlockEntity;
import net.nexium.util.Fmt;

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
        // One shared pool: a single bar under the largest form
        if (!be.tier(0).isBlank()) state.fill[0] = (float) be.fillFraction();
    }
}
