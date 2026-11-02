package net.storagenexus.aiot;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Which of Silk Touch and Fortune an AIOT that holds both is using. Stored on the AIOT (component
 * {@code storagenexus:mining_mode}) only while it holds both, and the item model picks the gem colour from it:
 * blue for Silk Touch, green for Fortune.
 */
public enum MiningMode implements StringRepresentable {
    FORTUNE("fortune", Enchantments.FORTUNE),
    SILK_TOUCH("silk_touch", Enchantments.SILK_TOUCH);

    public static final Codec<MiningMode> CODEC = StringRepresentable.fromEnum(MiningMode::values);
    public static final StreamCodec<ByteBuf, MiningMode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String name;
    private final ResourceKey<Enchantment> enchantment;

    MiningMode(String name, ResourceKey<Enchantment> enchantment) {
        this.name = name;
        this.enchantment = enchantment;
    }

    /** The enchantment this mode keeps active. */
    public ResourceKey<Enchantment> enchantment() {
        return enchantment;
    }

    public MiningMode other() {
        return this == FORTUNE ? SILK_TOUCH : FORTUNE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
