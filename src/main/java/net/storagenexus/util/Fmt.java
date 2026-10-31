package net.storagenexus.util;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

import java.util.Locale;

public final class Fmt {
    /** 1234 -> "1.2k", 3_456_000 -> "3.5M". */
    public static String compact(long n) {
        if (n < 1000) return Long.toString(n);
        if (n < 10_000) return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        if (n < 1_000_000) return (n / 1000) + "k";
        if (n < 10_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        if (n < 1_000_000_000) return (n / 1_000_000) + "M";
        return String.format(Locale.ROOT, "%.1fB", n / 1_000_000_000.0);
    }

    /** Fabric droplets to millibuckets. */
    public static long toMb(long droplets) {
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    public static String fluid(long droplets) {
        long mb = toMb(droplets);
        if (mb >= 1000) return compact(mb / 1000) + " B";
        return mb + " mB";
    }

    private Fmt() {}
}
