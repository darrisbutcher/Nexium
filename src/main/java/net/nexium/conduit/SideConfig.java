package net.nexium.conduit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/** Per-side, per-conduit-type settings set with the Conduit Wrench. */
public class SideConfig {
    public enum Mode {
        DISABLED, INSERT, EXTRACT, BOTH;

        public boolean inserts() { return this == INSERT || this == BOTH; }
        public boolean extracts() { return this == EXTRACT || this == BOTH; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public enum RedstoneControl {
        ALWAYS, WITH_SIGNAL, WITHOUT_SIGNAL, NEVER;

        public RedstoneControl next() { return values()[(ordinal() + 1) % values().length]; }

        public boolean allows(boolean powered) {
            return switch (this) {
                case ALWAYS -> true;
                case WITH_SIGNAL -> powered;
                case WITHOUT_SIGNAL -> !powered;
                case NEVER -> false;
            };
        }
    }

    public static final int MAX_PRIORITY = 10;

    public Mode mode;
    /** Colour channel this side inserts on: it receives from extract sides with the same extract channel. */
    public int channel;
    /**
     * Colour channels this side extracts on, one bit each (bit n is channel n): what it takes out goes to the insert sides of any of
     * these colours. Never empty. May differ from the insert channel on a Both side.
     */
    public int extractChannels = 1;
    public RedstoneControl redstone = RedstoneControl.ALWAYS;
    public int priority;
    public boolean roundRobin;
    /** Switched off from the Power Meter: this side neither takes energy nor gives it, and keeps its other settings. */
    public boolean paused;
    public ItemStack filter = ItemStack.EMPTY;
    public ItemStack upgrades = ItemStack.EMPTY;
    /** Not saved: round-robin position. */
    public int rrIndex;

    public SideConfig(ConduitType type) {
        // An energy side works both ways from the start: it takes from a generator and gives to a machine without a wrench
        this.mode = type == ConduitType.ENERGY ? Mode.BOTH : Mode.INSERT;
    }

    /** The channels things taken in through this side travel on, as a mask: the insert channel for an Insert side, else the extract channels. */
    public int sourceChannels() {
        return mode == Mode.INSERT ? 1 << channel : extractChannels;
    }

    public boolean extractsOn(int channel) {
        return (extractChannels >> channel & 1) != 0;
    }

    /** Makes {@code channel} the only extract channel. */
    public void setExtractChannel(int channel) {
        extractChannels = 1 << Math.floorMod(channel, 16);
    }

    /** Switches {@code channel} on or off as an extract channel; the last one left on is never switched off. */
    public void toggleExtractChannel(int channel) {
        int next = extractChannels ^ (1 << Math.floorMod(channel, 16));
        if (next != 0) extractChannels = next;
    }

    /** The lowest extract channel on (the one older versions knew as the extract channel). */
    public int firstExtractChannel() {
        return Integer.numberOfTrailingZeros(extractChannels);
    }

    private record Data(int mode, int channel, int extractChannel, int redstone, int priority, boolean roundRobin, ItemStack filter, ItemStack upgrades, boolean paused,
                        int extractChannels) {}

    private static final Codec<Data> DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("mode").forGetter(Data::mode),
            Codec.INT.fieldOf("channel").forGetter(Data::channel),
            // Older saves had one channel for both directions
            Codec.INT.optionalFieldOf("extract_channel", -1).forGetter(Data::extractChannel),
            Codec.INT.fieldOf("redstone").forGetter(Data::redstone),
            Codec.INT.fieldOf("priority").forGetter(Data::priority),
            Codec.BOOL.fieldOf("round_robin").forGetter(Data::roundRobin),
            ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(Data::filter),
            ItemStack.OPTIONAL_CODEC.fieldOf("upgrades").forGetter(Data::upgrades),
            Codec.BOOL.optionalFieldOf("paused", false).forGetter(Data::paused),
            // The set of extract channels, one bit each; 0 (older saves) means just the one in extract_channel
            Codec.INT.optionalFieldOf("extract_channels", 0).forGetter(Data::extractChannels)
    ).apply(i, Data::new));

    public static Codec<SideConfig> codec(ConduitType type) {
        return DATA_CODEC.xmap(d -> {
            SideConfig c = new SideConfig(type);
            c.mode = Mode.values()[Math.floorMod(d.mode(), Mode.values().length)];
            c.channel = Math.floorMod(d.channel(), 16);
            int mask = d.extractChannels() & 0xFFFF;
            c.extractChannels = mask != 0 ? mask : 1 << (d.extractChannel() < 0 ? c.channel : Math.floorMod(d.extractChannel(), 16));
            c.redstone = RedstoneControl.values()[Math.floorMod(d.redstone(), RedstoneControl.values().length)];
            c.priority = d.priority();
            c.roundRobin = d.roundRobin();
            c.filter = d.filter();
            c.upgrades = d.upgrades();
            c.paused = d.paused();
            return c;
        }, c -> new Data(c.mode.ordinal(), c.channel, c.firstExtractChannel(), c.redstone.ordinal(), c.priority, c.roundRobin, c.filter, c.upgrades, c.paused,
                c.extractChannels));
    }

    public int speedUpgrades() {
        return Math.min(3, upgrades.getCount());
    }
}
