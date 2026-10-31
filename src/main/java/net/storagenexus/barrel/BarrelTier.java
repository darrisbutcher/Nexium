package net.storagenexus.barrel;

/** Barrel upgrade path: a vanilla barrel has 27 slots and every tier adds another 18. */
public enum BarrelTier {
    IRON("iron", 45, 0xC8CDD4),
    GOLD("gold", 63, 0xF8D23C),
    DIAMOND("diamond", 81, 0x4FE6DB),
    NETHERITE("netherite", 99, 0x4A4150);

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
