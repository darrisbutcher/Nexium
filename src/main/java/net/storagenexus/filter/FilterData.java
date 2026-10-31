package net.storagenexus.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Filter settings. Nine ghost entries plus three flags:
 * whitelist (vs blacklist), matchComponents (NBT-style exact match) and tagMode
 * (match anything sharing an item tag with an entry — the Advanced Filter).
 */
public record FilterData(List<ItemStack> entries, boolean whitelist, boolean matchComponents, boolean tagMode) {
    public static final int SIZE = 9;

    public static final Codec<FilterData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("entries").forGetter(FilterData::entries),
            Codec.BOOL.fieldOf("whitelist").forGetter(FilterData::whitelist),
            Codec.BOOL.fieldOf("match_components").forGetter(FilterData::matchComponents),
            Codec.BOOL.fieldOf("tag_mode").forGetter(FilterData::tagMode)
    ).apply(i, FilterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), FilterData::entries,
            ByteBufCodecs.BOOL, FilterData::whitelist,
            ByteBufCodecs.BOOL, FilterData::matchComponents,
            ByteBufCodecs.BOOL, FilterData::tagMode,
            FilterData::new);

    public static FilterData empty(boolean tagMode) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) list.add(ItemStack.EMPTY);
        return new FilterData(list, true, false, tagMode);
    }

    public NonNullList<ItemStack> toList() {
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(SIZE, entries.size()); i++) list.set(i, entries.get(i).copy());
        return list;
    }

    private boolean hasAnyEntry() {
        for (ItemStack s : entries) if (!s.isEmpty()) return true;
        return false;
    }

    public boolean matches(ItemVariant variant) {
        if (variant.isBlank()) return false;
        if (!hasAnyEntry()) return !whitelist; // empty whitelist allows nothing, empty blacklist allows all
        ItemStack candidate = variant.toStack();
        boolean found = false;
        for (ItemStack entry : entries) {
            if (entry.isEmpty()) continue;
            if (entryMatches(entry, candidate)) {
                found = true;
                break;
            }
        }
        return whitelist == found;
    }

    private boolean entryMatches(ItemStack entry, ItemStack candidate) {
        if (tagMode) {
            if (entry.is(candidate.getItem())) return true;
            Holder<Item> holder = candidate.getItem().builtInRegistryHolder();
            for (TagKey<Item> tag : entry.getItem().builtInRegistryHolder().tags().toList()) {
                if (holder.is(tag)) return true;
            }
            return false;
        }
        if (matchComponents) return ItemStack.isSameItemSameComponents(entry, candidate);
        return ItemStack.isSameItem(entry, candidate);
    }

    /** Fluids are filtered by putting the matching bucket in the filter. */
    public boolean matches(FluidVariant variant) {
        if (variant.isBlank()) return false;
        if (!hasAnyEntry()) return !whitelist;
        Fluid fluid = variant.getFluid();
        boolean found = false;
        for (ItemStack entry : entries) {
            if (entry.isEmpty()) continue;
            if (fluid != Fluids.EMPTY && fluid.getBucket() == entry.getItem()) {
                found = true;
                break;
            }
        }
        return whitelist == found;
    }

    public FilterData withEntries(List<ItemStack> newEntries) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : newEntries) copy.add(s.copyWithCount(Math.min(1, s.getCount())));
        return new FilterData(copy, whitelist, matchComponents, tagMode);
    }

    public FilterData withWhitelist(boolean v) { return new FilterData(entries, v, matchComponents, tagMode); }
    public FilterData withMatchComponents(boolean v) { return new FilterData(entries, whitelist, v, tagMode); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilterData other)) return false;
        if (whitelist != other.whitelist || matchComponents != other.matchComponents || tagMode != other.tagMode) return false;
        if (entries.size() != other.entries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            if (!ItemStack.matches(entries.get(i), other.entries.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = Objects.hash(whitelist, matchComponents, tagMode);
        for (ItemStack s : entries) h = 31 * h + (s.isEmpty() ? 0 : s.getItem().hashCode());
        return h;
    }
}
