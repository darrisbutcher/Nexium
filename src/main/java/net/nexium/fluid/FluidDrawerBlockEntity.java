package net.nexium.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModComponents;
import net.nexium.upgrade.UpgradeItem;
import net.nexium.upgrade.Upgradeable;
import net.nexium.util.ComparatorReadable;
import net.nexium.util.SyncedBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluid storage with upgrade slots. Fluid drawers hold 32 buckets per block (a 2x2 splits it
 * into 8 buckets per slot); the Fluid Tank is the same thing with one slot and no front.
 */
public class FluidDrawerBlockEntity extends SyncedBlockEntity implements Upgradeable, net.nexium.upgrade.Lockable, ComparatorReadable {
    public static final long BASE_CAPACITY = 32 * FluidConstants.BUCKET;

    private final int slotCount;
    private final FluidSlot[] slots;
    private final CombinedStorage<FluidVariant, FluidSlot> combined;
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);
    private int multiplier = 1;
    private boolean dirty;
    /** Locked with a Drawer Key: each tank stays reserved for its fluid. */
    private boolean locked;
    /** Keys an older save held as Lock Upgrades, to hand back (not saved). */
    private int keysToReturn;

    public FluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FLUID_DRAWER, pos, state, state.getBlock() instanceof FluidDrawerBlock f ? f.slots() : 1);
    }

    protected FluidDrawerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.slotCount = slotCount;
        this.slots = new FluidSlot[slotCount];
        for (int i = 0; i < slotCount; i++) slots[i] = new FluidSlot();
        this.combined = new CombinedStorage<>(List.of(slots));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidDrawerBlockEntity be) {
        if (be.keysToReturn > 0) be.returnKeys(level, pos);
        if (be.dirty && level.getGameTime() % 4 == 0) {
            be.dirty = false;
            be.sync();
        }
    }

    public Storage<FluidVariant> getFluidStorage() { return combined; }
    public int slotCount() { return slotCount; }
    public FluidSlot slot(int i) { return slots[i]; }

    @Override
    public boolean isLocked() { return locked; }

    @Override
    public void setLocked(boolean on) {
        if (on == locked) return;
        locked = on;
        for (FluidSlot s : slots) s.locked = on ? s.variant : FluidVariant.blank();
        dirty = true;
        setChanged();
    }

    @Override
    public void restoreLegacyLock(int keys) {
        locked = true;
        keysToReturn += keys;
        for (FluidSlot s : slots) if (s.locked.isBlank()) s.locked = s.variant;
    }

    /** Hands back the keys an older save held as Lock Upgrades: dropped just above the drawer. */
    private void returnKeys(Level level, BlockPos pos) {
        net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                new ItemStack(net.nexium.registry.ModItems.DRAWER_KEY, keysToReturn));
        keysToReturn = 0;
        setChanged();
    }
    private boolean isVoid() { return hasUpgrade(UpgradeItem.Kind.VOID); }

    /** Total storage before upgrades; the tank overrides this. */
    protected long baseCapacity() {
        return BASE_CAPACITY;
    }

    long capacityFor(int mult) {
        return baseCapacity() * mult / slotCount;
    }

    /** How full the drawer is, 0 to 1: its tanks' fill levels, averaged. */
    public double fillFraction() {
        long capacity = capacityFor(multiplier);
        if (capacity <= 0) return 0;
        double sum = 0;
        for (FluidSlot slot : slots) sum += Math.min(1.0, (double) slot.amount / capacity);
        return sum / slotCount;
    }

    @Override
    public int comparatorSignal() {
        return ComparatorReadable.signal(fillFraction());
    }

    public class FluidSlot extends SingleVariantStorage<FluidVariant> {
        FluidVariant locked = FluidVariant.blank();

        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return capacityFor(multiplier); }

        @Override
        protected boolean canInsert(FluidVariant variant) {
            if (isLocked() && !locked.isBlank()) return variant.equals(locked);
            return true;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            long inserted = super.insert(resource, maxAmount, transaction);
            if (isVoid() && resource.equals(variant)) return maxAmount;
            return inserted;
        }

        @Override
        protected void onFinalCommit() {
            if (isLocked() && locked.isBlank() && !variant.isBlank()) locked = variant;
            dirty = true;
            setChanged();
        }

        public FluidVariant displayVariant() { return variant.isBlank() ? locked : variant; }
        public long capacity() { return capacityFor(multiplier); }
    }

    @Override
    public NonNullList<ItemStack> getUpgrades() { return upgrades; }

    @Override
    public void onUpgradesChanged() {
        multiplier = capacityMultiplier();
        dirty = true;
        setChanged();
    }

    @Override
    public long totalCapacity() {
        return baseCapacity() * multiplier / FluidConstants.BUCKET;
    }

    @Override
    public boolean holdsFluid() {
        return true;
    }

    @Override
    public int capacitySlots() {
        return slotCount;
    }

    @Override
    public boolean canRemoveUpgrade(int slot) {
        long cap = capacityFor(Upgradeable.multiplierFor(upgrades, slot));
        for (FluidSlot s : slots) if (s.amount > cap) return false;
        return true;
    }

    public record SlotData(FluidVariant variant, long amount, FluidVariant locked) {
        public static final Codec<SlotData> CODEC = RecordCodecBuilder.create(i -> i.group(
                // A fluid that no longer exists (a removed mod's, or the retired Honey) loads as an empty slot, not an error
                FluidVariant.CODEC.orElse(FluidVariant.blank()).fieldOf("variant").forGetter(SlotData::variant),
                Codec.LONG.fieldOf("amount").forGetter(SlotData::amount),
                FluidVariant.CODEC.orElse(FluidVariant.blank()).optionalFieldOf("locked", FluidVariant.blank()).forGetter(SlotData::locked)
        ).apply(i, SlotData::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("slots", SlotData.CODEC.listOf(), slotData());
        output.store("upgrades", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(upgrades));
        output.putBoolean("locked", locked);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        locked = input.getBooleanOr("locked", false);
        input.read("upgrades", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            int keys = Upgradeable.loadSaved(list, upgrades);
            if (keys > 0) restoreLegacyLock(keys);
        });
        multiplier = capacityMultiplier();
        input.read("slots", SlotData.CODEC.listOf()).ifPresent(this::loadSlots);
        if (locked) for (FluidSlot s : slots) if (s.locked.isBlank()) s.locked = s.variant;
    }

    private List<SlotData> slotData() {
        List<SlotData> data = new ArrayList<>();
        for (FluidSlot s : slots) data.add(new SlotData(s.variant, s.amount, s.locked));
        return data;
    }

    private void loadSlots(List<SlotData> list) {
        for (int i = 0; i < slotCount && i < list.size(); i++) {
            slots[i].variant = list.get(i).variant();
            slots[i].amount = list.get(i).variant().isBlank() ? 0 : list.get(i).amount();
            slots[i].locked = list.get(i).locked();
        }
    }

    // ---- Keep contents when broken -----------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        for (FluidSlot s : slots) {
            if (!s.variant.isBlank() || !s.locked.isBlank()) {
                components.set(ModComponents.FLUID_SLOTS, slotData());
                break;
            }
        }
        collectUpgrades(components);
        if (locked) components.set(ModComponents.DRAWER_LOCKED, true);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        applyUpgrades(components);
        multiplier = capacityMultiplier();
        List<SlotData> list = components.get(ModComponents.FLUID_SLOTS);
        if (list != null) loadSlots(list);
        dirty = true;
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("slots");
        output.discard("upgrades");
        output.discard("locked");
    }
}
