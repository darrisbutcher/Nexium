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
    /** How full each section is, 0 to 1, shown as a bar under the count; negative for no bar. */
    public final float[] fill = new float[4];
    /** A Lock Upgrade is installed: a small padlock shows in the top-right corner. */
    public boolean locked;
    public final ItemStackRenderState lockIcon = new ItemStackRenderState();
    /** A Void Upgrade is installed: a small marker shows in the top-left corner. */
    public boolean voiding;
    public final ItemStackRenderState voidIcon = new ItemStackRenderState();
}
