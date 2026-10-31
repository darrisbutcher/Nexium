package net.storagenexus.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.storagenexus.StorageNexus;
import net.storagenexus.filter.FilterData;

import java.util.UUID;
import java.util.function.UnaryOperator;

public final class ModComponents {
    /** Terminal a Storage Tablet is linked to (any dimension). */
    public static final DataComponentType<GlobalPos> TERMINAL_LINK = register("terminal_link",
            b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    /** Dimensional link a Link Card carries. */
    public static final DataComponentType<UUID> LINK_ID = register("link_id",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** Cached display name of the link on a Link Card, for the tooltip. */
    public static final DataComponentType<String> LINK_NAME = register("link_name",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Filter configuration stored on Basic and Advanced Filters. */
    public static final DataComponentType<FilterData> FILTER = register("filter",
            b -> b.persistent(FilterData.CODEC).networkSynchronized(FilterData.STREAM_CODEC));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, StorageNexus.id(name),
                op.apply(DataComponentType.builder()).build());
    }

    public static void init() {}

    private ModComponents() {}
}
