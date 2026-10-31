package net.storagenexus.drawer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrawerInteractionTest {
    // u = 0..1 left to right as seen from the front, v = 0..1 bottom to top

    @Test
    void singleSlotCoversTheWholeFront() {
        assertEquals(0, DrawerInteraction.slotAt(1, 0.1, 0.1));
        assertEquals(0, DrawerInteraction.slotAt(1, 0.9, 0.9));
    }

    @Test
    void twoSlotsAreTopThenBottom() {
        assertEquals(0, DrawerInteraction.slotAt(2, 0.2, 0.75));
        assertEquals(1, DrawerInteraction.slotAt(2, 0.8, 0.25));
    }

    @Test
    void fourSlotsReadLikeABook() {
        assertEquals(0, DrawerInteraction.slotAt(4, 0.25, 0.75));
        assertEquals(1, DrawerInteraction.slotAt(4, 0.75, 0.75));
        assertEquals(2, DrawerInteraction.slotAt(4, 0.25, 0.25));
        assertEquals(3, DrawerInteraction.slotAt(4, 0.75, 0.25));
    }

    @Test
    void compactingHasLargestFormOnTop() {
        assertEquals(0, DrawerInteraction.slotAt(3, 0.1, 0.9));
        assertEquals(0, DrawerInteraction.slotAt(3, 0.9, 0.9));
        assertEquals(1, DrawerInteraction.slotAt(3, 0.25, 0.25));
        assertEquals(2, DrawerInteraction.slotAt(3, 0.75, 0.25));
    }

    @Test
    void centreLinesBelongToTopAndRight() {
        assertEquals(1, DrawerInteraction.slotAt(4, 0.5, 0.5));
    }
}
