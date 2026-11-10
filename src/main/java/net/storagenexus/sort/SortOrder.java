package net.storagenexus.sort;

/** The three orders the sort key cycles through. */
public enum SortOrder {
    ALPHABETICAL("alphabetical"),
    CREATIVE("creative"),
    AMOUNT("amount");

    public final String key;

    SortOrder(String key) {
        this.key = key;
    }

    public SortOrder next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static SortOrder byIndex(int index) {
        SortOrder[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
