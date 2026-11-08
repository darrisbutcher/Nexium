package net.storagenexus.builder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

/**
 * The Construction Builder's tool menu (G while holding one): its mode, plane lock and batch size, Undo for the last
 * {@value BuilderService#UNDO_STEPS} builds, and its one module slot, which takes a Nexus Link so blocks can come from a
 * storage network. The slot writes straight back to the builder; the builder is locked in its hotbar slot meanwhile.
 */
public class BuilderMenu extends BaseMenu {
    public static final int BTN_MODE = 0, BTN_PLANE = 1, BTN_SIZE = 2, BTN_UNDO = 3, BTN_MODULE = 4;
    public static final int MODULE_SLOT_X = 8, MODULE_SLOT_Y = 88;
    /** Where the "Blocks from" note goes, under the module row. */
    public static final int NOTE_Y = MODULE_SLOT_Y + 21;
    public static final int INVENTORY_Y = NOTE_Y + 30;

    private final Player player;
    private final int toolSlot;
    private final SimpleContainer module = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            saveModule();
        }
    };
    private boolean loading;
    /** How many builds Undo can still take back (kept on the server, synced for the button). */
    private final DataSlot undoLeft = DataSlot.standalone();

    /** Both sides: {@code toolSlot} is the selected hotbar slot when the menu was opened. */
    public BuilderMenu(int id, Inventory inventory, Integer toolSlot) {
        super(ModMenus.BUILDER, id);
        this.player = inventory.player;
        this.toolSlot = toolSlot;
        if (!player.level().isClientSide()) loadModule();
        addCustomSlot(new Slot(module, 0, MODULE_SLOT_X, MODULE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ModuleItem item && item.type() == ModuleType.NEXUS_LINK;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addDataSlot(undoLeft);
        if (player instanceof ServerPlayer serverPlayer) undoLeft.set(BuilderService.undoLeft(serverPlayer));
        addPlayerSlots(inventory, 8, INVENTORY_Y);
    }

    /** The builder: whatever is in its hotbar slot. */
    public ItemStack tool() {
        return player.getInventory().getItem(toolSlot);
    }

    public int toolSlot() {
        return toolSlot;
    }

    public int undoLeft() {
        return undoLeft.get();
    }

    private void loadModule() {
        loading = true;
        module.setItem(0, BuilderSettings.module(tool()));
        loading = false;
    }

    private void saveModule() {
        if (loading || player.level().isClientSide() || !(tool().getItem() instanceof BuilderItem)) return;
        BuilderSettings.setModule(tool(), module.getItem(0).copy());
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer serverPlayer) undoLeft.set(BuilderService.undoLeft(serverPlayer));
        super.broadcastChanges();
    }

    private int lockedMenuSlot() {
        return playerSlotStart + 27 + toolSlot;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
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
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        ItemStack builder = tool();
        if (!(builder.getItem() instanceof BuilderItem)) return true;
        switch (id) {
            case BTN_MODE -> serverPlayer.sendOverlayMessage(Component.translatable("message.storagenexus.builder.mode",
                    Component.translatable("screen.storagenexus.builder.mode." + BuilderSettings.cycleMode(builder).name().toLowerCase())));
            case BTN_PLANE -> serverPlayer.sendOverlayMessage(Component.translatable("message.storagenexus.builder.plane",
                    Component.translatable("screen.storagenexus.builder.plane." + BuilderSettings.cyclePlane(builder).name().toLowerCase())));
            case BTN_SIZE -> serverPlayer.sendOverlayMessage(Component.translatable("message.storagenexus.builder.size",
                    BuilderSettings.cycleSize(builder)));
            case BTN_UNDO -> {
                int undone = BuilderService.undo(serverPlayer);
                serverPlayer.sendOverlayMessage(undone < 0 ? Component.translatable("message.storagenexus.builder.nothing_to_undo")
                        : Component.translatable("message.storagenexus.builder.undone", undone));
            }
            case BTN_MODULE -> {
                ItemStack installed = BuilderSettings.module(builder);
                if (installed.isEmpty()) return true;
                boolean nowOn = installed.has(ModComponents.MODULE_OFF);
                if (nowOn) installed.remove(ModComponents.MODULE_OFF);
                else installed.set(ModComponents.MODULE_OFF, Unit.INSTANCE);
                BuilderSettings.setModule(builder, installed);
                loadModule();
                serverPlayer.sendOverlayMessage(Component.translatable(nowOn ? "message.storagenexus.tool_menu.module_on"
                        : "message.storagenexus.tool_menu.module_off", installed.getHoverName()));
            }
            default -> {}
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().getSelectedSlot() == toolSlot && tool().getItem() instanceof BuilderItem;
    }
}
