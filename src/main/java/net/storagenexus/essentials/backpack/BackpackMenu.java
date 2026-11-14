package net.storagenexus.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.registry.ModMenus;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.util.BaseMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * A Backpack's screen: its {@value BackpackModules#SLOTS} module slots (each with an on/off switch), its
 * {@value BackpackContents#SIZE} slots, and the player's inventory. The slots write straight back to the Backpack's
 * data (on the server) after every change. The Backpack is either the one worn in the backpack slot
 * ({@link Backpacks#EQUIPPED}) or the one in the inventory slot it was opened from, which is locked in place while the
 * screen is open.
 *
 * <p>While the Torch Stack module is in and on, torches stack to {@value BackpackContents#TORCH_STACK_MAX} in the
 * Backpack's slots. Switching it off or taking it out puts the torches over a normal stack back, in the Backpack
 * where they fit and otherwise in the inventory.
 */
public class BackpackMenu extends BaseMenu {
    /** Button id of the first module's on/off switch; module slot {@code n} is {@code BTN_TOGGLE_MODULE + n}. */
    public static final int BTN_TOGGLE_MODULE = 0;

    public static final int MODULE_X = 182, MODULE_Y = 18, MODULE_ROW = 20;
    public static final int GRID_X = 8, GRID_Y = 18;
    /** The player's inventory sits below the Backpack's rows. */
    public static final int INVENTORY_Y = 108;
    public static final int WIDTH = 256, HEIGHT = INVENTORY_Y + 82;

    private final Player player;
    /** {@link Backpacks#EQUIPPED}, or the inventory slot the Backpack was opened from. */
    private final int source;
    /** Server only: the Backpack stack the menu was opened for, so it closes if that stack is replaced. */
    private final ItemStack opened;
    private boolean loading;

    private final SimpleContainer modules = new SimpleContainer(BackpackModules.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) onModulesChanged();
        }
    };
    private final SimpleContainer contents = new SimpleContainer(BackpackContents.SIZE) {
        @Override
        public int getMaxStackSize() {
            // The slots limit each stack (torches only go above 99 with the module); this keeps the container from clamping them
            return BackpackContents.TORCH_STACK_MAX;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // The container clamps every stack it is given to this, not to the item's own limit
            return BackpackContents.TORCH_STACK_MAX;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) saveContents();
        }
    };

    /** Both sides: {@code source} is {@link Backpacks#EQUIPPED} or the inventory slot the Backpack was opened from. */
    public BackpackMenu(int id, Inventory inventory, Integer source) {
        super(ModMenus.BACKPACK, id);
        this.player = inventory.player;
        this.source = source;
        this.opened = player.level().isClientSide() ? ItemStack.EMPTY : backpack();
        if (!player.level().isClientSide()) {
            loading = true;
            List<ItemStack> installed = BackpackModules.slots(opened);
            for (int i = 0; i < BackpackModules.SLOTS; i++) modules.setItem(i, installed.get(i));
            List<ItemStack> stored = BackpackContents.read(opened);
            for (int i = 0; i < BackpackContents.SIZE; i++) contents.setItem(i, stored.get(i));
            loading = false;
        }
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            addCustomSlot(new Slot(modules, i, MODULE_X, MODULE_Y + i * MODULE_ROW) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BackpackModuleItem module && !hasOtherOfType(module, getContainerSlot());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            // Three rows like the inventory, then the last row (the "hotbar") a little apart
            int row = i / 9;
            addCustomSlot(new Slot(contents, i, GRID_X + (i % 9) * 18, GRID_Y + row * 18 + (row == 3 ? 4 : 0)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // No Backpack inside a Backpack
                    return !(stack.getItem() instanceof BackpackItem);
                }

                @Override
                public int getMaxStackSize() {
                    return BackpackContents.TORCH_STACK_MAX;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return BackpackContents.maxStack(stack, torchStack());
                }
            });
        }
        addPlayerSlots(inventory, GRID_X, INVENTORY_Y);
        if (!player.level().isClientSide()) returnOverflow();
    }

    /** The Backpack at this source (or an empty stack): the worn one, or whatever sits in that inventory slot. */
    public static ItemStack stackAt(Player player, int source) {
        return source == Backpacks.EQUIPPED ? ExtraSlots.of(player).backpack() : player.getInventory().getItem(source);
    }

    /** The Backpack this menu is for. */
    public ItemStack backpack() {
        return stackAt(player, source);
    }

    /** {@link Backpacks#EQUIPPED}, or the inventory slot the Backpack was opened from. */
    public int source() {
        return source;
    }

    /** Whether torches stack to 256 here: the Torch Stack module is in one of the module slots and on. Works on both sides, from the module slots. */
    public boolean torchStack() {
        return moduleActive(BackpackModuleType.TORCH_STACK);
    }

    private boolean moduleActive(BackpackModuleType type) {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            ItemStack module = modules.getItem(i);
            if (module.getItem() instanceof BackpackModuleItem item && item.type() == type
                    && !module.has(net.storagenexus.registry.ModComponents.MODULE_OFF)) return true;
        }
        return false;
    }

    private boolean hasOtherOfType(BackpackModuleItem module, int exceptSlot) {
        for (int i = 0; i < BackpackModules.SLOTS; i++) {
            if (i != exceptSlot && modules.getItem(i).getItem() instanceof BackpackModuleItem other && other.type() == module.type()) return true;
        }
        return false;
    }

    private boolean valid() {
        return !player.level().isClientSide() && backpack() == opened && opened.getItem() instanceof BackpackItem;
    }

    private void saveContents() {
        if (!valid()) return;
        List<ItemStack> stacks = new ArrayList<>(BackpackContents.SIZE);
        for (int i = 0; i < BackpackContents.SIZE; i++) stacks.add(contents.getItem(i));
        BackpackContents.write(opened, stacks);
    }

    private void saveModules() {
        if (!valid()) return;
        List<ItemStack> stacks = new ArrayList<>(BackpackModules.SLOTS);
        for (int i = 0; i < BackpackModules.SLOTS; i++) stacks.add(modules.getItem(i));
        BackpackModules.setSlots(opened, stacks);
    }

    private void onModulesChanged() {
        saveModules();
        returnOverflow();
    }

    /**
     * Without the Torch Stack module, no slot may hold more than a normal stack. What is over goes into the Backpack's
     * other slots where it fits, and the rest into the player's inventory (or onto the ground).
     */
    private void returnOverflow() {
        if (player.level().isClientSide() || torchStack()) return;
        List<ItemStack> overflow = new ArrayList<>();
        loading = true;
        for (int i = 0; i < BackpackContents.SIZE; i++) {
            ItemStack stack = contents.getItem(i);
            if (stack.getCount() > stack.getMaxStackSize()) overflow.add(stack.split(stack.getCount() - stack.getMaxStackSize()));
        }
        for (ItemStack extra : overflow) {
            // Into the Backpack's own stacks and empty slots first
            for (int i = 0; i < BackpackContents.SIZE && !extra.isEmpty(); i++) {
                ItemStack there = contents.getItem(i);
                if (there.isEmpty()) {
                    contents.setItem(i, extra.split(Math.min(extra.getCount(), extra.getMaxStackSize())));
                } else if (ItemStack.isSameItemSameComponents(there, extra) && there.getCount() < there.getMaxStackSize()) {
                    int moved = Math.min(extra.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(moved);
                    extra.shrink(moved);
                }
            }
            if (!extra.isEmpty()) player.getInventory().placeItemBackInInventory(extra, Prediction.PREDICTED);
        }
        loading = false;
        if (!overflow.isEmpty()) {
            saveContents();
            broadcastChanges();
        }
    }

    /** The slot index (in this menu) of the inventory slot the Backpack was opened from, or -1 if it is the worn one (or the offhand). */
    private int lockedMenuSlot() {
        if (source < 0 || source >= 36) return -1;
        return playerSlotStart + (source >= 9 ? source - 9 : 27 + source);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // The Backpack stays where it is: no clicking it, and no number-key (or offhand) swaps into its slot
        if (slotIndex >= 0 && slotIndex == lockedMenuSlot()) return;
        if (input == ContainerInput.SWAP && (button == source || (button == 40 && source == Inventory.SLOT_OFFHAND))) return;
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == lockedMenuSlot()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        int slot = id - BTN_TOGGLE_MODULE;
        if (slot < 0 || slot >= BackpackModules.SLOTS || !valid()) return true;
        ItemStack module = modules.getItem(slot);
        if (!(module.getItem() instanceof BackpackModuleItem)) return true;
        ItemStack changed = module.copy();
        boolean nowOn = changed.has(net.storagenexus.registry.ModComponents.MODULE_OFF);
        if (nowOn) changed.remove(net.storagenexus.registry.ModComponents.MODULE_OFF);
        else changed.set(net.storagenexus.registry.ModComponents.MODULE_OFF, net.minecraft.util.Unit.INSTANCE);
        modules.setItem(slot, changed);
        serverPlayer.sendOverlayMessage(Component.translatable(nowOn ? "message.nexusessentials.backpack_module_on" : "message.nexusessentials.backpack_module_off",
                changed.getHoverName()).withStyle(nowOn ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return true;
    }

    @Override
    public void removed(Player player) {
        if (valid()) {
            saveContents();
            saveModules();
        }
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        // On the client the Backpack may not have arrived yet; the server decides
        return player.level().isClientSide() || valid();
    }
}
