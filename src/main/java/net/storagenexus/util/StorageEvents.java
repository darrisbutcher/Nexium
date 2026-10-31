package net.storagenexus.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cheap global invalidation counters. Anything that caches a scan of the world
 * (drawer clusters, storage networks, conduit networks) stores the version it was
 * built at and rebuilds when the counter has moved on.
 */
public final class StorageEvents {
    private static final AtomicLong STORAGE_VERSION = new AtomicLong();
    private static final AtomicLong CONDUIT_VERSION = new AtomicLong();

    public static void storageChanged() { STORAGE_VERSION.incrementAndGet(); }
    public static long storageVersion() { return STORAGE_VERSION.get(); }

    public static void conduitsChanged() { CONDUIT_VERSION.incrementAndGet(); }
    public static long conduitVersion() { return CONDUIT_VERSION.get(); }

    private StorageEvents() {}
}
