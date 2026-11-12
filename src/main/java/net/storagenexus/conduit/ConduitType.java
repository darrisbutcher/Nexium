package net.storagenexus.conduit;

public enum ConduitType {
    ITEM, FLUID, ENERGY;

    public static final ConduitType[] VALUES = values();

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
