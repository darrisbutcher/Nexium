package net.storagenexus.drawer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
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
import java.util.List;

/**
 * Item drawer. A 1x1 holds 54 stacks; 1x2 and 2x2 split the same total across their slots
 * (27 and 13.5 stacks each). Capacity upgrades multiply that.
 */
public class DrawerBlockEntity extends SyncedBlockEntity implements Upgradeable, ItemDrawer {
    public static final int BASE_STACKS = 54;

    private final int slotCount;
    private final DrawerSlot[] slots;
    private final CombinedStorage<ItemVariant, DrawerSlot> combined;
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
    private int multiplier = 1;
    private boolean dirty;

    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAWER, pos, state);
        this.slotCount = state.getBlock() instanceof DrawerBlock d ? d.slots() : 1;
        this.slots = new DrawerSlot[slotCount];
        for (int i = 0; i < slotCount; i++) slots[i] = new DrawerSlot();
        this.combined = new CombinedStorage<>(List.of(slots));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrawerBlockEntity be) {
        if (be.dirty && level.getGameTime() % 4 == 0) {
            be.dirty = false;
            be.sync();
        }
    }

    // ---- Storage --------------------------------------------------------------------------

    public Storage<ItemVariant> getItemStorage() {
        return combined;
    }

    public DrawerSlot slot(int i) {
        return slots[i];
    }

    @Override
    public int slotCount() {
        return slotCount;
    }

    public boolean isVoid() {
        return hasUpgrade(UpgradeItem.Kind.VOID);
    }

    public boolean isLocked() {
        return hasUpgrade(UpgradeItem.Kind.LOCK);
    }

    private static int maxStack(ItemVariant variant) {
        return variant.isBlank() ? 64 : variant.toStack().getMaxStackSize();
    }

    long capacityFor(ItemVariant variant, int mult) {
        return (long) BASE_STACKS * maxStack(variant) * mult / slotCount;
    }

    private void contentsChanged() {
        dirty = true;
        setChanged();
    }

    public class DrawerSlot extends SingleVariantStorage<ItemVariant> {
        ItemVariant locked = ItemVariant.blank();

        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return capacityFor(variant, multiplier);
        }

        @Override
        protected boolean canInsert(ItemVariant variant) {
            if (isLocked() && !locked.isBlank()) return variant.equals(locked);
            return true;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            long inserted = super.insert(resource, maxAmount, transaction);
            if (isVoid() && resource.equals(variant)) return maxAmount;
            return inserted;
        }

        @Override
        protected void onFinalCommit() {
            if (isLocked() && locked.isBlank() && !variant.isBlank()) locked = variant;
            contentsChanged();
        }

        /** What the front shows: the stored item, or the locked item when empty. */
        public ItemVariant displayVariant() {
            return variant.isBlank() ? locked : variant;
        }

        public long capacity() {
            return getCapacity(displayVariant());
        }
    }

    // ---- Player interaction ----------------------------------------------------------------

    @Override
    public void insertFromHand(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot >= slotCount) return;
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = slots[slot].insert(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            stack.shrink((int) Math.min(inserted, stack.getCount()));
        }
    }

    @Override
    public void insertAllFromInventory(int slot, Player player) {
        if (slot >= slotCount) return;
        ItemVariant target = slots[slot].displayVariant();
        if (target.isBlank()) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && target.matches(s)) insertFromHand(slot, s);
        }
        inv.setChanged();
    }

    @Override
    public void extractToPlayer(int slot, Player player, boolean single) {
        if (slot >= slotCount) return;
        DrawerSlot s = slots[slot];
        if (s.isResourceBlank()) return;
        ItemVariant variant = s.getResource();
        int want = single ? 1 : maxStack(variant);
        try (Transaction tx = Transaction.openOuter()) {
            long got = s.extract(variant, want, tx);
            tx.commit();
            if (got > 0) DrawerInteraction.give(player, variant.toStack((int) got));
        }
    }

    // ---- Upgrades --------------------------------------------------------------------------

    @Override
    public NonNullList<ItemStack> getUpgrades() {
        return upgrades;
    }

    @Override
    public void onUpgradesChanged() {
        multiplier = capacityMultiplier();
        if (isLocked()) {
            for (DrawerSlot s : slots) if (s.locked.isBlank()) s.locked = s.variant;
        } else {
            for (DrawerSlot s : slots) s.locked = ItemVariant.blank();
        }
        contentsChanged();
    }

    @Override
    public boolean canRemoveUpgrade(int slot) {
        int newMult = Upgradeable.multiplierFor(upgrades, slot);
        for (DrawerSlot s : slots) {
            if (s.amount > capacityFor(s.variant, newMult)) return false;
        }
        return true;
    }

    // ---- Save / load -----------------------------------------------------------------------

    public record SlotData(ItemVariant variant, long amount, ItemVariant locked) {
        public static final Codec<SlotData> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemVariant.CODEC.fieldOf("variant").forGetter(SlotData::variant),
                Codec.LONG.fieldOf("amount").forGetter(SlotData::amount),
                ItemVariant.CODEC.optionalFieldOf("locked", ItemVariant.blank()).forGetter(SlotData::locked)
        ).apply(i, SlotData::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("slots", SlotData.CODEC.listOf(), slotData());
        output.store("upgrades", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(upgrades));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("upgrades", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < UPGRADE_SLOTS; i++) upgrades.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
            Upgradeable.moveLockToLockSlot(upgrades);
        });
        multiplier = capacityMultiplier();
        input.read("slots", SlotData.CODEC.listOf()).ifPresent(this::loadSlots);
    }

    private List<SlotData> slotData() {
        List<SlotData> data = new ArrayList<>();
        for (DrawerSlot s : slots) data.add(new SlotData(s.variant, s.amount, s.locked));
        return data;
    }

    private void loadSlots(List<SlotData> list) {
        for (int i = 0; i < slotCount && i < list.size(); i++) {
            slots[i].variant = list.get(i).variant();
            slots[i].amount = list.get(i).amount();
            slots[i].locked = list.get(i).locked();
        }
    }

    // ---- Keep contents when broken -----------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        for (DrawerSlot s : slots) {
            if (!s.variant.isBlank() || !s.locked.isBlank()) {
                components.set(ModComponents.DRAWER_SLOTS, slotData());
                break;
            }
        }
        collectUpgrades(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        applyUpgrades(components);
        multiplier = capacityMultiplier();
        List<SlotData> list = components.get(ModComponents.DRAWER_SLOTS);
        if (list != null) loadSlots(list);
        dirty = true;
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("slots");
        output.discard("upgrades");
    }
}
