package net.storagenexus.util;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmtTest {
    @Test
    void compactCounts() {
        assertEquals("999", Fmt.compact(999));
        assertEquals("1.2k", Fmt.compact(1234));
        assertEquals("54k", Fmt.compact(54_321));
        assertEquals("3.5M", Fmt.compact(3_456_000));
        assertEquals("221k", Fmt.compact(221_184)); // 1x1 drawer with four netherite upgrades
        assertEquals("1.5B", Fmt.compact(1_500_000_000L));
    }

    @Test
    void fluidAmounts() {
        assertEquals("500 mB", Fmt.fluid(FluidConstants.BUCKET / 2));
        assertEquals("1 B", Fmt.fluid(FluidConstants.BUCKET));
        assertEquals("32 B", Fmt.fluid(32 * FluidConstants.BUCKET));
        assertEquals("2.0k B", Fmt.fluid(2048 * FluidConstants.BUCKET));
    }
}
