package net.storagenexus.client.render;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.util.Fmt;

public class DrawerRenderer extends AbstractFrontRenderer<DrawerBlockEntity> {
    public DrawerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void fill(DrawerBlockEntity be, FrontRenderState state) {
        state.slots = be.slotCount();
        for (int i = 0; i < be.slotCount(); i++) {
            DrawerBlockEntity.DrawerSlot slot = be.slot(i);
            ItemVariant v = slot.displayVariant();
            setItem(be, state, i, v.isBlank() ? ItemStack.EMPTY : v.toStack(), Fmt.compact(slot.getAmount()));
            if (!v.isBlank()) state.fill[i] = fraction(slot.getAmount(), slot.capacity());
        }
    }
}
