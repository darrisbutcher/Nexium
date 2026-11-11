package net.storagenexus.builder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.storagenexus.aiot.module.NexusLink;
import net.storagenexus.network.StorageNetwork;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a Construction Builder click on the server, and undoes the last builds. Blocks come from the player's inventory
 * first, then from the storage network the builder's Nexus Link points at; creative players need none. Each block placed
 * costs one point of durability. Exchange gives back what breaking the old block would drop. The last {@value #UNDO_STEPS} builds of each player are remembered (until they log out),
 * so the tool menu's Undo can take them back one at a time, returning the blocks.
 */
public final class BuilderService {
    public static final int UNDO_STEPS = 2;

    /**
     * One block built: where, what went there, what was there, the item it used and how many, and (Exchange) what the
     * old block dropped into the player's inventory.
     */
    record Entry(BlockPos pos, BlockState placed, BlockState previous, Item used, int count, List<ItemStack> returned) {}

    record Batch(ResourceKey<Level> dimension, List<Entry> entries) {}

    private static final Map<UUID, Deque<Batch>> HISTORY = new HashMap<>();

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> HISTORY.remove(handler.getPlayer().getUUID()));
    }

    /** Builds what {@link BuilderPlan} plans for this click. Returns how many blocks went down. */
    public static int build(ServerLevel level, ServerPlayer player, ItemStack builder, BlockPos clicked, Direction face) {
        BuilderPlan plan = BuilderPlan.of(level, player, builder, clicked, face);
        if (plan.isEmpty()) return 0;
        boolean creative = player.hasInfiniteMaterials();
        GlobalPos link = BuilderSettings.activeLink(builder);
        StorageNetwork network = null;
        boolean networkLooked = false;
        List<Entry> done = new ArrayList<>();
        for (BlockPos pos : plan.targets()) {
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, builder)) continue;
            BlockState previous = level.getBlockState(pos);
            if (!plan.exchange() && !BuilderPlan.canPlace(level, player, pos, plan.state())) continue;
            if (!creative) {
                // All the items this block uses (two slabs for a double slab), or none
                int taken = 0;
                while (taken < plan.perBlock() && takeFromInventory(player.getInventory(), plan.item())) taken++;
                if (taken < plan.perBlock() && link != null) {
                    if (!networkLooked) {
                        network = NexusLink.network(player, link);
                        networkLooked = true;
                    }
                    while (taken < plan.perBlock() && network != null && takeFromNetwork(network, plan.item())) taken++;
                }
                if (taken < plan.perBlock()) {
                    if (taken > 0) player.getInventory().placeItemBackInInventory(new ItemStack(plan.item(), taken), false, Prediction.SERVER_ONLY);
                    player.sendOverlayMessage(Component.translatable("message.storagenexus.builder.out_of", new ItemStack(plan.item()).getHoverName()));
                    break;
                }
            }
            List<ItemStack> returned = List.of();
            if (plan.exchange()) {
                // The old block comes back as what breaking it drops: cobblestone for stone, nothing for glass
                if (!creative) {
                    returned = List.copyOf(Block.getDrops(previous, level, pos, null, player, ItemStack.EMPTY));
                    for (ItemStack drop : returned) player.getInventory().placeItemBackInInventory(drop.copy(), false, Prediction.SERVER_ONLY);
                }
                level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(previous));
            }
            // Fences, panes and walls join what is next to them here, as a block placed by hand would
            BlockState placed = Block.updateFromNeighbourShapes(plan.state(), level, pos);
            if (!placed.is(plan.state().getBlock())) placed = plan.state();
            level.setBlock(pos, placed, Block.UPDATE_ALL);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
            done.add(new Entry(pos.immutable(), placed, previous, plan.item(), plan.perBlock(), returned));
            if (!creative && builder.isDamageableItem()) {
                builder.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (builder.isEmpty()) break;
            }
        }
        if (!done.isEmpty()) {
            SoundType sound = plan.state().getSoundType();
            BlockPos at = done.getFirst().pos();
            level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            // Remember each block as it stands now: the ones placed after it may have joined on to it
            List<Entry> asBuilt = new ArrayList<>(done.size());
            for (Entry e : done) asBuilt.add(new Entry(e.pos(), level.getBlockState(e.pos()), e.previous(), e.used(), e.count(), e.returned()));
            Deque<Batch> history = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
            history.addLast(new Batch(level.dimension(), List.copyOf(asBuilt)));
            while (history.size() > UNDO_STEPS) history.removeFirst();
        }
        return done.size();
    }

    /** How many builds Undo can still take back for this player. */
    public static int undoLeft(ServerPlayer player) {
        Deque<Batch> history = HISTORY.get(player.getUUID());
        return history == null ? 0 : history.size();
    }

    /**
     * Takes back the player's last build: each block still as it was built goes back to what was there, and its item is
     * returned (Exchange takes back what the old block dropped for you, so nothing is doubled; a spot whose drops you no
     * longer have is left as it is). Returns how many blocks were undone, or -1 if there was nothing to undo.
     */
    public static int undo(ServerPlayer player) {
        Deque<Batch> history = HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) return -1;
        Batch batch = history.removeLast();
        ServerLevel level = player.level().getServer().getLevel(batch.dimension());
        if (level == null) return 0;
        boolean creative = player.hasInfiniteMaterials();
        int undone = 0;
        List<Entry> entries = new ArrayList<>(batch.entries());
        java.util.Collections.reverse(entries);
        // Which blocks still stand as built is decided before any is taken back: taking one back changes how the
        // fences and panes beside it join up
        entries.removeIf(e -> !level.isLoaded(e.pos()) || level.getBlockState(e.pos()) != e.placed());
        for (Entry e : entries) {
            if (!creative && !takeAll(player.getInventory(), e.returned())) continue;
            level.setBlock(e.pos(), e.previous(), Block.UPDATE_ALL);
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, e.pos(), Block.getId(e.placed()));
            if (!creative) player.getInventory().placeItemBackInInventory(new ItemStack(e.used(), e.count()), false, Prediction.SERVER_ONLY);
            undone++;
        }
        return undone;
    }

    /** Takes one of {@code item} from the player's main inventory or offhand (not armor). */
    private static boolean takeFromInventory(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i >= Inventory.INVENTORY_SIZE && i != Inventory.SLOT_OFFHAND) continue;
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item) && !stack.isEmpty() && stack.getComponentsPatch().isEmpty()) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Takes every one of these stacks from the player's main inventory or offhand, or none of them if any is missing. */
    private static boolean takeAll(Inventory inventory, List<ItemStack> wanted) {
        if (wanted.isEmpty()) return true;
        // Kinds and how many of each, then a look before anything is taken
        List<ItemStack> kinds = new ArrayList<>();
        for (ItemStack w : wanted) {
            if (w.isEmpty()) continue;
            ItemStack same = null;
            for (ItemStack k : kinds) if (ItemStack.isSameItemSameComponents(k, w)) same = k;
            if (same != null) same.grow(w.getCount());
            else kinds.add(w.copy());
        }
        for (ItemStack kind : kinds) {
            int have = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (i >= Inventory.INVENTORY_SIZE && i != Inventory.SLOT_OFFHAND) continue;
                ItemStack stack = inventory.getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, kind)) have += stack.getCount();
            }
            if (have < kind.getCount()) return false;
        }
        for (ItemStack kind : kinds) {
            int left = kind.getCount();
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                if (i >= Inventory.INVENTORY_SIZE && i != Inventory.SLOT_OFFHAND) continue;
                ItemStack stack = inventory.getItem(i);
                if (!ItemStack.isSameItemSameComponents(stack, kind)) continue;
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        inventory.setChanged();
        return true;
    }

    private static boolean takeFromNetwork(StorageNetwork network, Item item) {
        try (Transaction tx = Transaction.openOuter()) {
            if (network.extractItem(ItemVariant.of(item), 1, tx) == 1) {
                tx.commit();
                return true;
            }
        }
        return false;
    }

    private BuilderService() {}
}
