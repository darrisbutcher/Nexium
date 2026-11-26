package net.nexium.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowPlacementTest {
    private static final float EPS = 0.001f;

    private static float[] zone(boolean doubleArrow, float... rings) {
        return ArrowPlacement.zone(rings, rings.length / 2, doubleArrow);
    }

    @Test
    void anInsertRingLeavesTheOuterArmForTheArrow() {
        // An item insert ring at 2.0-2.6: the free stretch past it runs to the end of the arm, and the arrow sits in its middle
        float[] z = zone(false, 2.0f, 2.6f);
        assertNotNull(z);
        assertEquals(ArrowPlacement.SINGLE, z[1] - z[0], EPS);
        assertEquals((2.6f + ArrowPlacement.ARM_END) / 2, (z[0] + z[1]) / 2, EPS);
    }

    @Test
    void anExtractRingWithRoomBeyondItTakesTheOuterStretch() {
        float[] z = zone(false, 4.6f, 5.2f);
        assertNotNull(z);
        assertEquals((5.2f + ArrowPlacement.ARM_END) / 2, (z[0] + z[1]) / 2, EPS);
    }

    @Test
    void ringsFillingTheOuterArmPushTheArrowToTheInnerStretch() {
        // Four extract colours reach the block: the only room is between the core and the first ring
        float[] z = zone(false, 4.6f, 5.2f, 5.5f, 6.1f, 6.4f, 7.0f, 7.3f, 7.9f);
        assertNotNull(z);
        assertTrue(z[1] <= 4.6f - ArrowPlacement.MARGIN + EPS, "the arrow stays clear of the first ring");
        assertTrue(z[0] >= ArrowPlacement.ARM_START, "and of the core");
    }

    @Test
    void aSideThatDoesBothGetsALongEnoughDoubleArrow() {
        float[] z = zone(true, 2.0f, 2.6f, 4.6f, 5.2f);
        assertNotNull(z);
        assertEquals(ArrowPlacement.DOUBLE, z[1] - z[0], EPS);
        assertTrue(z[0] >= 5.2f, "past the extract ring, where the room is");
    }

    @Test
    void theOrderOfTheRingsDoesNotMatter() {
        assertArrayEquals(zone(false, 2.0f, 2.6f, 4.6f, 5.2f, 3.0f, 3.6f), zone(false, 3.0f, 3.6f, 2.0f, 2.6f, 4.6f, 5.2f), EPS);
    }

    @Test
    void aTightStretchGetsAShorterArrow() {
        // Rings at 1.6-3.0 and 4.4-7.9 leave 1.4: too short for a full arrow, long enough for a shorter one
        float[] z = zone(false, 1.6f, 3.0f, 4.4f, 7.9f);
        assertNotNull(z);
        assertEquals(3.0f + ArrowPlacement.MARGIN, z[0], EPS);
        assertEquals(4.4f - ArrowPlacement.MARGIN, z[1], EPS);
    }

    @Test
    void withNoRoomThereIsNoArrow() {
        // An item, a fluid and an energy conduit, each inserting and extracting one colour: every gap is a few tenths of a pixel
        float[] rings = {2.0f, 2.6f, 3.0f, 3.6f, 4.0f, 4.6f, 4.6f, 5.2f, 5.6f, 6.2f, 6.6f, 7.2f};
        assertNull(ArrowPlacement.zone(rings, 6, true));
        assertNull(ArrowPlacement.zone(rings, 6, false));
    }

    @Test
    void theArrowAlwaysStaysOnTheArm() {
        float[][] layouts = {{}, {2.0f, 2.6f}, {4.6f, 5.2f}, {3.0f, 3.6f, 5.6f, 6.2f}, {6.6f, 7.2f}, {1.6f, 2.2f}};
        for (float[] rings : layouts) {
            for (boolean both : new boolean[]{false, true}) {
                float[] z = ArrowPlacement.zone(rings, rings.length / 2, both);
                if (z == null) continue;
                assertTrue(z[0] >= ArrowPlacement.ARM_START && z[1] <= ArrowPlacement.ARM_END && z[0] < z[1]);
                for (int i = 0; i < rings.length; i += 2) {
                    assertTrue(z[1] <= rings[i] || z[0] >= rings[i + 1], "the arrow does not overlap a ring");
                }
            }
        }
    }
}
