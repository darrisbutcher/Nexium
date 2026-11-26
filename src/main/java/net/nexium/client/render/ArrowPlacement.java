package net.nexium.client.render;

import java.util.Arrays;

/**
 * Where the direction arrow of a conduit arm goes. The rings already take up parts of the arm (and which parts depends on the conduit
 * types and the colours that are on), so the arrow goes in the stretch they leave free: the outermost one (nearest the block) that is
 * long enough, else the longest, with the arrow made shorter to fit; with no stretch long enough there is no arrow, and the rings
 * stand alone. All lengths are in pixels (1/16 of a block), measured from the centre of the block out along the arm.
 */
final class ArrowPlacement {
    /** Where the arm starts (the edge of the core) and ends (the block it connects to). */
    static final float ARM_START = 2.1f, ARM_END = 7.4f;
    /** Clear space kept either side of the arrow. */
    static final float MARGIN = 0.15f;
    /** The length of a single arrow and of a double one when there is room, and the shortest that still reads as one. */
    static final float SINGLE = 1.8f, DOUBLE = 1.7f, MIN_SINGLE = 0.8f, MIN_DOUBLE = 1.2f;

    private ArrowPlacement() {
    }

    /**
     * The stretch of arm for the arrow, as {@code {from, to}} with {@code from < to}, or null if the rings leave no room.
     *
     * @param rings       the stretches the rings cover, as {@code near, far} pairs in any order
     * @param count       how many pairs there are
     * @param doubleArrow whether the arm both extracts and inserts, so the arrow is a double one
     */
    static float[] zone(float[] rings, int count, boolean doubleArrow) {
        // The rings in order from the centre outward
        float[] r = Arrays.copyOf(rings, count * 2);
        for (int i = 1; i < count; i++) {
            float near = r[2 * i], far = r[2 * i + 1];
            int j = i - 1;
            while (j >= 0 && r[2 * j] > near) {
                r[2 * j + 2] = r[2 * j];
                r[2 * j + 3] = r[2 * j + 1];
                j--;
            }
            r[2 * j + 2] = near;
            r[2 * j + 3] = far;
        }
        // The free stretches between them
        float[] gaps = new float[2 * (count + 1)];
        int gapCount = 0;
        float cursor = ARM_START;
        for (int i = 0; i < count; i++) {
            if (r[2 * i] > cursor) {
                gaps[2 * gapCount] = cursor;
                gaps[2 * gapCount + 1] = Math.min(r[2 * i], ARM_END);
                gapCount++;
            }
            cursor = Math.max(cursor, r[2 * i + 1]);
        }
        if (cursor < ARM_END) {
            gaps[2 * gapCount] = cursor;
            gaps[2 * gapCount + 1] = ARM_END;
            gapCount++;
        }
        float want = doubleArrow ? DOUBLE : SINGLE, least = doubleArrow ? MIN_DOUBLE : MIN_SINGLE;
        // The outermost stretch that has room for the whole arrow
        for (int g = gapCount - 1; g >= 0; g--) {
            float mid = (gaps[2 * g] + gaps[2 * g + 1]) / 2;
            if (gaps[2 * g + 1] - gaps[2 * g] >= want + 2 * MARGIN) return new float[]{mid - want / 2, mid + want / 2};
        }
        // Otherwise the longest stretch, if a shorter arrow still fits
        int best = -1;
        for (int g = 0; g < gapCount; g++) {
            if (best < 0 || gaps[2 * g + 1] - gaps[2 * g] > gaps[2 * best + 1] - gaps[2 * best]) best = g;
        }
        if (best >= 0 && gaps[2 * best + 1] - gaps[2 * best] - 2 * MARGIN >= least) {
            return new float[]{gaps[2 * best] + MARGIN, gaps[2 * best + 1] - MARGIN};
        }
        return null;
    }
}
