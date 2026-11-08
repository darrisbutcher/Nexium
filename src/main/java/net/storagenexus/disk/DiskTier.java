package net.storagenexus.disk;

/** Storage disk sizes: how many items an item disk holds, and how many buckets a fluid disk holds. */
public enum DiskTier {
    K16("16k", 16_000L, 64),
    K64("64k", 64_000L, 256),
    K256("256k", 256_000L, 1_024),
    M1("1m", 1_000_000L, 4_096),
    NEXIUM("nexium", 4_000_000L, 16_384);

    private final String id;
    private final long items;
    private final long buckets;

    DiskTier(String id, long items, long buckets) {
        this.id = id;
        this.items = items;
        this.buckets = buckets;
    }

    public String id() { return id; }

    /** Items an item disk of this size holds, of any number of kinds. */
    public long items() { return items; }

    /** Buckets a fluid disk of this size holds. */
    public long buckets() { return buckets; }

    /** The size below, or null for the smallest. */
    public DiskTier previous() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }
}
