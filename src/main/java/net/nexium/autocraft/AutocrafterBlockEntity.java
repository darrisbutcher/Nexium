package net.nexium.autocraft;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.energy.EnergyBuffer;
import net.nexium.network.NetworkBlock;
import net.nexium.network.StorageNetwork;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Autocrafter: a network block that holds up to {@value #SLOTS} encoded patterns and does what they say, on Nexus Energy, when
 * an {@link AutocraftingService} job asks it to. A crafting pattern is crafted here (it takes {@value #CRAFT_TICKS} ticks and
 * {@value #CRAFT_COST} NE a craft, up to {@value #MAX_BATCH} at a time) and the result goes back into the network. A processing
 * pattern pushes its inputs into a block next to the Autocrafter ({@value #PUSH_COST} NE a craft); what the machine makes has to
 * find its own way back to the network (an Import/Export Bus, a conduit).
 */
public class AutocrafterBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS = 9;
    public static final long BUFFER = 20_000, CRAFT_COST = 100, PUSH_COST = 50;
    public static final int CRAFT_TICKS = 10, MAX_BATCH = 8;

    /** Why a start did not happen. */
    public enum Start { STARTED, BUSY, NO_ENERGY, NO_ITEMS, NO_MACHINE, BAD_PATTERN }

    private final EnergyBuffer energy = EnergyBuffer.machine(BUFFER, this::setChanged);
    private final SimpleContainer patterns = new SimpleContainer(SLOTS) {
        @Override public void setChanged() {
            super.setChanged();
            AutocrafterBlockEntity.this.setChanged();
            net.nexium.util.StorageEvents.storageChanged();
        }
    };
    /** What a craft in progress will put into the network, and how many crafts that is. */
    private final List<ItemStack> pending = new ArrayList<>();
    private int pendingCrafts, ticksLeft;
    private UUID job;
    /** Crafts finished since a job last asked. */
    private int completed;
    private int rotor;
    /** How many crafts the last start did. */
    private int started;

    public AutocrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCRAFTER, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    public SimpleContainer patterns() { return patterns; }

    public int started() { return started; }

    /** The pattern in a slot, or null. */
    public Pattern pattern(int slot) {
        ItemStack stack = patterns.getItem(slot);
        return PatternItem.isEncoded(stack) ? stack.get(ModComponents.PATTERN) : null;
    }

    public boolean busy() { return !pending.isEmpty(); }

    /** The job a craft in progress is for. */
    public UUID job() { return job; }

    /** Hands over the crafts finished for this job since the last call. */
    public int takeCompleted(UUID forJob) {
        if (completed == 0 || (job != null && !job.equals(forJob))) return 0;
        int n = completed;
        completed = 0;
        return n;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutocrafterBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (pending.isEmpty()) return;
        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }
        if (level == null) return;
        StorageNetwork net = StorageNetwork.find(level, worldPosition);
        if (net == null) return;
        try (Transaction tx = Transaction.openOuter()) {
            for (ItemStack stack : pending) {
                if (stack.isEmpty()) continue;
                long put = net.insertItem(ItemVariant.of(stack), stack.getCount(), tx);
                stack.shrink((int) put);
            }
            tx.commit();
        }
        pending.removeIf(ItemStack::isEmpty);
        if (pending.isEmpty()) {
            completed += pendingCrafts;
            pendingCrafts = 0;
        }
        setChanged();
    }

    // ---------------------------------------------------------------- starting crafts

    /** Starts up to {@code wanted} crafts of a crafting pattern, as many as the energy allows (and {@value #MAX_BATCH}). */
    public Start startCrafting(ServerLevel level, StorageNetwork net, Pattern pattern, long wanted, UUID forJob) {
        started = 0;
        if (!pending.isEmpty()) return Start.BUSY;
        if (pattern.processing() || !pattern.valid()) return Start.BAD_PATTERN;
        List<ItemStack> grid = new ArrayList<>(9);
        for (ItemStack stack : pattern.inputs()) grid.add(stack.copyWithCount(1));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) return Start.BAD_PATTERN;
        ItemStack result = recipe.get().value().assemble(input);
        if (result.isEmpty() || !ItemVariant.of(result).equals(ItemVariant.of(pattern.primary()))) return Start.BAD_PATTERN;
        long affordable = energy.stored() / CRAFT_COST;
        if (affordable <= 0) return Start.NO_ENERGY;
        long crafts = Math.min(Math.min(wanted, MAX_BATCH), affordable);
        for (; crafts >= 1; crafts = crafts > 1 ? 1 : 0) {
            if (extractInputs(net, pattern, crafts)) {
                energy.remove(crafts * CRAFT_COST, false);
                ItemStack made = result.copy();
                made.setCount((int) (made.getCount() * crafts));
                pending.add(made);
                for (ItemStack stack : recipe.get().value().getRemainingItems(input)) {
                    if (!stack.isEmpty()) pending.add(stack.copyWithCount((int) (stack.getCount() * crafts)));
                }
                pendingCrafts = (int) crafts;
                ticksLeft = CRAFT_TICKS;
                job = forJob;
                started = (int) crafts;
                setChanged();
                return Start.STARTED;
            }
        }
        return Start.NO_ITEMS;
    }

    private static boolean extractInputs(StorageNetwork net, Pattern pattern, long crafts) {
        try (Transaction tx = Transaction.openOuter()) {
            for (var need : pattern.needs().entrySet()) {
                long want = need.getValue() * crafts;
                if (net.extractItem(need.getKey(), want, tx) < want) return false;
            }
            tx.commit();
            return true;
        }
    }

    /**
     * Pushes up to {@code wanted} crafts' worth of a processing pattern's inputs into the blocks next to this one, one craft at a
     * time, each into one machine that takes all of it. Returns how many went in; {@link #started()} is also set.
     */
    public Start push(ServerLevel level, StorageNetwork net, Pattern pattern, long wanted) {
        started = 0;
        if (!pattern.processing() || !pattern.valid()) return Start.BAD_PATTERN;
        List<Storage<ItemVariant>> machines = machines(level);
        if (machines.isEmpty()) return Start.NO_MACHINE;
        long limit = Math.min(wanted, MAX_BATCH);
        Start why = Start.STARTED;
        for (int n = 0; n < limit; n++) {
            if (energy.stored() < PUSH_COST) {
                why = Start.NO_ENERGY;
                break;
            }
            if (!pushOne(net, pattern, machines)) {
                why = Start.NO_ITEMS;
                break;
            }
            energy.remove(PUSH_COST, false);
            started++;
        }
        if (started > 0) {
            setChanged();
            return Start.STARTED;
        }
        return why;
    }

    private boolean pushOne(StorageNetwork net, Pattern pattern, List<Storage<ItemVariant>> machines) {
        for (int m = 0; m < machines.size(); m++) {
            Storage<ItemVariant> machine = machines.get((rotor + m) % machines.size());
            try (Transaction tx = Transaction.openOuter()) {
                boolean ok = true;
                for (var need : pattern.needs().entrySet()) {
                    long want = need.getValue();
                    if (net.extractItem(need.getKey(), want, tx) < want || machine.insert(need.getKey(), want, tx) < want) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    tx.commit();
                    rotor = (rotor + m + 1) % machines.size();
                    return true;
                }
            }
        }
        return false;
    }

    /** What a processing pattern pushes into: the item inventories of the blocks touching this one, other than network blocks. */
    private List<Storage<ItemVariant>> machines(ServerLevel level) {
        List<Storage<ItemVariant>> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BlockPos next = worldPosition.relative(d);
            if (!level.hasChunkAt(next) || level.getBlockState(next).getBlock() instanceof NetworkBlock) continue;
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, next, d.getOpposite());
            if (storage != null && storage.supportsInsertion()) out.add(storage);
        }
        return out;
    }

    // ---------------------------------------------------------------- menu and saving

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AutocrafterMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, patterns);
            for (ItemStack stack : pending) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.stored());
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) list.add(patterns.getItem(i));
        output.store("patterns", ItemStack.OPTIONAL_CODEC.listOf(), list);
        output.store("pending", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(pending));
        output.putInt("pending_crafts", pendingCrafts);
        output.putInt("ticks_left", ticksLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.set(input.getLongOr("energy", 0L));
        List<ItemStack> list = input.read("patterns", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) patterns.setItem(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        pending.clear();
        pending.addAll(input.read("pending", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        pending.removeIf(ItemStack::isEmpty);
        pendingCrafts = input.getIntOr("pending_crafts", 0);
        ticksLeft = input.getIntOr("ticks_left", 0);
    }
}
