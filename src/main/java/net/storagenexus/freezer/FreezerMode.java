package net.storagenexus.freezer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.config.NexusConfig;

/** What a Freezer is making: ice from 1 bucket of water, packed ice from 9, blue ice from 81. */
public enum FreezerMode {
    ICE("ice", Items.ICE, 1),
    PACKED_ICE("packed_ice", Items.PACKED_ICE, 9),
    BLUE_ICE("blue_ice", Items.BLUE_ICE, 81);

    private final String key;
    private final Item output;
    private final int buckets;

    FreezerMode(String key, Item output, int buckets) {
        this.key = key;
        this.output = output;
        this.buckets = buckets;
    }

    public String key() { return key; }
    public Item output() { return output; }
    public int buckets() { return buckets; }

    /** The water one item takes, in Fabric droplets. */
    public long cost() {
        return buckets * FluidConstants.BUCKET;
    }

    /** Ticks one item takes, from the config. */
    public int ticks() {
        NexusConfig c = NexusConfig.get();
        return switch (this) {
            case ICE -> c.freezerIceTicks;
            case PACKED_ICE -> c.freezerPackedIceTicks;
            case BLUE_ICE -> c.freezerBlueIceTicks;
        };
    }

    public FreezerMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static FreezerMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : ICE;
    }
}
