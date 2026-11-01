package net.storagenexus.essentials.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * Everything about a picked-up mob: its full saved data (the same data the world saves it with) plus a few
 * values for the tooltip. The full data is sent to clients too, because the creative inventory sends whole
 * item stacks back to the server.
 */
public record CapturedMob(EntityType<?> type, CompoundTag data, Optional<Component> customName, float health, float maxHealth, boolean baby) {
    public static final Codec<CapturedMob> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(CapturedMob::type),
            CompoundTag.CODEC.fieldOf("data").forGetter(CapturedMob::data),
            ComponentSerialization.CODEC.optionalFieldOf("custom_name").forGetter(CapturedMob::customName),
            Codec.FLOAT.fieldOf("health").forGetter(CapturedMob::health),
            Codec.FLOAT.fieldOf("max_health").forGetter(CapturedMob::maxHealth),
            Codec.BOOL.optionalFieldOf("baby", false).forGetter(CapturedMob::baby)
    ).apply(i, CapturedMob::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedMob> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ENTITY_TYPE), CapturedMob::type,
            ByteBufCodecs.COMPOUND_TAG, CapturedMob::data,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, CapturedMob::customName,
            ByteBufCodecs.FLOAT, CapturedMob::health,
            ByteBufCodecs.FLOAT, CapturedMob::maxHealth,
            ByteBufCodecs.BOOL, CapturedMob::baby,
            CapturedMob::new);
}
