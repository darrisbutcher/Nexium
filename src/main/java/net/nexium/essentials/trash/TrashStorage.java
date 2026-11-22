package net.nexium.essentials.trash;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * What the Trash Can shows to hoppers, droppers, Item Conduits and Import/Export Buses: an inventory that takes every item, in any
 * amount, and keeps nothing. There is nothing to roll back, so the amount that goes in is simply gone.
 */
final class TrashStorage implements InsertionOnlyStorage<ItemVariant> {
    static final TrashStorage INSTANCE = new TrashStorage();

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        return maxAmount;
    }

    private TrashStorage() {}
}
