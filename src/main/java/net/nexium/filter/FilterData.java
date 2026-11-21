package net.nexium.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
 * An Advanced Filter can also hold chosen item {@code tags} (anything in one of them matches) and one {@code special}
 * match (smeltable items, or furnace fuel). An item passes when any entry, tag or the special matches; the whitelist flag
 * says whether that lets it through or keeps it out.
 */
public record FilterData(List<ItemStack> entries, boolean whitelist, boolean matchComponents, boolean tagMode, List<Identifier> tags,
                         Special special) {
    public static final int SIZE = 9;
    /** Most tags one filter can hold. */
    public static final int MAX_TAGS = 32;

    /** A match decided by the game rather than by a list: what a furnace can smelt, or what burns in one. */
    public enum Special {
        NONE, SMELTABLE, FUEL;

        public Special next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean matches(Item item) {
            return switch (this) {
                case NONE -> false;
                case SMELTABLE -> FilterSpecials.isSmeltable(item);
                case FUEL -> FilterSpecials.isFuel(item);
            };
        }
    }

    public static final Codec<FilterData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("entries").forGetter(FilterData::entries),
            Codec.BOOL.fieldOf("whitelist").forGetter(FilterData::whitelist),
            Codec.BOOL.fieldOf("match_components").forGetter(FilterData::matchComponents),
            Codec.BOOL.fieldOf("tag_mode").forGetter(FilterData::tagMode),
            // Newer fields: a filter saved before them has none
            Identifier.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(FilterData::tags),
            Codec.INT.xmap(n -> Special.values()[Math.floorMod(n, Special.values().length)], Special::ordinal)
                    .optionalFieldOf("special", Special.NONE).forGetter(FilterData::special)
    ).apply(i, FilterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), FilterData::entries,
            ByteBufCodecs.BOOL, FilterData::whitelist,
            ByteBufCodecs.BOOL, FilterData::matchComponents,
            ByteBufCodecs.BOOL, FilterData::tagMode,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TAGS)), FilterData::tags,
            ByteBufCodecs.VAR_INT.map(n -> Special.values()[Math.floorMod(n, Special.values().length)], Special::ordinal), FilterData::special,
            FilterData::new);

    public static FilterData empty(boolean tagMode) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) list.add(ItemStack.EMPTY);
        return new FilterData(list, true, false, tagMode, List.of(), Special.NONE);
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

    /** Whether the filter holds anything to match against: an entry, a chosen tag or a special match. */
    private boolean hasAnyCondition() {
        return hasAnyEntry() || !tags.isEmpty() || special != Special.NONE;
    }

    public boolean matches(ItemVariant variant) {
        if (variant.isBlank()) return false;
        if (!hasAnyCondition()) return !whitelist; // empty whitelist allows nothing, empty blacklist allows all
        ItemStack candidate = variant.toStack();
        boolean found = special.matches(candidate.getItem()) || inChosenTag(candidate);
        if (!found) {
            for (ItemStack entry : entries) {
                if (entry.isEmpty()) continue;
                if (entryMatches(entry, candidate)) {
                    found = true;
                    break;
                }
            }
        }
        return whitelist == found;
    }

    /** Whether the item is in any of the tags chosen for this filter. */
    private boolean inChosenTag(ItemStack candidate) {
        for (Identifier id : tags) {
            if (candidate.is(TagKey.create(Registries.ITEM, id))) return true;
        }
        return false;
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
        return new FilterData(copy, whitelist, matchComponents, tagMode, tags, special);
    }

    public FilterData withWhitelist(boolean v) { return new FilterData(entries, v, matchComponents, tagMode, tags, special); }
    public FilterData withMatchComponents(boolean v) { return new FilterData(entries, whitelist, v, tagMode, tags, special); }
    public FilterData withTags(List<Identifier> v) { return new FilterData(entries, whitelist, matchComponents, tagMode, List.copyOf(v), special); }
    public FilterData withSpecial(Special v) { return new FilterData(entries, whitelist, matchComponents, tagMode, tags, v); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilterData other)) return false;
        if (whitelist != other.whitelist || matchComponents != other.matchComponents || tagMode != other.tagMode) return false;
        if (special != other.special || !tags.equals(other.tags)) return false;
        if (entries.size() != other.entries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            if (!ItemStack.matches(entries.get(i), other.entries.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = Objects.hash(whitelist, matchComponents, tagMode, tags, special);
        for (ItemStack s : entries) h = 31 * h + (s.isEmpty() ? 0 : s.getItem().hashCode());
        return h;
    }
}
