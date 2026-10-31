package net.storagenexus.drawer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.upgrade.UpgradeItem;
import net.storagenexus.upgrade.Upgradeable;
import net.storagenexus.util.SyncedBlockEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores everything as a count of the smallest form ("units"). Each tier shows
 * units / ratio. Holds 54 stacks of the largest form, times capacity upgrades.
 */
public class CompactingDrawerBlockEntity extends SyncedBlockEntity implements Upgradeable, ItemDrawer {
    public static final int TIERS = 3;

    private final ItemVariant[] tiers = {ItemVariant.blank(), ItemVariant.blank(), ItemVariant.blank()};
    private final long[] ratio = {1, 1, 1};
    private int tierCount = 0;
    private long units = 0;
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
    private int multiplier = 1;
    private boolean dirty;
    private final CompactingStorage storage = new CompactingStorage();

    public CompactingDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPACTING_DRAWER, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompactingDrawerBlockEntity be) {
        if (be.dirty && level.getGameTime() % 4 == 0) {
            be.dirty = false;
            be.sync();
        }
    }

    public Storage<ItemVariant> getItemStorage() {
        return storage;
    }

    @Override
    public int slotCount() {
        return TIERS;
    }

    public ItemVariant tier(int i) { return tiers[i]; }
    public long amountOf(int i) { return i < tierCount && ratio[i] > 0 ? units / ratio[i] : 0; }
    public int tierCount() { return tierCount; }

    private boolean isLocked() { return hasUpgrade(UpgradeItem.Kind.LOCK); }
    private boolean isVoid() { return hasUpgrade(UpgradeItem.Kind.VOID); }

    /** How full the shared pool is, 0 to 1. */
    public double fillFraction() {
        long cap = capacityUnits(multiplier);
        return cap <= 0 ? 0 : Math.min(1.0, (double) units / cap);
    }

    /** 54 stacks of the largest form (e.g. 54 stacks of iron blocks), times upgrades, in units. */
    private long capacityUnits(int mult) {
        if (tierCount == 0) return 0;
        ItemVariant largest = tiers[0];
        return (long) DrawerBlockEntity.BASE_STACKS * largest.toStack().getMaxStackSize() * ratio[0] * mult;
    }

    private int tierOf(ItemVariant resource) {
        for (int i = 0; i < tierCount; i++) if (tiers[i].equals(resource)) return i;
        return -1;
    }

    /** Sets up the chain for a new item. Only when empty and not locked. */
    private boolean configure(ItemVariant resource) {
        if (units > 0 || (isLocked() && tierCount > 0)) return false;
        if (!(level instanceof ServerLevel server)) return false;
        if (resource.hasComponents()) {
            // Items with custom data never compact; treat as single tier.
            tiers[0] = resource; ratio[0] = 1; tierCount = 1;
            for (int i = 1; i < TIERS; i++) { tiers[i] = ItemVariant.blank(); ratio[i] = 1; }
            return true;
        }
        List<CompactingRecipes.Tier> chain = CompactingRecipes.chain(server, resource.getItem());
        tierCount = Math.min(TIERS, chain.size());
        // ratio = how many of the smallest form one item of this tier is worth
        long[] tmp = new long[tierCount];
        tmp[tierCount - 1] = 1;
        for (int i = tierCount - 2; i >= 0; i--) {
            tmp[i] = tmp[i + 1] * Math.max(1, chain.get(i).factorToNextSmaller());
        }
        for (int i = 0; i < TIERS; i++) {
            tiers[i] = i < tierCount ? ItemVariant.of(chain.get(i).item()) : ItemVariant.blank();
            ratio[i] = i < tierCount ? tmp[i] : 1;
        }
        return true;
    }

    private void contentsChanged() {
        if (units == 0 && !isLocked()) {
            tierCount = 0;
            for (int i = 0; i < TIERS; i++) { tiers[i] = ItemVariant.blank(); ratio[i] = 1; }
        }
        dirty = true;
        setChanged();
    }

    /** Everything a transaction can change, including the tier setup done by {@link #configure}. */
    private record Snapshot(long units, ItemVariant[] tiers, long[] ratio, int tierCount) {}

    private class CompactingStorage extends SnapshotParticipant<Snapshot> implements Storage<ItemVariant> {
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            int t = tierOf(resource);
            if (t < 0) {
                // Snapshot first so an aborted (e.g. simulated) insert also undoes the configuration.
                updateSnapshots(transaction);
                if (!configure(resource)) return 0;
                t = tierOf(resource);
                if (t < 0) return 0;
            }
            long space = capacityUnits(multiplier) - units;
            long fit = Math.max(0, space / ratio[t]);
            long inserted = Math.min(fit, maxAmount);
            if (inserted > 0) {
                updateSnapshots(transaction);
                units += inserted * ratio[t];
            }
            return isVoid() ? maxAmount : inserted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            int t = tierOf(resource);
            if (t < 0 || maxAmount <= 0) return 0;
            long available = units / ratio[t];
            long taken = Math.min(available, maxAmount);
            if (taken > 0) {
                updateSnapshots(transaction);
                units -= taken * ratio[t];
            }
            return taken;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            List<StorageView<ItemVariant>> views = new ArrayList<>();
            for (int i = 0; i < tierCount; i++) views.add(new TierView(i));
            return views.iterator();
        }

        @Override
        protected Snapshot createSnapshot() { return new Snapshot(units, tiers.clone(), ratio.clone(), tierCount); }

        @Override
        protected void readSnapshot(Snapshot snapshot) {
            units = snapshot.units();
            System.arraycopy(snapshot.tiers(), 0, tiers, 0, TIERS);
            System.arraycopy(snapshot.ratio(), 0, ratio, 0, TIERS);
            tierCount = snapshot.tierCount();
        }

        @Override
        protected void onFinalCommit() { contentsChanged(); }
    }

    private class TierView implements StorageView<ItemVariant>, net.storagenexus.util.PooledStorageView {
        /** The smallest form (ratio 1) stands for the whole pool in totals. */
        @Override public boolean isPrimaryView() { return tier == tierCount - 1; }

        private final int tier;

        TierView(int tier) { this.tier = tier; }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return resource.equals(tiers[tier]) ? storage.extract(resource, maxAmount, transaction) : 0;
        }

        @Override public boolean isResourceBlank() { return tiers[tier].isBlank(); }
        @Override public ItemVariant getResource() { return tiers[tier]; }
        @Override public long getAmount() { return units / ratio[tier]; }
        @Override public long getCapacity() { return capacityUnits(multiplier) / ratio[tier]; }
    }

    // ---- Player interaction ----------------------------------------------------------------

    @Override
    public void insertFromHand(int slot, ItemStack stack) {
        if (stack.isEmpty()) return;
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            stack.shrink((int) Math.min(inserted, stack.getCount()));
        }
    }

    @Override
    public void insertAllFromInventory(int slot, Player player) {
        if (tierCount == 0) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && tierOf(ItemVariant.of(s)) >= 0) insertFromHand(slot, s);
        }
        inv.setChanged();
    }

    @Override
    public void extractToPlayer(int slot, Player player, boolean single) {
        if (slot >= tierCount) return;
        ItemVariant variant = tiers[slot];
        int want = single ? 1 : variant.toStack().getMaxStackSize();
        try (Transaction tx = Transaction.openOuter()) {
            long got = storage.extract(variant, want, tx);
            tx.commit();
            if (got > 0) DrawerInteraction.give(player, variant.toStack((int) got));
        }
    }

    // ---- Upgrades --------------------------------------------------------------------------

    @Override
    public NonNullList<ItemStack> getUpgrades() { return upgrades; }

    @Override
    public void onUpgradesChanged() {
        multiplier = capacityMultiplier();
        contentsChanged();
    }

    @Override
    public long totalCapacity() {
        return (long) DrawerBlockEntity.BASE_STACKS * multiplier;
    }

    @Override
    public boolean canRemoveUpgrade(int slot) {
        return units <= capacityUnits(Upgradeable.multiplierFor(upgrades, slot));
    }

    // ---- Save / load -----------------------------------------------------------------------

    /** Everything stored, as kept on the dropped item. */
    public record Contents(List<ItemVariant> tiers, List<Long> ratios, int tierCount, long units) {
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemVariant.CODEC.listOf().fieldOf("tiers").forGetter(Contents::tiers),
                Codec.LONG.listOf().fieldOf("ratios").forGetter(Contents::ratios),
                Codec.INT.fieldOf("tier_count").forGetter(Contents::tierCount),
                Codec.LONG.fieldOf("units").forGetter(Contents::units)
        ).apply(i, Contents::new));
    }

    private Contents contents() {
        List<Long> r = new ArrayList<>();
        for (long l : ratio) r.add(l);
        return new Contents(List.of(tiers), r, tierCount, units);
    }

    private void loadContents(Contents c) {
        for (int i = 0; i < TIERS; i++) {
            tiers[i] = i < c.tiers().size() ? c.tiers().get(i) : ItemVariant.blank();
            ratio[i] = i < c.ratios().size() ? Math.max(1, c.ratios().get(i)) : 1;
        }
        tierCount = Math.clamp(c.tierCount(), 0, TIERS);
        units = c.units();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("contents", Contents.CODEC, contents());
        output.store("upgrades", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(upgrades));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("contents", Contents.CODEC).ifPresentOrElse(this::loadContents, () -> {
            // Saves from before 1.0.1 stored the fields separately.
            List<ItemVariant> t = input.read("tiers", ItemVariant.CODEC.listOf()).orElse(List.of());
            List<Long> r = input.read("ratios", Codec.LONG.listOf()).orElse(List.of());
            loadContents(new Contents(t, r, input.getIntOr("tier_count", 0), input.getLongOr("units", 0L)));
        });
        input.read("upgrades", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < UPGRADE_SLOTS; i++) upgrades.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
            Upgradeable.moveLockToLockSlot(upgrades);
        });
        multiplier = capacityMultiplier();
    }

    // ---- Keep contents when broken -----------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (tierCount > 0) components.set(ModComponents.COMPACTING_CONTENTS, contents());
        collectUpgrades(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        applyUpgrades(components);
        multiplier = capacityMultiplier();
        Contents c = components.get(ModComponents.COMPACTING_CONTENTS);
        if (c != null) loadContents(c);
        dirty = true;
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("contents");
        output.discard("upgrades");
    }
}
