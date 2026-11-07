package net.storagenexus.essentials.assembler;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.essentials.registry.ModBlockEntities;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Farm Assembler: pick what to make, put in what it needs (from the screen, by shift-clicking, or with hoppers and
 * anything else that inserts items), and build it. It only takes what the chosen target still needs, so it can never
 * hold too much. Choosing another target hands back what it held; breaking it drops everything.
 */
public class FarmAssemblerBlockEntity extends BlockEntity {
    private AssemblyTarget target = AssemblyTarget.IRON_FARM;
    /** What has been put in, as the stacks it came in (Captured Mobs keep their mob). */
    private List<ItemStack> materials = new ArrayList<>();
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
        target = next;
        setChanged();
    }

    /** Builds the target if everything is in and the output is free. Returns whether it did. */
    public boolean build(ServerLevel level) {
        if (!isComplete() || !output.getItem(0).isEmpty()) return false;
        ItemStack made = target.build(level);
        if (made.isEmpty()) return false;
        materials = new ArrayList<>();
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
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output.removeItemNoUpdate(0));
        materials = new ArrayList<>();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("target", target.id());
        out.store("materials", ItemStack.CODEC.listOf(), List.copyOf(materials));
        out.store("output", ItemStack.OPTIONAL_CODEC, output.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        target = AssemblyTarget.byId(in.getStringOr("target", AssemblyTarget.IRON_FARM.id()));
        materials = new ArrayList<>(in.read("materials", ItemStack.CODEC.listOf()).orElse(List.of()));
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
