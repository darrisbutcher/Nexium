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
import net.minecraft.world.item.Items;
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
 * costs one point of durability. The last {@value #UNDO_STEPS} builds of each player are remembered (until they log out),
 * so the tool menu's Undo can take them back one at a time, returning the blocks.
 */
public final class BuilderService {
    public static final int UNDO_STEPS = 2;

    /** One block built: where, what went there, what was there, the item it used and (Exchange) the item it gave back. */
    record Entry(BlockPos pos, BlockState placed, BlockState previous, Item used, @Nullable Item returned) {}

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
                boolean took = takeFromInventory(player.getInventory(), plan.item());
                if (!took && link != null) {
                    if (!networkLooked) {
                        network = NexusLink.network(player, link);
                        networkLooked = true;
                    }
                    took = network != null && takeFromNetwork(network, plan.item());
                }
                if (!took) {
                    player.sendOverlayMessage(Component.translatable("message.storagenexus.builder.out_of", new ItemStack(plan.item()).getHoverName()));
                    break;
                }
            }
            Item returned = null;
            if (plan.exchange()) {
                Item old = previous.getBlock().asItem();
                if (old != Items.AIR) {
                    returned = old;
                    if (!creative) player.getInventory().placeItemBackInInventory(new ItemStack(old), false, Prediction.SERVER_ONLY);
                }
                level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(previous));
            }
            level.setBlock(pos, plan.state(), Block.UPDATE_ALL);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
            done.add(new Entry(pos.immutable(), plan.state(), previous, plan.item(), returned));
            if (!creative && builder.isDamageableItem()) {
                builder.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (builder.isEmpty()) break;
            }
        }
        if (!done.isEmpty()) {
            SoundType sound = plan.state().getSoundType();
            BlockPos at = done.getFirst().pos();
            level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            Deque<Batch> history = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
            history.addLast(new Batch(level.dimension(), List.copyOf(done)));
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
     * returned (Exchange takes back the block it gave you, so nothing is doubled; a spot whose block you no longer have is
     * left as it is). Returns how many blocks were undone, or -1 if there was nothing to undo.
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
        for (Entry e : entries) {
            if (!level.isLoaded(e.pos()) || level.getBlockState(e.pos()) != e.placed()) continue;
            if (!creative && e.returned() != null && !takeFromInventory(player.getInventory(), e.returned())) continue;
            level.setBlock(e.pos(), e.previous(), Block.UPDATE_ALL);
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, e.pos(), Block.getId(e.placed()));
            if (!creative) player.getInventory().placeItemBackInInventory(new ItemStack(e.used()), false, Prediction.SERVER_ONLY);
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
