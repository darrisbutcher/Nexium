package net.storagenexus.conduit;

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
    public int channel;
    public RedstoneControl redstone = RedstoneControl.ALWAYS;
    public int priority;
    public boolean roundRobin;
    public ItemStack filter = ItemStack.EMPTY;
    public ItemStack upgrades = ItemStack.EMPTY;
    /** Not saved: round-robin position. */
    public int rrIndex;

    public SideConfig(ConduitType type) {
        this.mode = Mode.INSERT;
    }

    private record Data(int mode, int channel, int redstone, int priority, boolean roundRobin, ItemStack filter, ItemStack upgrades) {}

    private static final Codec<Data> DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("mode").forGetter(Data::mode),
            Codec.INT.fieldOf("channel").forGetter(Data::channel),
            Codec.INT.fieldOf("redstone").forGetter(Data::redstone),
            Codec.INT.fieldOf("priority").forGetter(Data::priority),
            Codec.BOOL.fieldOf("round_robin").forGetter(Data::roundRobin),
            ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(Data::filter),
            ItemStack.OPTIONAL_CODEC.fieldOf("upgrades").forGetter(Data::upgrades)
    ).apply(i, Data::new));

    public static Codec<SideConfig> codec(ConduitType type) {
        return DATA_CODEC.xmap(d -> {
            SideConfig c = new SideConfig(type);
            c.mode = Mode.values()[Math.floorMod(d.mode(), Mode.values().length)];
            c.channel = Math.floorMod(d.channel(), 16);
            c.redstone = RedstoneControl.values()[Math.floorMod(d.redstone(), RedstoneControl.values().length)];
            c.priority = d.priority();
            c.roundRobin = d.roundRobin();
            c.filter = d.filter();
            c.upgrades = d.upgrades();
            return c;
        }, c -> new Data(c.mode.ordinal(), c.channel, c.redstone.ordinal(), c.priority, c.roundRobin, c.filter, c.upgrades));
    }

    public int speedUpgrades() {
        return Math.min(3, upgrades.getCount());
    }
}
