package net.storagenexus.aiot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.aiot.module.RetiredModuleItem;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

/**
 * The tool menu for the tool in the main hand (an AIOT or a Nexium tool): the Mining and Haste II switches, and on an
 * AIOT its module slots, each with an on/off switch. Modules are put in and taken out like items in any container;
 * the slots write straight back to the AIOT. The tool itself is locked in its hotbar slot while the menu is open.
 */
public class ToolMenuMenu extends BaseMenu {
    public static final int BTN_SWAP_SILK_TOUCH_FORTUNE = 0, BTN_TOGGLE_HASTE = 1, BTN_TOGGLE_MODULE = 2;
    /** Steps the AIOT's vein size. */
    public static final int BTN_VEIN_SIZE = BTN_TOGGLE_MODULE + AiotModules.SLOTS;
    /** Steps the 3x3 Mining module's tunnel length. */
    public static final int BTN_TUNNEL = BTN_VEIN_SIZE + 1;
    public static final int MODULE_SLOT_X = 8, MODULE_SLOT_Y = 88, MODULE_ROW = 18;
    /** The player inventory sits below the module rows. */
    public static final int INVENTORY_Y = MODULE_SLOT_Y + AiotModules.SLOTS * MODULE_ROW + 13;

    private final Player player;
    /** The hotbar slot holding the tool. */
    private final int toolSlot;
    /** Every change in the slots is written straight back to the AIOT (on the server). */
    private final SimpleContainer modules = new SimpleContainer(AiotModules.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            saveModules();
        }
    };
    private boolean loading;
    /** The server's vein size limit, synced so the screen caps the size the way the server does (the config is server-side). */
    private final DataSlot veinMax = DataSlot.standalone();
    /** The server's tunnel length limit, synced the same way. */
    private final DataSlot tunnelMax = DataSlot.standalone();

    /** Both sides: {@code toolSlot} is the selected hotbar slot when the menu was opened. */
    public ToolMenuMenu(int id, Inventory inventory, Integer toolSlot) {
        super(ModMenus.TOOL_MENU, id);
        this.player = inventory.player;
        this.toolSlot = toolSlot;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // An AIOT saved with the retired Vein Mining Module hands it back as its materials
            int retired = AiotModules.takeRetired(tool()).size();
            if (retired > 0) {
                RetiredModuleItem.refund(serverPlayer, retired);
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.storagenexus.retired_module_returned"));
            }
        }
        if (!player.level().isClientSide()) {
            loading = true;
            NonNullList<ItemStack> installed = AiotModules.slots(tool());
            for (int i = 0; i < AiotModules.SLOTS; i++) modules.setItem(i, installed.get(i));
            loading = false;
        }
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            addCustomSlot(new Slot(modules, i, MODULE_SLOT_X, MODULE_SLOT_Y + i * MODULE_ROW) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isActive() && stack.getItem() instanceof ModuleItem module && !hasOtherOfType(module, getContainerSlot());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return tool().getItem() instanceof AiotItem;
                }
            });
        }
        addDataSlot(veinMax);
        addDataSlot(tunnelMax);
        if (!player.level().isClientSide()) {
            veinMax.set(NexusConfig.get().aiotVeinMaxBlocks);
            tunnelMax.set(NexusConfig.get().aiotTunnelMaxLength);
        }
        addPlayerSlots(inventory, 8, INVENTORY_Y);
    }

    /** The tool the menu is for: whatever is in its hotbar slot. */
    public ItemStack tool() {
        return player.getInventory().getItem(toolSlot);
    }

    /** The most blocks a vein mining swing may mine on this server. */
    public int veinMax() {
        return veinMax.get();
    }

    /** The longest tunnel a 3x3 Mining swing may dig on this server. */
    public int tunnelMax() {
        return tunnelMax.get();
    }

    public int toolSlot() {
        return toolSlot;
    }

    private boolean hasOtherOfType(ModuleItem module, int exceptSlot) {
        for (int i = 0; i < AiotModules.SLOTS; i++) {
            if (i != exceptSlot && modules.getItem(i).getItem() instanceof ModuleItem other && other.type() == module.type()) return true;
        }
        return false;
    }

    private void saveModules() {
        if (loading || player.level().isClientSide() || !(tool().getItem() instanceof AiotItem)) return;
        NonNullList<ItemStack> slots = NonNullList.withSize(AiotModules.SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < AiotModules.SLOTS; i++) slots.set(i, modules.getItem(i).copy());
        AiotModules.setSlots(tool(), slots);
    }

    /** The slot index (in this menu) of the inventory slot holding the tool. */
    private int lockedMenuSlot() {
        return playerSlotStart + 27 + toolSlot;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // The tool stays where it is: no clicking it, and no number-key swaps into its hotbar slot
        if (slotIndex == lockedMenuSlot()) return;
        if (input == ContainerInput.SWAP && button == toolSlot) return;
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == lockedMenuSlot()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return true;
        if (id == BTN_SWAP_SILK_TOUCH_FORTUNE) {
            ToolMenu.swapSilkTouchFortune(serverPlayer, tool());
        } else if (id == BTN_TOGGLE_HASTE) {
            ToolMenu.toggleHaste(serverPlayer, tool());
        } else if (id == BTN_VEIN_SIZE) {
            ToolMenu.cycleVeinSize(serverPlayer, tool());
        } else if (id == BTN_TUNNEL) {
            if (ToolMenu.cycleTunnelLength(serverPlayer, tool())) {
                loading = true;
                NonNullList<ItemStack> installed = AiotModules.slots(tool());
                for (int i = 0; i < AiotModules.SLOTS; i++) modules.setItem(i, installed.get(i));
                loading = false;
            }
        } else if (id >= BTN_TOGGLE_MODULE && id < BTN_TOGGLE_MODULE + AiotModules.SLOTS) {
            int slot = id - BTN_TOGGLE_MODULE;
            if (ToolMenu.toggleModule(serverPlayer, tool(), slot)) {
                // Keep the menu's copy in step with the AIOT
                loading = true;
                modules.setItem(slot, AiotModules.slots(tool()).get(slot));
                loading = false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getSelectedSlot() == toolSlot && ToolMenu.appliesTo(tool());
    }
}
