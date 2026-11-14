package net.storagenexus.energy;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The Nexus Furnace: eight input slots, each with an output slot of its own, all cooking at once. It smelts anything a
 * furnace, a blast furnace or a smoker does, in {@value #COOK_TICKS} ticks, for {@value #ENERGY_PER_SLOT} NE a tick per
 * slot that is working. With less energy than every working slot needs, it runs as many slots as it can pay for and the
 * rest wait their turn, so every slot cooks the same amount over time. A stack put in is shared out evenly over the
 * input slots. The experience the recipes give, times {@value #XP_MULTIPLIER}, is kept until a player collects it.
 */
public class NexusFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS = 8, SIZE = SLOTS * 2;
    public static final int COOK_TICKS = 50;
    /** Every item gives ten times the experience its recipe does. */
    public static final int XP_MULTIPLIER = 10;
    public static final int ENERGY_PER_SLOT = 30;
    /** Ten seconds of energy for every slot. */
    public static final long CAPACITY = (long) ENERGY_PER_SLOT * SLOTS * 200;

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] OUTPUT_SLOTS = {8, 9, 10, 11, 12, 13, 14, 15};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final int[] progress = new int[SLOTS];
    private final EnergyBuffer energy = EnergyBuffer.machine(CAPACITY, this::setChanged);
    private final Inputs inputs = new Inputs();
    private double experience;
    private int rotation;
    /** Slots that had work, and slots that were paid for, on the last tick (for the screen). */
    private int wanting, working;

    public NexusFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS_FURNACE, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    public int progress(int slot) { return progress[slot]; }

    public int wanting() { return wanting; }

    public int working() { return working; }

    /** Experience waiting to be collected, in points. */
    public double experience() { return experience; }

    /** Storage for what goes in from the top and sides: a stack is shared out evenly over the input slots. */
    public InsertionOnlyStorage<ItemVariant> inputs() { return inputs; }

    // ---------------------------------------------------------------- recipes

    /** The recipe a furnace, a blast furnace or a smoker has for this item (smelting first), if any. */
    public static Optional<? extends AbstractCookingRecipe> recipeFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return Optional.empty();
        var input = new SingleRecipeInput(stack);
        var manager = server.recipeAccess();
        Optional<? extends AbstractCookingRecipe> found = manager.getRecipeFor(RecipeType.SMELTING, input, level).map(h -> h.value());
        if (found.isEmpty()) found = manager.getRecipeFor(RecipeType.BLASTING, input, level).map(h -> h.value());
        if (found.isEmpty()) found = manager.getRecipeFor(RecipeType.SMOKING, input, level).map(h -> h.value());
        return found;
    }

    // ---------------------------------------------------------------- working

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusFurnaceBlockEntity furnace) {
        if (level instanceof ServerLevel server) furnace.work(server);
    }

    private void work(ServerLevel level) {
        int[] want = new int[SLOTS];
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack in = items.get(i);
            var recipe = recipeFor(level, in);
            if (recipe.isEmpty()) {
                progress[i] = 0;
                continue;
            }
            ItemStack result = recipe.get().assemble(new SingleRecipeInput(in));
            ItemStack out = items.get(SLOTS + i);
            boolean room = out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
            if (room) want[n++] = i;
        }
        wanting = n;
        int run = (int) Math.min(n, energy.stored() / ENERGY_PER_SLOT);
        working = run;
        if (run > 0) {
            energy.use((long) run * ENERGY_PER_SLOT);
            // The slots that run take turns when the energy is short, so all of them cook the same amount over time
            int start = Math.floorMod(rotation, n);
            for (int k = 0; k < run; k++) {
                int i = want[(start + k) % n];
                if (++progress[i] >= COOK_TICKS) finish(level, i);
            }
            rotation = (start + run) % n;
            setChanged();
        }
        boolean lit = run > 0;
        BlockState state = getBlockState();
        if (state.hasProperty(NexusFurnaceBlock.LIT) && state.getValue(NexusFurnaceBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(NexusFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private void finish(ServerLevel level, int i) {
        progress[i] = 0;
        ItemStack in = items.get(i);
        var recipe = recipeFor(level, in);
        if (recipe.isEmpty()) return;
        ItemStack result = recipe.get().assemble(new SingleRecipeInput(in));
        ItemStack out = items.get(SLOTS + i);
        if (out.isEmpty()) items.set(SLOTS + i, result.copy());
        else if (ItemStack.isSameItemSameComponents(out, result)) out.grow(result.getCount());
        else return;
        in.shrink(1);
        // The recipe's amount in thousandths of a point, so 0.7 (a float that is really 0.69999999) is exactly 0.7
        addExperience(Math.round(recipe.get().experience() * 1000.0) * XP_MULTIPLIER / 1000.0);
    }

    /**
     * Banks experience. The bank is kept to a thousandth of a point, so adding up many small amounts never drifts below the
     * whole number it should reach (ten times 0.7 is 7, not 6.9999999).
     */
    public void addExperience(double points) {
        experience = Math.round((experience + points) * 1000.0) / 1000.0;
        setChanged();
    }

    /** Gives the player the whole points of experience collected so far; the fraction stays. Returns how many. */
    public int takeExperience(Player player) {
        int points = (int) Math.floor(experience);
        if (points <= 0) return 0;
        experience = Math.round((experience - points) * 1000.0) / 1000.0;
        player.giveExperiencePoints(points);
        setChanged();
        return points;
    }

    // ---------------------------------------------------------------- sharing a stack out evenly

    /**
     * Puts as much of {@code stack} as fits into the input slots, always into the slot holding the least of it, so a
     * stack is shared out evenly. With {@code simulate} nothing changes. Returns how many went in.
     */
    public int insertEvenly(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || level == null || recipeFor(level, stack).isEmpty()) return 0;
        int moved = 0;
        int[] counts = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) counts[i] = items.get(i).getCount();
        int limit = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        while (moved < stack.getCount()) {
            int best = -1;
            for (int i = 0; i < SLOTS; i++) {
                ItemStack in = items.get(i);
                boolean fits = in.isEmpty() || ItemStack.isSameItemSameComponents(in, stack);
                if (!fits || counts[i] >= limit) continue;
                if (best < 0 || counts[i] < counts[best]) best = i;
            }
            if (best < 0) break;
            counts[best]++;
            moved++;
        }
        if (!simulate && moved > 0) {
            for (int i = 0; i < SLOTS; i++) {
                int add = counts[i] - items.get(i).getCount();
                if (add <= 0) continue;
                if (items.get(i).isEmpty()) items.set(i, stack.copyWithCount(add));
                else items.get(i).grow(add);
            }
            setChanged();
        }
        return moved;
    }

    private final class Inputs extends SnapshotParticipant<List<ItemStack>> implements InsertionOnlyStorage<ItemVariant> {
        @Override
        public long insert(ItemVariant variant, long maxAmount, TransactionContext tx) {
            if (variant.isBlank() || maxAmount <= 0) return 0;
            ItemStack stack = variant.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            int fits = insertEvenly(stack, true);
            if (fits <= 0) return 0;
            updateSnapshots(tx);
            return insertEvenly(stack.copyWithCount(fits), false);
        }

        @Override
        public Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<ItemVariant>> iterator() {
            return java.util.Collections.emptyIterator();
        }

        @Override
        protected List<ItemStack> createSnapshot() {
            List<ItemStack> copy = new java.util.ArrayList<>();
            for (int i = 0; i < SLOTS; i++) copy.add(items.get(i).copy());
            return copy;
        }

        @Override
        protected void readSnapshot(List<ItemStack> snapshot) {
            for (int i = 0; i < SLOTS; i++) items.set(i, snapshot.get(i));
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    }

    // ---------------------------------------------------------------- container: eight inputs and eight outputs

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < SLOTS && recipeFor(level, stack).isPresent();
    }

    /** Hoppers and pipes go in from the top and sides and out from the bottom. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == Direction.DOWN || slot >= SLOTS || !canPlaceItem(slot, stack)) return false;
        // Only into the slot with the least of it, so a hopper shares a stack out evenly too
        int mine = items.get(slot).getCount();
        int limit = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        for (int i = 0; i < SLOTS; i++) {
            ItemStack in = items.get(i);
            if ((in.isEmpty() || ItemStack.isSameItemSameComponents(in, stack)) && in.getCount() < limit && in.getCount() < mine) return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot >= SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new NexusFurnaceMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("energy", energy.stored());
        output.putIntArray("progress", progress);
        output.putDouble("experience", experience);
        output.putInt("rotation", rotation);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.set(input.getLongOr("energy", 0L));
        int[] saved = input.getIntArray("progress").orElse(new int[0]);
        for (int i = 0; i < SLOTS; i++) progress[i] = i < saved.length ? Math.max(0, Math.min(COOK_TICKS, saved[i])) : 0;
        experience = Math.max(0, input.getDoubleOr("experience", 0.0));
        rotation = Math.max(0, input.getIntOr("rotation", 0));
    }
}
