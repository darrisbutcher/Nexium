package net.storagenexus.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

/** What a drawer front shows: up to four items with a count under each. */
public class FrontRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int frontLight;
    public int slots;
    public final ItemStackRenderState[] items = {
            new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()
    };
    public final String[] counts = new String[4];
    public final boolean[] present = new boolean[4];
}
