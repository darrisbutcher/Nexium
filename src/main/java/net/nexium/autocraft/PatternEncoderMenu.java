package net.nexium.autocraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.nexium.autocraft.PatternEncoderBlockEntity.*;

/**
 * The Pattern Encoder's screen. The input and output slots are templates: clicking one with an item sets it (clicking again with the
 * same item in processing mode adds one), an empty hand clears it (right-click takes one off), and nothing is taken from you. The
 * Mode button switches between a crafting grid (the result comes from the recipe) and a machine's inputs and outputs, and Encode
 * writes the pattern onto the blank pattern.
 */
public class PatternEncoderMenu extends BaseMenu {
    public static final int BTN_ENCODE = 0, BTN_MODE = 1, BTN_CLEAR = 2;
    public static final int INVENTORY_TOP = 122, GRID_X = 26, GRID_Y = 20, OUT_X = 116, OUT_Y = 20, BLANK_X = 116, BLANK_Y = 78, ENCODED_X = 142;

    private final @Nullable PatternEncoderBlockEntity encoder;
    private final Container slots;
    private final ContainerData data;
    private final Player player;

    /** Client side. */
    public PatternEncoderMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (PatternEncoderBlockEntity) null);
    }

    /** Server side. */
    public PatternEncoderMenu(int id, Inventory inventory, @Nullable PatternEncoderBlockEntity encoder) {
        super(ModMenus.PATTERN_ENCODER, id);
        this.encoder = encoder;
        this.player = inventory.player;
        this.slots = encoder != null ? encoder.slots() : new SimpleContainer(SIZE);
        for (int i = 0; i < INPUTS; i++) addCustomSlot(new Template(i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18));
        for (int i = 0; i < OUTPUTS; i++) addCustomSlot(new Template(INPUTS + i, OUT_X, OUT_Y + i * 18));
        addCustomSlot(new Slot(slots, BLANK, BLANK_X, BLANK_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof PatternItem; }
            @Override public int getMaxStackSize() { return 64; }
        });
        addCustomSlot(new Slot(slots, ENCODED, ENCODED_X, BLANK_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
        data = encoder == null ? new SimpleContainerData(1) : new ContainerData() {
            @Override public int get(int index) { return encoder.processing() ? 1 : 0; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 1; }
        };
        addDataSlots(data);
    }

    public boolean processing() { return data.get(0) == 1; }

    /** A slot that holds a template, never real items. */
    private final class Template extends Slot {
        final int index;

        Template(int index, int x, int y) {
            super(slots, index, x, y);
            this.index = index;
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public int getMaxStackSize() { return 64; }

        @Override
        public boolean isActive() {
            // The second and third outputs only exist for a machine; a recipe has one result
            return index <= INPUTS || processing();
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < INPUTS + OUTPUTS) {
            // A recipe's result can't be set by hand; in processing mode every template can
            if (input == ContainerInput.PICKUP && (slotIndex < INPUTS || processing())) template(slotIndex, button);
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void template(int index, int button) {
        ItemStack carried = getCarried(), there = slots.getItem(index);
        if (carried.isEmpty()) {
            if (button == 1 && there.getCount() > 1) there.shrink(1);
            else slots.setItem(index, ItemStack.EMPTY);
        } else if (processing() && ItemStack.isSameItemSameComponents(there, carried)) {
            there.setCount(Math.min(64, there.getCount() + 1));
        } else {
            slots.setItem(index, carried.copyWithCount(1));
        }
        slots.setChanged();
        updateResult();
    }

    /** In crafting mode the first output is the recipe's result. */
    private void updateResult() {
        if (processing() || !(player.level() instanceof ServerLevel level)) return;
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < INPUTS; i++) grid.add(slots.getItem(i).copyWithCount(1));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level);
        slots.setItem(INPUTS, recipe.map(r -> r.value().assemble(CraftingInput.of(3, 3, grid))).orElse(ItemStack.EMPTY));
        slots.setItem(INPUTS + 1, ItemStack.EMPTY);
        slots.setItem(INPUTS + 2, ItemStack.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || encoder == null) return true;
        switch (id) {
            case BTN_MODE -> {
                encoder.setProcessing(!encoder.processing());
                for (int i = INPUTS; i < INPUTS + OUTPUTS; i++) slots.setItem(i, ItemStack.EMPTY);
                for (int i = 0; i < INPUTS; i++) if (!encoder.processing() && !slots.getItem(i).isEmpty()) slots.getItem(i).setCount(1);
                slots.setChanged();
                updateResult();
            }
            case BTN_CLEAR -> {
                for (int i = 0; i < INPUTS + OUTPUTS; i++) slots.setItem(i, ItemStack.EMPTY);
                slots.setChanged();
            }
            case BTN_ENCODE -> serverPlayer.sendOverlayMessage(Component.translatable(encode() ? "message.nexium.pattern.encoded" : "message.nexium.pattern.cannot"));
            default -> {}
        }
        return true;
    }

    /** Writes the pattern onto the blank pattern in the slot; the encoded pattern goes in the slot beside it. */
    public boolean encode() {
        ItemStack blank = slots.getItem(BLANK);
        if (!(blank.getItem() instanceof PatternItem) || !slots.getItem(ENCODED).isEmpty()) return false;
        List<ItemStack> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        if (processing()) {
            for (int i = 0; i < INPUTS; i++) if (!slots.getItem(i).isEmpty()) inputs.add(slots.getItem(i).copy());
            for (int i = INPUTS; i < INPUTS + OUTPUTS; i++) if (!slots.getItem(i).isEmpty()) outputs.add(slots.getItem(i).copy());
        } else {
            for (int i = 0; i < INPUTS; i++) inputs.add(slots.getItem(i).copyWithCount(1));
            if (!slots.getItem(INPUTS).isEmpty()) outputs.add(slots.getItem(INPUTS).copy());
        }
        Pattern pattern = new Pattern(processing(), inputs, outputs);
        if (!pattern.valid()) return false;
        ItemStack made = new ItemStack(ModItems.PATTERN);
        made.set(ModComponents.PATTERN, pattern);
        if (PatternItem.isEncoded(blank)) {
            // Re-encoding a pattern replaces what it said
            slots.setItem(BLANK, ItemStack.EMPTY);
        } else {
            blank.shrink(1);
        }
        slots.setItem(ENCODED, made);
        slots.setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < INPUTS + OUTPUTS) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean stillValid(Player player) {
        return encoder == null || (!encoder.isRemoved() && player.distanceToSqr(encoder.getBlockPos().getX() + 0.5,
                encoder.getBlockPos().getY() + 0.5, encoder.getBlockPos().getZ() + 0.5) <= 64);
    }
}
