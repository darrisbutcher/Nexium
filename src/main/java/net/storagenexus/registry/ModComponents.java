package net.storagenexus.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.StorageNexus;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.filter.FilterData;
import net.storagenexus.fluid.FluidDrawerBlockEntity;

import java.util.List;
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

    // Contents a storage block keeps when broken, so they survive in the dropped item.
    // Copied in by the blocks' loot tables and read back when the item is placed.

    /** Upgrade slots of drawers, fluid drawers and tanks. */
    public static final DataComponentType<ItemContainerContents> UPGRADES = register("upgrades",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** Item drawer slots. */
    public static final DataComponentType<List<DrawerBlockEntity.SlotData>> DRAWER_SLOTS = register("drawer_slots",
            b -> b.persistent(DrawerBlockEntity.SlotData.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(DrawerBlockEntity.SlotData.CODEC.listOf())));

    /** Compacting drawer contents. */
    public static final DataComponentType<CompactingDrawerBlockEntity.Contents> COMPACTING_CONTENTS = register("compacting_contents",
            b -> b.persistent(CompactingDrawerBlockEntity.Contents.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CompactingDrawerBlockEntity.Contents.CODEC)));

    /** Fluid drawer and tank slots. */
    public static final DataComponentType<List<FluidDrawerBlockEntity.SlotData>> FLUID_SLOTS = register("fluid_slots",
            b -> b.persistent(FluidDrawerBlockEntity.SlotData.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(FluidDrawerBlockEntity.SlotData.CODEC.listOf())));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, StorageNexus.id(name),
                op.apply(DataComponentType.builder()).build());
    }

    public static void init() {}

    private ModComponents() {}
}
