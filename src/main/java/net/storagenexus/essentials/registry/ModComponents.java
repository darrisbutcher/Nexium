package net.storagenexus.essentials.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.ironfarm.IronFarmTimers;

import java.util.function.UnaryOperator;

public final class ModComponents {
    /** Magnet range in blocks. */
    public static final DataComponentType<Integer> MAGNET_RANGE = register("magnet_range",
            b -> b.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Whether a magnet is switched on (the hotkey flips it). */
    public static final DataComponentType<Boolean> MAGNET_ACTIVE = register("magnet_active",
            b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** The mob stored in a Captured Mob item. */
    public static final DataComponentType<CapturedMob> CAPTURED_MOB = register("captured_mob",
            b -> b.persistent(CapturedMob.CODEC).networkSynchronized(CapturedMob.STREAM_CODEC));

    /** An Iron Farm's boost and cooldown timers, kept in the dropped item. */
    public static final DataComponentType<IronFarmTimers> IRON_FARM_TIMERS = register("iron_farm_timers",
            b -> b.persistent(IronFarmTimers.CODEC).networkSynchronized(IronFarmTimers.STREAM_CODEC));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, NexusEssentials.id(name),
                op.apply(DataComponentType.builder()).build());
    }

    public static void init() {}

    private ModComponents() {}
}
