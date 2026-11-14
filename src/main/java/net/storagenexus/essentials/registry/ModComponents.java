package net.storagenexus.essentials.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.backpack.BackpackContents;
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

    /** How far a farm got towards its next cycle, kept in the dropped item. */
    public static final DataComponentType<Integer> FARM_PROGRESS = register("farm_progress",
            b -> b.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Which mode a Gourd and Cactus Farm or a Generator was on, kept in the dropped item. */
    public static final DataComponentType<Integer> FARM_MODE = register("farm_mode",
            b -> b.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** What a Backpack holds (see BackpackContents): every occupied slot as an item, a count and a slot number. */
    public static final DataComponentType<BackpackContents.Saved> BACKPACK_ITEMS = register("backpack_items",
            b -> b.persistent(BackpackContents.Saved.CODEC).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(BackpackContents.Saved.CODEC)));

    /** The modules installed in a Backpack, one per slot (see BackpackModules). */
    public static final DataComponentType<ItemContainerContents> BACKPACK_MODULES = register("backpack_modules",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, NexusEssentials.id(name),
                op.apply(DataComponentType.builder()).build());
    }

    public static void init() {}

    private ModComponents() {}
}
