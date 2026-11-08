package net.storagenexus.disk;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one storage disk holds: an amount of each kind, up to a total, any number of kinds (like Refined Storage).
 * It is kept in the world's save ({@link DiskData}), not on the disk item, so a full disk never makes the item huge;
 * the item only carries the disk's id.
 * <p>
 * Changes go through Fabric transactions. Each kind is its own snapshot participant, so a transaction only copies
 * the amounts it touches, never the whole disk.
 */
public final class DiskContents<T extends TransferVariant<?>> implements Storage<T> {
    private final Map<T, Entry> entries = new LinkedHashMap<>();
    private final Total total = new Total();
    private final Runnable onChange;
    private long stored;
    /** Bumped on every committed change, so the drive knows when to refresh the disk's tooltip and lights. */
    private int version;
    /** Set from the disk item each time the disk is used (it depends only on the disk's size). */
    private long capacity;

    public DiskContents(Runnable onChange) {
        this.onChange = onChange;
    }

    public DiskContents<T> withCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public long capacity() { return capacity; }

    public long stored() { return stored; }

    public int version() { return version; }

    /** Kinds held (with an amount above zero). */
    public int types() {
        int n = 0;
        for (Entry e : entries.values()) if (e.amount > 0) n++;
        return n;
    }

    public boolean isEmpty() {
        return stored <= 0;
    }

    public long amountOf(T resource) {
        Entry e = entries.get(resource);
        return e == null ? 0 : e.amount;
    }

    /** Every kind and amount held, for saving. */
    public Map<T, Long> snapshot() {
        Map<T, Long> out = new LinkedHashMap<>();
        for (Entry e : entries.values()) if (e.amount > 0) out.put(e.resource, e.amount);
        return out;
    }

    /** Puts a saved amount back, outside any transaction (loading only). */
    void load(T resource, long amount) {
        if (amount <= 0 || resource.isBlank()) return;
        entries.computeIfAbsent(resource, Entry::new).amount += amount;
        stored += amount;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext tx) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        long n = Math.min(maxAmount, capacity - stored);
        if (n <= 0) return 0;
        Entry e = entries.computeIfAbsent(resource, Entry::new);
        total.updateSnapshots(tx);
        e.updateSnapshots(tx);
        e.amount += n;
        stored += n;
        return n;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext tx) {
        Entry e = entries.get(resource);
        if (e == null || e.amount <= 0 || maxAmount <= 0) return 0;
        long n = Math.min(maxAmount, e.amount);
        total.updateSnapshots(tx);
        e.updateSnapshots(tx);
        e.amount -= n;
        stored -= n;
        return n;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        // A copy, so taking items out while going through them is safe
        List<StorageView<T>> views = new ArrayList<>();
        for (Entry e : entries.values()) if (e.amount > 0) views.add(e);
        return views.iterator();
    }

    private final class Entry extends SnapshotParticipant<Long> implements StorageView<T> {
        private final T resource;
        private long amount;

        Entry(T resource) {
            this.resource = resource;
        }

        @Override
        protected Long createSnapshot() { return amount; }

        @Override
        protected void readSnapshot(Long snapshot) { amount = snapshot; }

        @Override
        protected void onFinalCommit() {
            if (amount <= 0) entries.remove(resource, this);
        }

        @Override
        public long extract(T res, long maxAmount, TransactionContext tx) {
            return res.equals(resource) ? DiskContents.this.extract(res, maxAmount, tx) : 0;
        }

        @Override
        public boolean isResourceBlank() { return false; }

        @Override
        public T getResource() { return resource; }

        @Override
        public long getAmount() { return amount; }

        /** This kind plus the disk's free space: as much of it as the disk could hold. */
        @Override
        public long getCapacity() { return amount + Math.max(0, capacity - stored); }
    }

    private final class Total extends SnapshotParticipant<Long> {
        @Override
        protected Long createSnapshot() { return stored; }

        @Override
        protected void readSnapshot(Long snapshot) { stored = snapshot; }

        @Override
        protected void onFinalCommit() {
            version++;
            onChange.run();
        }
    }
}
