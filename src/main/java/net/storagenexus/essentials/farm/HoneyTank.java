package net.storagenexus.essentials.farm;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.storagenexus.essentials.registry.ModFluids;

import java.util.Iterator;

/** The Bee Farm's tank: 16 buckets of honey, filled by the farm and emptied by pipes or glass bottles. */
public class HoneyTank extends SingleVariantStorage<FluidVariant> {
    /** A honey bottle is a quarter of a bucket (four to a honey block, as in vanilla). */
    public static final long BOTTLE = FluidConstants.BUCKET / 4;
    public static final long CAPACITY = 16 * FluidConstants.BUCKET;

    private final Runnable onChange;
    private final Storage<FluidVariant> outside = new Storage<>() {
        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return HoneyTank.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return HoneyTank.this.iterator();
        }
    };

    public HoneyTank(Runnable onChange) {
        this.onChange = onChange;
    }

    /** What pipes see: honey can be taken out but not put in. */
    public Storage<FluidVariant> outside() {
        return outside;
    }

    /** Adds honey from the farm itself; what doesn't fit is lost. */
    public void fill(long amount) {
        try (Transaction tx = Transaction.openOuter()) {
            insert(FluidVariant.of(ModFluids.HONEY), amount, tx);
            tx.commit();
        }
    }

    /** Takes one bottle's worth if the tank has it. */
    public boolean takeBottle() {
        try (Transaction tx = Transaction.openOuter()) {
            if (extract(FluidVariant.of(ModFluids.HONEY), BOTTLE, tx) == BOTTLE) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    public long stored() {
        return amount;
    }

    /** Sets the contents when loading a save. */
    void load(long stored) {
        variant = stored > 0 ? FluidVariant.of(ModFluids.HONEY) : FluidVariant.blank();
        amount = Math.max(0, Math.min(CAPACITY, stored));
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return CAPACITY;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        return variant.isOf(ModFluids.HONEY);
    }

    @Override
    protected void onFinalCommit() {
        onChange.run();
    }
}
