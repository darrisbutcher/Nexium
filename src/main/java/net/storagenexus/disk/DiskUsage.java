package net.storagenexus.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** How much a disk holds (items, or droplets for a fluid disk) and of how many kinds. */
public record DiskUsage(long stored, int types) {
    public static final DiskUsage EMPTY = new DiskUsage(0, 0);
    public static final Codec<DiskUsage> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("stored").forGetter(DiskUsage::stored),
            Codec.INT.fieldOf("types").forGetter(DiskUsage::types)
    ).apply(i, DiskUsage::new));
    public static final StreamCodec<ByteBuf, DiskUsage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DiskUsage::stored, ByteBufCodecs.VAR_INT, DiskUsage::types, DiskUsage::new);
}
