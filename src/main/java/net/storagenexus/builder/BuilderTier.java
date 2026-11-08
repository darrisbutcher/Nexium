package net.storagenexus.builder;

/** The Construction Builder's tiers: how many blocks one click places, and its durability (0 = unbreakable). */
public enum BuilderTier {
    IRON("iron", 9, 1024),
    DIAMOND("diamond", 27, 2048),
    NETHERITE("netherite", 64, 4096),
    NEXIUM("nexium", 128, 0);

    private final String id;
    private final int maxBlocks;
    private final int durability;

    BuilderTier(String id, int maxBlocks, int durability) {
        this.id = id;
        this.maxBlocks = maxBlocks;
        this.durability = durability;
    }

    public String id() { return id; }

    public int maxBlocks() { return maxBlocks; }

    public int durability() { return durability; }

    public boolean fireResistant() { return this == NETHERITE || this == NEXIUM; }
}
