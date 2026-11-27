package net.nexium.autocraft;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pattern Encoder's contents: nine input slots (a crafting grid, or what goes into a machine), three output slots (set by the
 * recipe in crafting mode), a slot for a blank pattern and the encoded pattern it makes. The input and output slots are only
 * templates: they never hold real items, so putting something in them takes nothing from you.
 */
public class PatternEncoderBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    /** Slot numbers in {@link #slots()}: nine inputs, three outputs, the blank pattern, the encoded one. */
    public static final int INPUTS = 9, OUTPUTS = 3, BLANK = 12, ENCODED = 13, SIZE = 14;

    private final SimpleContainer slots = new SimpleContainer(SIZE) {
        @Override public void setChanged() {
            super.setChanged();
            PatternEncoderBlockEntity.this.setChanged();
        }
    };
    private boolean processing;

    public PatternEncoderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PATTERN_ENCODER, pos, state);
    }

    public SimpleContainer slots() { return slots; }

    public boolean processing() { return processing; }

    public void setProcessing(boolean processing) {
        this.processing = processing;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PatternEncoderMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        // Only the two real slots hold items; the template slots hold nothing of the player's
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slots.getItem(BLANK));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slots.getItem(ENCODED));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) list.add(slots.getItem(i));
        output.store("slots", ItemStack.OPTIONAL_CODEC.listOf(), list);
        output.putBoolean("processing", processing);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<ItemStack> list = input.read("slots", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SIZE; i++) slots.setItem(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        processing = input.getBooleanOr("processing", false);
    }
}
