package net.storagenexus.util;

/**
 * A storage view that is one of several showing the same pool (a compacting drawer lists one
 * pool as nuggets, ingots and blocks). Totals count only the primary view so the pool counts once.
 */
public interface PooledStorageView {
    boolean isPrimaryView();
}
