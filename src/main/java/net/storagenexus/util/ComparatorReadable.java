package net.storagenexus.util;

/** A block entity a comparator can read: how full it is, as a redstone signal from 0 to 15. */
public interface ComparatorReadable {
    int comparatorSignal();

    /**
     * The signal for a fill level from 0 to 1, the way a chest gives it: 0 only when empty, 1 as soon as it holds
     * anything, 15 only when full.
     */
    static int signal(double fraction) {
        if (fraction <= 0) return 0;
        return Math.min(15, (int) Math.floor(Math.min(1.0, fraction) * 14) + 1);
    }
}
