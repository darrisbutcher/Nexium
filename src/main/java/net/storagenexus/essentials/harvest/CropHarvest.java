package net.storagenexus.essentials.harvest;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.storagenexus.aiot.module.VeinKey;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Right-click a fully grown crop to harvest it: it drops what breaking it would (Fortune on the held tool counts), and
 * one of its seeds goes back in the ground, so it starts growing again. With the vein mining key held, the whole
 * connected patch of that crop that is fully grown is harvested and replanted at once. Wheat, carrots, potatoes,
 * beetroot, nether wart and cocoa. Sneaking skips it, and a crop that isn't fully grown is left to vanilla (bone meal).
 */
public final class CropHarvest {
    public static void init() {
        UseBlockCallback.EVENT.register(CropHarvest::onUse);
    }

    private static InteractionResult onUse(Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!NexusConfig.get().rightClickHarvest) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!isRipe(level.getBlockState(pos))) return InteractionResult.PASS;
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            harvest(server, serverPlayer, pos, VeinKey.held(serverPlayer));
        }
        return InteractionResult.SUCCESS;
    }

    /** A crop this harvests, fully grown. */
    public static boolean isRipe(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof TorchflowerCropBlock) return false; // grows into a flower block instead
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        return false;
    }

    /** The same crop, just planted. */
    private static BlockState replanted(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getStateForAge(0);
        if (block instanceof NetherWartBlock) return state.setValue(NetherWartBlock.AGE, 0);
        if (block instanceof CocoaBlock) return state.setValue(CocoaBlock.AGE, 0);
        return state;
    }

    /**
     * Harvests the ripe crop at {@code pos} and, if {@code vein}, every ripe crop of the same kind connected to it (up to
     * {@code harvestVeinMax}). Returns how many were harvested.
     */
    public static int harvest(ServerLevel level, ServerPlayer player, BlockPos pos, boolean vein) {
        BlockState first = level.getBlockState(pos);
        if (!isRipe(first)) return 0;
        List<BlockPos> crops = vein ? connected(level, pos, first.getBlock(), NexusConfig.get().harvestVeinMax) : List.of(pos);
        ItemStack tool = player.getMainHandItem();
        for (BlockPos p : crops) harvestOne(level, player, p, level.getBlockState(p), tool);
        return crops.size();
    }

    private static void harvestOne(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool));
        Item seed = state.getBlock().asItem();
        boolean replant = takeOne(drops, seed) || takeOne(player.getInventory(), seed);
        level.setBlock(pos, replant ? replanted(state) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        for (ItemStack drop : drops) if (!drop.isEmpty()) Block.popResource(level, pos, drop);
    }

    private static boolean takeOne(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (drop.is(seed) && !drop.isEmpty()) {
                drop.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean takeOne(Inventory inventory, Item seed) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(seed)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Ripe crops of the same block connected to {@code start} (touching on any side or corner), {@code start} first. */
    private static List<BlockPos> connected(Level level, BlockPos start, Block block, int max) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && out.size() < max) {
            BlockPos p = queue.poll();
            out.add(p);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (seen.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.getBlock() == block && isRipe(s)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    private CropHarvest() {}
}
