package net.nexium.gear;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * What Nexium armor gives besides its stats. Each is switched on or off in the armor menu; the setting is saved on the
 * piece it belongs to ({@link #slot()}), in {@code nexium:bonuses_off}.
 */
public enum ArmorBonus implements StringRepresentable {
    /** Full set: Fire Resistance (no fire or lava damage). */
    NETHER_FORGED("nether_forged", EquipmentSlot.CHEST, true),
    /** Full set: no mining slowdown underwater or off the ground. */
    MINERS_SET("miners_set", EquipmentSlot.CHEST, true),
    /** Full set: no knockback. */
    NO_KNOCKBACK("no_knockback", EquipmentSlot.CHEST, true),
    /** The helmet alone: Night Vision. */
    NIGHT_VISION("night_vision", EquipmentSlot.HEAD, false),
    /** Full set: Water Breathing (no drowning). Last in the list so the ids sent over the wire of the others stay the same. */
    WATER_BREATHING("water_breathing", EquipmentSlot.CHEST, true);

    public static final Codec<ArmorBonus> CODEC = StringRepresentable.fromEnum(ArmorBonus::values);
    /**
     * What is saved on an item: a list of ids, where one this version no longer has (the removed Nexus restock) is skipped,
     * so an older chestplate still loads.
     */
    public static final Codec<java.util.List<ArmorBonus>> SAVED_CODEC = Codec.STRING.listOf().xmap(
            ids -> ids.stream().map(ArmorBonus::byId).filter(java.util.Objects::nonNull).toList(),
            bonuses -> bonuses.stream().map(ArmorBonus::id).toList());
    public static final StreamCodec<ByteBuf, ArmorBonus> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String id;
    private final EquipmentSlot slot;
    private final boolean needsFullSet;

    ArmorBonus(String id, EquipmentSlot slot, boolean needsFullSet) {
        this.id = id;
        this.slot = slot;
        this.needsFullSet = needsFullSet;
    }

    /** The bonus with this id, or null if there is none (any more). */
    public static @org.jspecify.annotations.Nullable ArmorBonus byId(String id) {
        for (ArmorBonus bonus : values()) if (bonus.id.equals(id)) return bonus;
        return null;
    }

    /** Lang key part. */
    public String id() {
        return id;
    }

    /** The piece that carries this bonus and its on/off setting. */
    public EquipmentSlot slot() {
        return slot;
    }

    /** True for set bonuses (all four pieces); false for the helmet's own Night Vision. */
    public boolean needsFullSet() {
        return needsFullSet;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
