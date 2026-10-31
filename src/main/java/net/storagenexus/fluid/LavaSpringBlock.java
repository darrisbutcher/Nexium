package net.storagenexus.fluid;

import net.minecraft.world.level.block.Block;

/** An endless lava source: a Fluid Intake next to it pulls lava from it without ever using it up. */
public class LavaSpringBlock extends Block {
    public LavaSpringBlock(Properties properties) {
        super(properties);
    }
}
