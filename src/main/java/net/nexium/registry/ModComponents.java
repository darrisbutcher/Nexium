package net.nexium.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.nexium.Nexium;
import net.nexium.aiot.MiningMode;
import net.nexium.drawer.CompactingDrawerBlockEntity;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.filter.FilterData;
import net.nexium.fluid.FluidDrawerBlockEntity;
import net.nexium.gear.ArmorBonus;

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

    /**
     * Enchantments an AIOT holds but isn't using: Silk Touch while Fortune is active, or the other way round.
     * Kept out of {@code minecraft:enchantments} so the active set is always one vanilla allows.
     */
    public static final DataComponentType<ItemEnchantments> INACTIVE_ENCHANTMENTS = register("inactive_enchantments",
            b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC));

    /** Which of Silk Touch and Fortune an AIOT holding both is using; the item model's gem colour follows it. */
    public static final DataComponentType<MiningMode> MINING_MODE = register("mining_mode",
            b -> b.persistent(MiningMode.CODEC).networkSynchronized(MiningMode.STREAM_CODEC));

    /** Modules installed in an AIOT, one slot per module type (see AiotModules). */
    public static final DataComponentType<ItemContainerContents> MODULES = register("modules",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** On a module: switched off in the tool menu. */
    public static final DataComponentType<Unit> MODULE_OFF = register("module_off",
            b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** On a Jetpack: its engine is switched off (no flight, no power use). */
    public static final DataComponentType<Unit> JETPACK_ENGINE_OFF = register("jetpack_engine_off",
            b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** On a Jetpack: hover is switched off (jump thrusts and lifts, and letting go drops). */
    public static final DataComponentType<Unit> JETPACK_HOVER_OFF = register("jetpack_hover_off",
            b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** On an AIOT: most blocks one vein mining swing mines, set in its tool menu (up to the config's limit). */
    public static final DataComponentType<Integer> VEIN_SIZE = register("vein_size",
            b -> b.persistent(Codec.intRange(1, 512)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a Backpack's Crafting module: the crafting grid is hidden (the button on its panel), so the screen is its plain width. */
    public static final DataComponentType<Unit> CRAFTING_HIDDEN = register("crafting_hidden",
            b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** On an AIOT: whether 3x3 mining is switched on (absent: off, or what an old 3x3 Mining Module in it was set to). */
    public static final DataComponentType<Boolean> AREA_MINING = register("area_mining",
            b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** On an AIOT: how many 3x3 layers deep one swing tunnels (1 = a plain 3x3), set in the tool menu. */
    public static final DataComponentType<Integer> TUNNEL_LENGTH = register("tunnel_length",
            b -> b.persistent(Codec.intRange(1, 64)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a Construction Builder: its mode (Extend, Line, Exchange), as an index. */
    public static final DataComponentType<Integer> BUILDER_MODE = register("builder_mode",
            b -> b.persistent(Codec.intRange(0, 15)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a Construction Builder: which way it spreads (any, horizontal, vertical), as an index. */
    public static final DataComponentType<Integer> BUILDER_PLANE = register("builder_plane",
            b -> b.persistent(Codec.intRange(0, 15)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a Construction Builder: how solid the preview blocks look, in percent (0 is the outline alone), set in its tool menu. */
    public static final DataComponentType<Integer> BUILDER_OPACITY = register("builder_opacity",
            b -> b.persistent(Codec.intRange(0, 100)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a Construction Builder: blocks one click places, set in its tool menu (up to its tier's limit). */
    public static final DataComponentType<Integer> BUILDER_SIZE = register("builder_size",
            b -> b.persistent(Codec.intRange(1, 1024)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** On a storage disk: the id its contents are saved under ({@link net.nexium.disk.DiskData}). */
    public static final DataComponentType<UUID> DISK_ID = register("disk_id",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    /** On a storage disk: how much it holds and of how many kinds, refreshed by the Disk Drive, for the tooltip and lights. */
    public static final DataComponentType<net.nexium.disk.DiskUsage> DISK_USAGE = register("disk_usage",
            b -> b.persistent(net.nexium.disk.DiskUsage.CODEC).networkSynchronized(net.nexium.disk.DiskUsage.STREAM_CODEC));

    /** On a Nexium chestplate or helmet: the bonuses it carries that are switched off in the armor menu. */
    public static final DataComponentType<List<ArmorBonus>> BONUSES_OFF = register("bonuses_off",
            b -> b.persistent(ArmorBonus.CODEC.listOf()).networkSynchronized(ArmorBonus.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** On a Nexium tool: its Haste II is turned off in the tool menu. */
    public static final DataComponentType<Unit> HASTE_OFF = register("haste_off",
            b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    // Contents a storage block keeps when broken, so they survive in the dropped item.
    // Copied in by the blocks' loot tables and read back when the item is placed.

    /** Upgrade slots of drawers, fluid drawers and tanks. */
    public static final DataComponentType<ItemContainerContents> UPGRADES = register("upgrades",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** A drawer locked with a Drawer Key stays locked in the dropped item. Only stored when it is locked. */
    public static final DataComponentType<Boolean> DRAWER_LOCKED = register("drawer_locked",
            b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

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

    /** What a Fluid Tank holds: its fluid and how much, kept when it is broken and carried by the item. */
    public static final DataComponentType<net.nexium.fluid.FluidTankBlockEntity.Contents> TANK_FLUID = register("tank_fluid",
            b -> b.persistent(net.nexium.fluid.FluidTankBlockEntity.Contents.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(net.nexium.fluid.FluidTankBlockEntity.Contents.CODEC)));

    /** Water (in Fabric droplets) a Freezer held when it was broken. */
    public static final DataComponentType<Long> FREEZER_WATER = register("freezer_water",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** What a Freezer was making when it was broken (0 ice, 1 packed ice, 2 blue ice). */
    public static final DataComponentType<Integer> FREEZER_MODE = register("freezer_mode",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Nexus Energy a Battery held when it was broken. */
    public static final DataComponentType<Long> BATTERY_CHARGE = register("battery_charge",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** The battery cell inside a Jetpack or a Crafting Storage Tablet (the cell's own charge is in its {@code battery_charge}). */
    public static final DataComponentType<net.minecraft.world.item.ItemStackTemplate> INSTALLED_CELL = register("installed_cell",
            b -> b.persistent(net.minecraft.world.item.ItemStackTemplate.CODEC).networkSynchronized(net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Nexium.id(name),
                op.apply(DataComponentType.builder()).build());
    }

    public static void init() {}

    private ModComponents() {}
}
