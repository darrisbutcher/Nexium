package net.storagenexus.essentials.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.fluid.HoneyFluid;

/** Fluids this mod adds: honey, which only ever lives in tanks and pipes. */
public final class ModFluids {
    /** The colour honey is drawn in where there is no bucket to show (terminal, tooltips). */
    public static final int HONEY_COLOR = 0xFFE8A62A;

    public static final HoneyFluid HONEY = Registry.register(BuiltInRegistries.FLUID, NexusEssentials.id("honey"), new HoneyFluid());

    public static void init() {
        FluidVariantAttributes.register(HONEY, new FluidVariantAttributeHandler() {
            @Override
            public Component getName(FluidVariant variant) {
                return Component.translatable("fluid.nexusessentials.honey");
            }

            @Override
            public int getAssociatedColor(FluidVariant variant) {
                return HONEY_COLOR;
            }
        });
    }

    private ModFluids() {}
}
