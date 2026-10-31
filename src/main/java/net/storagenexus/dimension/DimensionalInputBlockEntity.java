package net.storagenexus.dimension;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.upgrade.UpgradeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Accepts items (hoppers, pipes, players) into a 9-slot buffer and sends them to its linked
 * Outputs, in any dimension. If no Output can take an item it stays in the buffer, so the
 * hopper line backs up instead of anything being lost.
 */
public class DimensionalInputBlockEntity extends LinkedBlockEntity {
    public static final int BUFFER_SLOTS = 9;

    private final SimpleContainer buffer = new SimpleContainer(BUFFER_SLOTS) {
        @Override public void setChanged() { super.setChanged(); DimensionalInputBlockEntity.this.setChanged(); }
    };
    private final SimpleContainer upgradeSlot = new SimpleContainer(1) {
        @Override public void setChanged() { super.setChanged(); DimensionalInputBlockEntity.this.setChanged(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem u && u.kind() == UpgradeItem.Kind.TRANSFER_SPEED;
        }
        @Override public int getMaxStackSize() { return 2; }
    };
    private final ContainerStorage storage = ContainerStorage.of(buffer, null);

    private int cooldown;
    private int roundRobin;
    private int onlineOutputs;
    private int totalOutputs;

    public DimensionalInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_INPUT, pos, state);
    }

    public Storage<ItemVariant> getItemStorage() { return storage; }
    public SimpleContainer buffer() { return buffer; }
    public SimpleContainer upgradeSlot() { return upgradeSlot; }
    public int onlineOutputs() { return onlineOutputs; }
    public int totalOutputs() { return totalOutputs; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalInputBlockEntity be) {
        if (level.getGameTime() % 100 == 0) {
            be.validateLink();
            if (be.refreshName()) be.sync();
        }
        if (--be.cooldown > 0) return;
        int upgrades = be.upgradeSlot.getItem(0).getCount();
        be.cooldown = upgrades >= 2 ? 4 : 8;
        int batch = upgrades == 0 ? 1 : upgrades == 1 ? 16 : 64;
        be.transfer((ServerLevel) level, batch);
    }

    /** If another Input has taken over this link, drop it. */
    private void validateLink() {
        LinkRegistry reg = registry();
        if (reg == null || linkId == null) return;
        LinkRegistry.Link link = reg.get(linkId);
        if (link == null || !globalPos().equals(link.input())) setLink(null);
    }

    private List<DimensionalOutputBlockEntity> resolveOutputs(MinecraftServer server) {
        List<DimensionalOutputBlockEntity> out = new ArrayList<>();
        LinkRegistry reg = LinkRegistry.get(server);
        LinkRegistry.Link link = reg.get(linkId);
        totalOutputs = link == null ? 0 : link.outputs().size();
        if (link == null) return out;
        for (GlobalPos gp : link.outputs()) {
            ServerLevel l = server.getLevel(gp.dimension());
            if (l == null || !l.isLoaded(gp.pos())) continue;
            if (l.getBlockEntity(gp.pos()) instanceof DimensionalOutputBlockEntity o && linkId.equals(o.linkId())) out.add(o);
        }
        return out;
    }

    private void transfer(ServerLevel level, int batch) {
        if (linkId == null) {
            onlineOutputs = 0;
            totalOutputs = 0;
            return;
        }
        List<DimensionalOutputBlockEntity> outputs = resolveOutputs(level.getServer());
        onlineOutputs = outputs.size();
        if (outputs.isEmpty()) return;

        int remaining = batch;
        for (int slot = 0; slot < BUFFER_SLOTS && remaining > 0; slot++) {
            ItemStack stack = buffer.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemVariant variant = ItemVariant.of(stack);
            int want = Math.min(remaining, stack.getCount());
            for (int i = 0; i < outputs.size() && want > 0; i++) {
                DimensionalOutputBlockEntity out = outputs.get((roundRobin + i) % outputs.size());
                if (!out.accepts(variant)) continue;
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = out.receive(variant, want, tx);
                    if (moved > 0) {
                        tx.commit();
                        stack.shrink((int) moved);
                        want -= (int) moved;
                        remaining -= (int) moved;
                    }
                }
            }
            if (stack.isEmpty()) buffer.setItem(slot, ItemStack.EMPTY);
        }
        roundRobin = (roundRobin + 1) % Math.max(1, outputs.size());
        if (remaining != batch) buffer.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < BUFFER_SLOTS; i++) items.add(buffer.getItem(i));
        output.store("buffer", ItemStack.OPTIONAL_CODEC.listOf(), items);
        output.store("upgrade", ItemStack.OPTIONAL_CODEC, upgradeSlot.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("buffer", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < BUFFER_SLOTS; i++) buffer.setItem(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        });
        upgradeSlot.setItem(0, input.read("upgrade", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        Containers.dropContents(level, pos, buffer);
        Containers.dropContents(level, pos, upgradeSlot);
        LinkRegistry reg = registry();
        if (reg != null) reg.removeInput(globalPos());
    }
}
