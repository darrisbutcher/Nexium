package net.storagenexus.upgrade;

/**
 * A drawer that can be locked with a Drawer Key: a locked drawer keeps each of its slots reserved for the item (or
 * fluid) it holds, even when empty. The key is not used up and the lock is not an upgrade; it is a setting of the drawer.
 */
public interface Lockable {
    boolean isLocked();

    /** Locks or unlocks. Locking reserves every slot for what it holds now; unlocking frees them all. */
    void setLocked(boolean locked);

    /**
     * An older save kept the lock as an upgrade item in the drawer: the drawer stays locked, and the keys it held
     * are handed back (dropped beside it) the next time it ticks.
     */
    void restoreLegacyLock(int keys);
}
