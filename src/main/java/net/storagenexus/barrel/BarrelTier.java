package net.storagenexus.barrel;

/**
 * Barrel upgrade path. Iron is 5 rows of 9; from Gold on every tier is a full 6-row grid,
 * three columns wider than the tier below (6x11, 6x14, 6x17, 6x20).
 */
public enum BarrelTier {
    IRON("iron", 45, 0xC8CDD4),
    GOLD("gold", 66, 0xF8D23C),
    DIAMOND("diamond", 84, 0x4FE6DB),
    EMERALD("emerald", 102, 0x41D87A),
    NETHERITE("netherite", 120, 0x4A4150);

    private final String id;
    private final int slots;
    private final int color;

    BarrelTier(String id, int slots, int color) {
        this.id = id;
        this.slots = slots;
        this.color = color;
    }

    public String id() { return id; }
    public int slots() { return slots; }
    /** The tier's colour, used for textures and in the guide. */
    public int color() { return color; }

    /** The tier this one is upgraded from; null for iron (which upgrades a vanilla barrel). */
    public BarrelTier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }
}
