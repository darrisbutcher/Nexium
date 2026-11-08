package net.storagenexus.network;

/**
 * A storage that knows its own used and total space (a Disk Drive), so the terminal's capacity display uses those
 * instead of adding up its views (a disk's views each report the disk's free space, which would count it many times).
 */
public interface ReportsCapacity {
    long storedAmount();

    long totalCapacity();
}
