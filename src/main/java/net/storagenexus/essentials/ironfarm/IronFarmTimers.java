package net.storagenexus.essentials.ironfarm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An Iron Farm's timers, all in ticks. Saved with the block and kept in the dropped item, so breaking and
 * placing the farm again doesn't skip a bread cooldown.
 *
 * @param progress  work towards the next golem (a golem needs {@code ironFarmGolemTicks})
 * @param boostLeft boost left on the bread being eaten
 * @param boostUsed boosted ticks since the bread started; the cooldown is worked out from this
 * @param cooldown  ticks until bread can go in again
 */
public record IronFarmTimers(int progress, int boostLeft, long boostUsed, long cooldown) {
    public static final IronFarmTimers NONE = new IronFarmTimers(0, 0, 0, 0);

    public static final Codec<IronFarmTimers> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("progress", 0).forGetter(IronFarmTimers::progress),
            Codec.INT.optionalFieldOf("boost_left", 0).forGetter(IronFarmTimers::boostLeft),
            Codec.LONG.optionalFieldOf("boost_used", 0L).forGetter(IronFarmTimers::boostUsed),
            Codec.LONG.optionalFieldOf("cooldown", 0L).forGetter(IronFarmTimers::cooldown)
    ).apply(i, IronFarmTimers::new));

    public static final StreamCodec<ByteBuf, IronFarmTimers> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IronFarmTimers::progress,
            ByteBufCodecs.VAR_INT, IronFarmTimers::boostLeft,
            ByteBufCodecs.VAR_LONG, IronFarmTimers::boostUsed,
            ByteBufCodecs.VAR_LONG, IronFarmTimers::cooldown,
            IronFarmTimers::new);

    public boolean isNone() {
        return equals(NONE);
    }
}
