package net.nexium.essentials.assembler;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.essentials.registry.ModBlockEntities;
import net.nexium.network.StorageNetwork;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Farm Assembler: pick what to make, put in what it needs (from the screen, by shift-clicking, or with hoppers and
 * anything else that inserts items), and build it. It only takes what the chosen target still needs, so it can never
 * hold too much. Choosing another target hands back what it held; breaking it drops everything.
 * A line of water or lava buckets can also be met with fluid from a storage network (see {@link #addFromNetwork()}): the
 * assembler then holds that as fluid, one bucket's worth for each bucket, and gives it back as fluid.
 */
public class FarmAssemblerBlockEntity extends BlockEntity {
    private AssemblyTarget target = AssemblyTarget.IRON_FARM;
    /** What has been put in, as the stacks it came in (Captured Mobs keep their mob). */
    private List<ItemStack> materials = new ArrayList<>();
    /** Fluid it holds towards the lines of water or lava buckets, in droplets (whole buckets only), taken from a storage network. */
    private Map<Fluid, Long> fluids = new LinkedHashMap<>();
    private static final Codec<Map<Fluid, Long>> FLUIDS_CODEC = Codec.unboundedMap(BuiltInRegistries.FLUID.byNameCodec(), Codec.LONG);
    /** The finished farm, waiting to be taken. */
    private final SimpleContainer output = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            FarmAssemblerBlockEntity.this.setChanged();
        }
    };
    private final InputStorage storage = new InputStorage();

    public FarmAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARM_ASSEMBLER, pos, state);
    }

    public AssemblyTarget target() { return target; }

    public List<ItemStack> materials() { return Collections.unmodifiableList(materials); }

    public SimpleContainer output() { return output; }

    /** How much fluid it holds (taken from a network for a line of water or lava buckets), in droplets. */
    public long fluid(Fluid fluid) { return fluids.getOrDefault(fluid, 0L); }

    /** Where hoppers and other mods put items in. Nothing can be taken out this way. */
    public Storage<ItemVariant> storage() { return storage; }

    // ---------------------------------------------------------------- what it holds

    /** How much it holds towards each of the target's requirements. */
    public int[] counts() {
        List<Requirement> reqs = target.requirements();
        int[] counts = new int[reqs.size()];
        for (ItemStack m : materials) {
            int i = indexOf(reqs, m);
            if (i >= 0) counts[i] += m.getCount();
        }
        // Fluid held for a line of water or lava buckets counts a bucket's worth at a time
        for (int i = 0; i < reqs.size(); i++) {
            Fluid fluid = reqs.get(i).fluid();
            if (fluid != null) counts[i] += (int) (fluid(fluid) / FluidConstants.BUCKET);
        }
        return counts;
    }

    private static int indexOf(List<Requirement> reqs, ItemStack stack) {
        for (int i = 0; i < reqs.size(); i++) if (reqs.get(i).matches().test(stack)) return i;
        return -1;
    }

    /** How many of this stack it would take: what its requirement still needs, up to the stack's size. */
    public int room(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        List<Requirement> reqs = target.requirements();
        int i = indexOf(reqs, stack);
        if (i < 0) return 0;
        return Math.max(0, Math.min(stack.getCount(), reqs.get(i).needed() - counts()[i]));
    }

    /** How many more of what this stack is the target still needs (not limited by the stack's own size), 0 if it is not a material. */
    private int stillNeeded(ItemStack stack) {
        List<Requirement> reqs = target.requirements();
        int i = indexOf(reqs, stack);
        return i < 0 ? 0 : Math.max(0, reqs.get(i).needed() - counts()[i]);
    }

    /** Takes as much of {@code stack} as it needs (the stack itself is not changed). Returns how many it took. */
    public int accept(ItemStack stack) {
        int n = room(stack);
        if (n > 0) {
            add(stack.copyWithCount(n));
            setChanged();
        }
        return n;
    }

    private void add(ItemStack stack) {
        for (ItemStack m : materials) {
            if (stack.isEmpty()) return;
            if (ItemStack.isSameItemSameComponents(m, stack) && m.getCount() < m.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), m.getMaxStackSize() - m.getCount());
                m.grow(move);
                stack.shrink(move);
            }
        }
        while (!stack.isEmpty()) materials.add(stack.split(stack.getMaxStackSize()));
    }

    public boolean isComplete() {
        List<Requirement> reqs = target.requirements();
        int[] counts = counts();
        for (int i = 0; i < reqs.size(); i++) if (counts[i] < reqs.get(i).needed()) return false;
        return true;
    }

    // ---------------------------------------------------------------- what the screen's buttons do

    /** Moves everything the target still needs out of the player's inventory (not their armor or offhand). */
    public int addFrom(Player player) {
        Inventory inventory = player.getInventory();
        int moved = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            int n = accept(stack);
            if (n > 0) {
                stack.shrink(n);
                moved += n;
            }
        }
        if (moved > 0) inventory.setChanged();
        return moved;
    }

    /**
     * The storage network this assembler touches (a Storage Connector, an Inventory Cable or any other network block against it), or
     * null. The connectors do not take the assembler for a storage inventory (it is a machine), so touching one links it.
     */
    public @Nullable StorageNetwork network() {
        if (level == null) return null;
        for (Direction side : Direction.values()) {
            StorageNetwork found = StorageNetwork.find(level, worldPosition.relative(side));
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Takes what the target still needs out of the storage network it touches: spawn eggs or Captured Mobs, blocks, everything on its
     * list, nothing more than it needs, and nothing it does not ask for. A line of water or lava buckets takes real buckets from the
     * network first, then makes up the rest from the network's water or lava, a bucket's worth for each bucket (whole buckets only), and
     * holds that as fluid. Returns how many items and buckets it took (0 when it touches no network).
     */
    public int addFromNetwork() {
        StorageNetwork network = network();
        if (network == null) return 0;
        int moved = 0;
        for (Map.Entry<ItemVariant, Long> entry : network.listItems().entrySet()) {
            ItemVariant variant = entry.getKey();
            ItemStack sample = variant.toStack(1);
            long want = Math.min(entry.getValue(), stillNeeded(sample));
            while (want > 0) {
                int batch = (int) Math.min(want, sample.getMaxStackSize());
                try (Transaction tx = Transaction.openOuter()) {
                    long got = network.extractItem(variant, batch, tx);
                    if (got <= 0) break;
                    int took = accept(variant.toStack((int) got));
                    if (took < got) network.insertItem(variant, got - took, tx); // it took less than it asked for: hand the rest back
                    tx.commit();
                    if (took <= 0) break;
                    moved += took;
                    want -= took;
                }
            }
        }
        moved += addFluidFromNetwork(network);
        return moved;
    }

    /** Makes up what the lines of water and lava buckets still miss from the network's fluid, in whole buckets. Returns how many buckets. */
    private int addFluidFromNetwork(StorageNetwork network) {
        List<Requirement> reqs = target.requirements();
        int moved = 0;
        for (int i = 0; i < reqs.size(); i++) {
            Fluid fluid = reqs.get(i).fluid();
            if (fluid == null) continue;
            long missing = reqs.get(i).needed() - counts()[i];
            FluidVariant variant = FluidVariant.of(fluid);
            long buckets = Math.min(missing, network.countFluid(variant) / FluidConstants.BUCKET);
            if (buckets <= 0) continue;
            try (Transaction tx = Transaction.openOuter()) {
                long got = network.extractFluid(variant, buckets * FluidConstants.BUCKET, tx);
                long whole = got / FluidConstants.BUCKET * FluidConstants.BUCKET;
                if (whole <= 0) continue; // not even a bucket: nothing is taken (the transaction is not committed)
                if (got > whole) network.insertFluid(variant, got - whole, tx); // only whole buckets are kept: hand a part bucket back
                fluids.merge(fluid, whole, Long::sum);
                tx.commit();
                moved += (int) (whole / FluidConstants.BUCKET);
            }
        }
        if (moved > 0) setChanged();
        return moved;
    }

    /**
     * Gives back the fluid it holds: into the storage network if one still touches it and has room, and what that does not take as
     * filled buckets, to the player (or dropped on the assembler with no player).
     */
    private void returnFluids(@Nullable Player player) {
        if (fluids.isEmpty()) return;
        StorageNetwork network = network();
        for (Map.Entry<Fluid, Long> held : fluids.entrySet()) {
            long left = held.getValue();
            if (network != null) {
                try (Transaction tx = Transaction.openOuter()) {
                    left -= network.insertFluid(FluidVariant.of(held.getKey()), left, tx);
                    tx.commit();
                }
            }
            giveBuckets(held.getKey(), left, player);
        }
        fluids = new LinkedHashMap<>();
    }

    /** {@code droplets} of {@code fluid} as filled buckets (whole ones), to the player or dropped on the assembler. */
    private void giveBuckets(Fluid fluid, long droplets, @Nullable Player player) {
        Item bucket = fluid.getBucket();
        for (long n = droplets / FluidConstants.BUCKET; n > 0; n--) {
            ItemStack stack = new ItemStack(bucket);
            if (player != null) player.getInventory().placeItemBackInInventory(stack, false, Prediction.SERVER_ONLY);
            else if (level != null) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), stack);
        }
    }

    /**
     * Switches to another target, handing what it held back to the player (or dropping what doesn't fit), or with no
     * player, dropping it on the assembler.
     */
    public void select(AssemblyTarget next, @Nullable Player player) {
        if (next == target) return;
        for (ItemStack m : materials) {
            if (player != null) player.getInventory().placeItemBackInInventory(m, false, Prediction.SERVER_ONLY);
            else if (level != null) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), m);
        }
        materials = new ArrayList<>();
        returnFluids(player);
        target = next;
        setChanged();
    }

    /** Builds the target if everything is in and the output is free. Returns whether it did. */
    public boolean build(ServerLevel level) {
        if (!isComplete() || !output.getItem(0).isEmpty()) return false;
        ItemStack made = target.build(level);
        if (made.isEmpty()) return false;
        materials = new ArrayList<>();
        fluids = new LinkedHashMap<>();
        output.setItem(0, made);
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 20, 0.4, 0.2, 0.4, 0.02);
        level.playSound(null, x, y, z, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
        return true;
    }

    // ---------------------------------------------------------------- saving, breaking

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (ItemStack m : materials) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), m);
        for (Map.Entry<Fluid, Long> held : fluids.entrySet()) {
            for (long n = held.getValue() / FluidConstants.BUCKET; n > 0; n--) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(held.getKey().getBucket()));
            }
        }
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output.removeItemNoUpdate(0));
        materials = new ArrayList<>();
        fluids = new LinkedHashMap<>();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("target", target.id());
        out.store("materials", ItemStack.CODEC.listOf(), List.copyOf(materials));
        if (!fluids.isEmpty()) out.store("fluids", FLUIDS_CODEC, Map.copyOf(fluids));
        out.store("output", ItemStack.OPTIONAL_CODEC, output.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        target = AssemblyTarget.byId(in.getStringOr("target", AssemblyTarget.IRON_FARM.id()));
        materials = new ArrayList<>(in.read("materials", ItemStack.CODEC.listOf()).orElse(List.of()));
        fluids = new LinkedHashMap<>(in.read("fluids", FLUIDS_CODEC).orElse(Map.of()));
        output.setItem(0, in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    // ---------------------------------------------------------------- hoppers and other mods

    private class InputStorage extends SnapshotParticipant<List<ItemStack>> implements InsertionOnlyStorage<ItemVariant> {
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            ItemStack offered = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            int n = room(offered);
            if (n <= 0) return 0;
            updateSnapshots(transaction);
            add(offered.copyWithCount(n));
            return n;
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            List<ItemStack> copy = new ArrayList<>(materials.size());
            for (ItemStack m : materials) copy.add(m.copy());
            return copy;
        }

        @Override
        protected void readSnapshot(List<ItemStack> snapshot) {
            materials = new ArrayList<>(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }
}
