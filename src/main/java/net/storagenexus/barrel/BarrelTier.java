package net.storagenexus.barrel;

/** Barrel upgrade path: a vanilla barrel has 27 slots and every tier adds another 9. */
public enum BarrelTier {
    IRON("iron", 36, 0xC8CDD4),
    GOLD("gold", 45, 0xF8D23C),
    DIAMOND("diamond", 54, 0x4FE6DB),
    NETHERITE("netherite", 63, 0x4A4150);

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
    public int rows() { return slots / 9; }
    /** The tier's colour, used for textures and in the guide. */
    public int color() { return color; }

    /** The tier this one is upgraded from; null for iron (which upgrades a vanilla barrel). */
    public BarrelTier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }
}
