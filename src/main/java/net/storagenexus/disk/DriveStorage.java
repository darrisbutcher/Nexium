package net.storagenexus.disk;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.storagenexus.network.ReportsCapacity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * The disks of one kind (items or fluids) in a Disk Drive, as one storage. Inserting fills a disk that already holds
 * the kind first, so kinds don't spread over every disk; then the disks in slot order.
 */
public final class DriveStorage<T extends TransferVariant<?>> implements Storage<T>, ReportsCapacity {
    private final Supplier<List<DiskContents<T>>> disks;

    public DriveStorage(Supplier<List<DiskContents<T>>> disks) {
        this.disks = disks;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext tx) {
        List<DiskContents<T>> list = disks.get();
        long inserted = 0;
        for (DiskContents<T> disk : list) {
            if (inserted >= maxAmount) return inserted;
            if (disk.amountOf(resource) > 0) inserted += disk.insert(resource, maxAmount - inserted, tx);
        }
        for (DiskContents<T> disk : list) {
            if (inserted >= maxAmount) return inserted;
            inserted += disk.insert(resource, maxAmount - inserted, tx);
        }
        return inserted;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext tx) {
        long extracted = 0;
        for (DiskContents<T> disk : disks.get()) {
            if (extracted >= maxAmount) break;
            extracted += disk.extract(resource, maxAmount - extracted, tx);
        }
        return extracted;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        List<StorageView<T>> views = new ArrayList<>();
        for (DiskContents<T> disk : disks.get()) disk.iterator().forEachRemaining(views::add);
        return views.iterator();
    }

    @Override
    public long storedAmount() {
        long n = 0;
        for (DiskContents<T> disk : disks.get()) n += disk.stored();
        return n;
    }

    @Override
    public long totalCapacity() {
        long n = 0;
        for (DiskContents<T> disk : disks.get()) n += disk.capacity();
        return n;
    }
}
