package net.storagenexus.terminal;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.StorageNexus;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModMenus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Storage / Crafting Terminal (also opened by Storage Tablets from any distance or dimension).
 * The network's contents are not slots; they are synced with {@link TerminalSyncPayload} and
 * clicks come back as {@link TerminalActionPayload}. The Crafting Terminal works with the vanilla
 * recipe book, which counts and takes ingredients from the network as well as the inventory.
 */
public class TerminalMenu extends AbstractCraftingMenu {
    public static final int GRID_X = 8, GRID_Y = 18, COLS = 9, ROWS = 6;
    public static final int MIN_ROWS = 3, MAX_ROWS = 12;

    /** How many grid rows the client shows; the client picks this from the screen height. */
    @FunctionalInterface
    public interface RowChooser {
        int rows(boolean crafting);
    }

    public static RowChooser clientRows = crafting -> ROWS;
    public static final int WIDTH = 194;
    /** Menu button id: empty the crafting grid into the network (else your inventory, else the ground). */
    public static final int CLEAR_GRID = 1;
    private static final int MAX_PART_BYTES = 256 * 1024;
    private static final int MAX_ENTRY_BYTES = 512 * 1024;

    private final boolean crafting;
    private final Player player;

    // Server side
    private final @Nullable ServerLevel networkLevel;
    private final @Nullable BlockPos terminalPos;
    private final Predicate<Player> validity;
    private final @Nullable Runnable onClose;
    private int syncTimer;
    private int lastHash = Integer.MIN_VALUE;

    private final int rows;
    private int playerSlotStart;
    private @Nullable Slot resultSlot;
    private final List<Slot> inputSlots = new ArrayList<>();

    // Client side
    private List<TerminalSyncPayload.ItemEntry> clientItems = new ArrayList<>();
    private List<TerminalSyncPayload.FluidEntry> clientFluids = new ArrayList<>();
    private boolean clientOnline = true;
    private int clientRevision;
    private @Nullable List<TerminalSyncPayload.ItemEntry> pendingItems;
    private List<TerminalSyncPayload.FluidEntry> pendingFluids = List.of();

    /** Client constructor. Slot positions only matter on the client, so it picks the row count. */
    public TerminalMenu(int id, Inventory inventory, Boolean crafting) {
        this(id, inventory, crafting, null, null, p -> true, null, clientRows.rows(crafting));
    }

    public TerminalMenu(int id, Inventory inventory, boolean crafting, @Nullable ServerLevel networkLevel,
                        @Nullable BlockPos terminalPos, Predicate<Player> validity, @Nullable Runnable onClose) {
        this(id, inventory, crafting, networkLevel, terminalPos, validity, onClose, ROWS);
    }

    private TerminalMenu(int id, Inventory inventory, boolean crafting, @Nullable ServerLevel networkLevel,
                         @Nullable BlockPos terminalPos, Predicate<Player> validity, @Nullable Runnable onClose, int rows) {
        super(ModMenus.TERMINAL, id, 3, 3);
        this.crafting = crafting;
        this.rows = Math.clamp(rows, MIN_ROWS, MAX_ROWS);
        this.player = inventory.player;
        this.networkLevel = networkLevel;
        this.terminalPos = terminalPos;
        this.validity = validity;
        this.onClose = onClose;

        if (crafting) {
            int top = craftingTop(this.rows);
            resultSlot = addSlot(new TerminalResultSlot(inventory.player, craftSlots, resultSlots, 0, 124, top + 18));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    inputSlots.add(addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, top + row * 18)));
                }
            }
        }
        playerSlotStart = slots.size();
        int invTop = inventoryTop(crafting, this.rows);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, GRID_X + col * 18, invTop + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, GRID_X + col * 18, invTop + 58));
    }

    /** Top of the 3x3 crafting grid, just under the item grid. */
    public static int craftingTop(int rows) {
        return GRID_Y + rows * 18 + 6;
    }

    public static int inventoryTop(boolean crafting, int rows) {
        return GRID_Y + rows * 18 + (crafting ? 72 : 14);
    }

    public static int imageHeight(boolean crafting, int rows) {
        return inventoryTop(crafting, rows) + 82;
    }

    /** The most rows that fit a screen of this (GUI-scaled) height, with a small margin. */
    public static int rowsFor(boolean crafting, int screenHeight) {
        int spare = screenHeight - 8 - imageHeight(crafting, 0);
        return Math.clamp(spare / 18, MIN_ROWS, MAX_ROWS);
    }

    public boolean isCrafting() { return crafting; }
    public int rows() { return rows; }

    // ---- Network access ---------------------------------------------------------------------

    private @Nullable StorageNetwork network() {
        if (networkLevel == null || terminalPos == null) return null;
        if (!networkLevel.isLoaded(terminalPos)) return null;
        return StorageNetwork.find(networkLevel, terminalPos);
    }

    @Override
    public boolean stillValid(Player player) {
        return validity.test(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || networkLevel == null) return;
        if (--syncTimer > 0) return;
        syncTimer = 10;
        StorageNetwork net = network();
        List<TerminalSyncPayload.ItemEntry> items = new ArrayList<>();
        List<TerminalSyncPayload.FluidEntry> fluids = new ArrayList<>();
        if (net != null) {
            for (Map.Entry<ItemVariant, Long> e : net.listItems().entrySet()) {
                items.add(new TerminalSyncPayload.ItemEntry(e.getKey().toStack(1), e.getValue()));
            }
            for (Map.Entry<FluidVariant, Long> e : net.listFluids().entrySet()) {
                fluids.add(new TerminalSyncPayload.FluidEntry(e.getKey().getFluid(), e.getValue()));
            }
        }
        items.sort(Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.stack().getItem()).toString()));
        int hash = (net != null ? 1 : 0);
        for (TerminalSyncPayload.ItemEntry e : items) hash = 31 * hash + ItemStack.hashItemAndComponents(e.stack()) * 17 + Long.hashCode(e.count());
        for (TerminalSyncPayload.FluidEntry e : fluids) hash = 31 * hash + e.fluid().hashCode() + Long.hashCode(e.amount());
        if (hash == lastHash) return;
        lastHash = hash;
        sendInParts(sp, net != null, items, fluids);
    }

    /** Splits the item list by encoded size so no payload gets near the 1 MiB limit. */
    private static void sendInParts(ServerPlayer sp, boolean online, List<TerminalSyncPayload.ItemEntry> items,
                                    List<TerminalSyncPayload.FluidEntry> fluids) {
        RegistryFriendlyByteBuf scratch = new RegistryFriendlyByteBuf(Unpooled.buffer(), sp.registryAccess());
        List<TerminalSyncPayload.ItemEntry> part = new ArrayList<>();
        int partBytes = 0;
        boolean first = true;
        try {
            for (TerminalSyncPayload.ItemEntry e : items) {
                scratch.clear();
                TerminalSyncPayload.ItemEntry.STREAM_CODEC.encode(scratch, e);
                int size = scratch.writerIndex();
                if (size > MAX_ENTRY_BYTES) {
                    StorageNexus.LOG.warn("Not showing {} in a terminal: its data is {} bytes", e.stack().getItem(), size);
                    continue;
                }
                if (!part.isEmpty() && partBytes + size > MAX_PART_BYTES) {
                    ServerPlayNetworking.send(sp, new TerminalSyncPayload(online, first, false, part, first ? fluids : List.of()));
                    first = false;
                    part = new ArrayList<>();
                    partBytes = 0;
                }
                part.add(e);
                partBytes += size;
            }
        } finally {
            scratch.release();
        }
        ServerPlayNetworking.send(sp, new TerminalSyncPayload(online, first, true, part, first ? fluids : List.of()));
    }

    private void forceResync() {
        syncTimer = 0;
        lastHash = Integer.MIN_VALUE;
    }

    // ---- Client ------------------------------------------------------------------------------

    public void receiveSync(TerminalSyncPayload payload) {
        if (payload.first()) {
            pendingItems = new ArrayList<>();
            pendingFluids = payload.fluids();
        }
        if (pendingItems == null) return; // opened mid-update; the next full update fixes it
        pendingItems.addAll(payload.items());
        if (!payload.last()) return;
        clientOnline = payload.online();
        clientItems = pendingItems;
        clientFluids = new ArrayList<>(pendingFluids);
        pendingItems = null;
        clientRevision++;
        // The recipe book re-checks what is craftable when the inventory changes; the
        // network's contents count too, so nudge it.
        player.getInventory().setChanged();
    }

    public List<TerminalSyncPayload.ItemEntry> clientItems() { return clientItems; }
    public List<TerminalSyncPayload.FluidEntry> clientFluids() { return clientFluids; }
    public boolean clientOnline() { return clientOnline; }
    public int clientRevision() { return clientRevision; }

    // ---- Actions (server) ---------------------------------------------------------------------

    public void handleAction(ServerPlayer sp, TerminalActionPayload payload) {
        StorageNetwork net = network();
        if (net == null) return;
        ItemStack carried = getCarried();
        switch (payload.action()) {
            case TerminalActionPayload.TAKE_ONE -> {
                if (!carried.isEmpty() || payload.item().isEmpty()) return;
                setCarried(extract(net, ItemVariant.of(payload.item()), 1));
            }
            case TerminalActionPayload.TAKE_STACK, TerminalActionPayload.TAKE_HALF -> {
                if (!carried.isEmpty() || payload.item().isEmpty()) return;
                ItemVariant v = ItemVariant.of(payload.item());
                int want = payload.item().getMaxStackSize();
                if (payload.action() == TerminalActionPayload.TAKE_HALF) {
                    // Half of what is actually extractable, so nothing has to be put back.
                    want = (int) Math.min(want, (simulateExtract(net, v, want) + 1) / 2);
                }
                setCarried(extract(net, v, want));
            }
            case TerminalActionPayload.TAKE_TO_INVENTORY -> {
                if (payload.item().isEmpty()) return;
                ItemStack taken = extract(net, ItemVariant.of(payload.item()), payload.item().getMaxStackSize());
                sp.getInventory().add(taken);
                if (!taken.isEmpty()) insert(net, taken);
                if (!taken.isEmpty()) sp.drop(taken, false, Prediction.SERVER_ONLY); // came from an extract-only source
            }
            case TerminalActionPayload.INSERT_CARRIED -> {
                if (carried.isEmpty()) return;
                insert(net, carried);
                setCarried(carried);
            }
            case TerminalActionPayload.INSERT_ONE -> {
                if (carried.isEmpty()) return;
                ItemStack one = carried.copyWithCount(1);
                insert(net, one);
                if (one.isEmpty()) carried.shrink(1);
                setCarried(carried);
            }
            case TerminalActionPayload.FILL_BUCKET -> {
                Fluid fluid = payload.fluid();
                // Comes from the client: FluidVariant.of throws on flowing fluids.
                if (!carried.is(Items.BUCKET) || fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())
                        || fluid.getBucket() == Items.AIR) return;
                try (Transaction tx = Transaction.openOuter()) {
                    if (net.extractFluid(FluidVariant.of(fluid), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                        tx.commit();
                        ItemStack filled = new ItemStack(fluid.getBucket());
                        carried.shrink(1);
                        if (carried.isEmpty()) setCarried(filled);
                        else if (!sp.getInventory().add(filled)) sp.drop(filled, false, Prediction.SERVER_ONLY);
                    }
                }
            }
            case TerminalActionPayload.EMPTY_BUCKET -> {
                Fluid fluid = fluidInBucket(carried);
                if (fluid == null || carried.getCount() != 1) return;
                try (Transaction tx = Transaction.openOuter()) {
                    if (net.insertFluid(FluidVariant.of(fluid), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET) {
                        tx.commit();
                        setCarried(new ItemStack(Items.BUCKET));
                    }
                }
            }
            default -> {}
        }
        forceResync();
        broadcastChanges();
    }

    private static @Nullable Fluid fluidInBucket(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.BUCKET)) return null;
        for (Fluid f : BuiltInRegistries.FLUID) {
            if (f != Fluids.EMPTY && f.isSource(f.defaultFluidState()) && f.getBucket() == stack.getItem()) return f;
        }
        return null;
    }

    private static long simulateExtract(StorageNetwork net, ItemVariant v, int max) {
        try (Transaction tx = Transaction.openOuter()) {
            return net.extractItem(v, max, tx);
        }
    }

    private static ItemStack extract(StorageNetwork net, ItemVariant v, int max) {
        try (Transaction tx = Transaction.openOuter()) {
            long got = net.extractItem(v, max, tx);
            tx.commit();
            return got > 0 ? v.toStack((int) got) : ItemStack.EMPTY;
        }
    }

    /** Inserts as much as fits and shrinks the stack by that amount. */
    private static void insert(StorageNetwork net, ItemStack stack) {
        if (stack.isEmpty()) return;
        try (Transaction tx = Transaction.openOuter()) {
            long n = net.insertItem(ItemVariant.of(stack), stack.getCount(), tx);
            tx.commit();
            stack.shrink((int) Math.min(n, stack.getCount()));
        }
    }

    // ---- Shift-click ----------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (crafting && index == 0) {
            // Crafting result: craft into the player inventory; the grid refills from the network.
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, playerSlotStart, slots.size(), true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            slot.onTake(p, original);
            return original;
        }
        if (index < playerSlotStart) {
            // Crafting grid -> player inventory
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, playerSlotStart, slots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return original;
        }
        // Player inventory -> network
        if (!p.level().isClientSide()) {
            StorageNetwork net = network();
            if (net != null) {
                ItemStack stack = slot.getItem();
                insert(net, stack);
                slot.setChanged();
                forceResync();
            }
        }
        return ItemStack.EMPTY;
    }

    // ---- Crafting -------------------------------------------------------------------------------

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (crafting && container == craftSlots && player instanceof ServerPlayer sp) {
            ServerLevel level = (ServerLevel) sp.level();
            CraftingInput input = craftSlots.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            ItemStack result = recipe.map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
            resultSlots.setItem(0, result);
            broadcastChanges();
        }
    }

    /** After a craft, refills emptied grid slots with the same ingredient from the network. */
    private class TerminalResultSlot extends ResultSlot {
        TerminalResultSlot(Player player, CraftingContainer craft, ResultContainer result, int slot, int x, int y) {
            super(player, craft, result, slot, x, y);
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            ItemStack[] before = new ItemStack[9];
            for (int i = 0; i < 9; i++) before[i] = craftSlots.getItem(i).copy();
            super.onTake(p, stack);
            if (p.level().isClientSide()) return;
            StorageNetwork net = network();
            if (net == null) return;
            for (int i = 0; i < 9; i++) {
                if (craftSlots.getItem(i).isEmpty() && !before[i].isEmpty()) {
                    ItemStack refill = extract(net, ItemVariant.of(before[i]), 1);
                    if (!refill.isEmpty()) craftSlots.setItem(i, refill);
                }
            }
            slotsChanged(craftSlots);
        }
    }

    // ---- Recipe book -----------------------------------------------------------------------------

    @Override
    public Slot getResultSlot() {
        return resultSlot;
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return inputSlots;
    }

    @Override
    protected Player owner() {
        return player;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id != CLEAR_GRID || !crafting) return false;
        if (!p.level().isClientSide()) {
            clearGrid(network(), p.getInventory());
            slotsChanged(craftSlots);
            forceResync();
            broadcastChanges();
        }
        return true;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    /** What the recipe book may craft from: the grid plus, on the client, everything in the network. */
    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents contents) {
        super.fillCraftSlotsStackedContents(contents);
        if (!player.level().isClientSide()) return;
        for (TerminalSyncPayload.ItemEntry e : clientItems) accountNetworkStack(contents, e.stack(), e.count());
    }

    private static void accountNetworkStack(StackedItemContents contents, ItemStack stack, long count) {
        if (!Inventory.isUsableForCrafting(stack)) return;
        int n = (int) Math.min(count, 1_000_000);
        contents.accountStack(stack.copyWithCount(n), n);
    }

    /**
     * Fills the grid for a recipe clicked in the recipe book, like vanilla's ServerPlaceRecipe
     * but taking ingredients from the network first and the player's inventory second.
     */
    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear,
                                                          RecipeHolder<?> holder, ServerLevel level, Inventory inventory) {
        if (!crafting || !(holder.value() instanceof CraftingRecipe recipe)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        StorageNetwork net = network();

        StackedItemContents available = new StackedItemContents();
        inventory.fillStackedContents(available);
        craftSlots.fillStackedContents(available);
        if (net != null) {
            for (Map.Entry<ItemVariant, Long> e : net.listItems().entrySet()) {
                accountNetworkStack(available, e.getKey().toStack(1), e.getValue());
            }
        }
        if (!available.canCraft(recipe, null)) {
            clearGrid(net, inventory);
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }

        boolean alreadyPlaced = recipe.matches(craftSlots.asCraftInput(), level);
        int biggest = available.getBiggestCraftableStack(recipe, null);
        int amount;
        if (useMaxItems) {
            amount = biggest;
        } else if (alreadyPlaced) {
            int smallest = Integer.MAX_VALUE;
            for (Slot s : inputSlots) if (!s.getItem().isEmpty()) smallest = Math.min(smallest, s.getItem().getCount());
            amount = smallest == Integer.MAX_VALUE ? 1 : smallest + 1;
        } else {
            amount = 1;
        }
        List<Holder<Item>> used = new ArrayList<>();
        if (!available.canCraft(recipe, amount, used::add)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        int clamped = amount;
        for (Holder<Item> item : used) clamped = Math.min(clamped, new ItemStack(item).getMaxStackSize());
        if (clamped != amount) {
            used.clear();
            if (!available.canCraft(recipe, clamped, used::add)) return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        final int perSlot = clamped;

        clearGrid(net, inventory);
        PlaceRecipeHelper.placeRecipe(3, 3, recipe, recipe.placementInfo().slotsToIngredientIndex(),
                (ingredientIndex, gridIndex, x, y) -> {
                    if (ingredientIndex == -1) return;
                    Item item = used.get(ingredientIndex).value();
                    ItemStack placed = net != null ? extract(net, ItemVariant.of(item), perSlot) : ItemStack.EMPTY;
                    // Top up from the player's inventory if the network ran short
                    for (int i = 0; i < inventory.getContainerSize() && placed.getCount() < perSlot; i++) {
                        ItemStack s = inventory.getItem(i);
                        if (!s.is(item) || !Inventory.isUsableForCrafting(s)) continue;
                        ItemStack taken = s.split(perSlot - placed.getCount());
                        if (placed.isEmpty()) placed = taken;
                        else placed.grow(taken.getCount());
                    }
                    inputSlots.get(gridIndex).set(placed);
                });
        inventory.setChanged();
        slotsChanged(craftSlots);
        forceResync();
        return RecipeBookMenu.PostPlaceAction.NOTHING;
    }

    /** Puts the grid's contents back: into the network, else the inventory, else on the ground. */
    private void clearGrid(@Nullable StorageNetwork net, Inventory inventory) {
        for (Slot s : inputSlots) {
            ItemStack stack = s.getItem().copy();
            if (stack.isEmpty()) continue;
            if (net != null) insert(net, stack);
            if (!stack.isEmpty()) inventory.placeItemBackInInventory(stack, false, Prediction.SERVER_ONLY);
            s.set(ItemStack.EMPTY);
        }
        resultSlots.clearContent();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p.level().isClientSide()) return;
        if (crafting) {
            StorageNetwork net = network();
            for (int i = 0; i < 9; i++) {
                ItemStack s = craftSlots.removeItemNoUpdate(i);
                if (s.isEmpty()) continue;
                if (net != null) insert(net, s);
                if (!s.isEmpty() && !p.getInventory().add(s)) p.drop(s, false, Prediction.SERVER_ONLY);
            }
        }
        if (onClose != null) onClose.run();
    }
}
