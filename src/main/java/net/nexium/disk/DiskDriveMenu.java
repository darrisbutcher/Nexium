package net.nexium.disk;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.nexium.filter.FilterItem;
import net.nexium.filter.FilterMenu;
import net.nexium.util.Menus;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/** The Disk Drive's screen: its 8 disks, priority, filter, Move to disks and Move up (lower tier disks onto higher tier ones). */
public class DiskDriveMenu extends BaseMenu {
    public static final int BTN_MINUS_1 = 0, BTN_PLUS_1 = 1, BTN_MINUS_10 = 2, BTN_PLUS_10 = 3, BTN_MOVE = 4, BTN_MOVE_UP = 5, BTN_MOVE_OUT = 6;
    /** Opens the editor for the filter in the filter slot. */
    public static final int BTN_EDIT_FILTER = 10;
    public static final int FILTER_X = 88, FILTER_Y = 48, INVENTORY_Y = 139;
    private static final int PRIORITY_OFFSET = 1000;

    private final @Nullable DiskDriveBlockEntity be;
    private final ContainerData data;

    /** Client side. */
    public DiskDriveMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (DiskDriveBlockEntity) null);
    }

    public DiskDriveMenu(int id, Inventory inventory, @Nullable DiskDriveBlockEntity be) {
        super(ModMenus.DISK_DRIVE, id);
        this.be = be;
        Container disks = be != null ? be.disks() : new SimpleContainer(DiskDriveBlockEntity.SLOTS);
        for (int i = 0; i < DiskDriveBlockEntity.SLOTS; i++) {
            addCustomSlot(new Slot(disks, i, slotX(i), slotY(i)) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof StorageDiskItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        Container filter = be != null ? be.filterContainer() : new SimpleContainer(1);
        addCustomSlot(new Slot(filter, 0, FILTER_X, FILTER_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FilterItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addPlayerSlots(inventory, 8, INVENTORY_Y);
        this.data = be != null ? new ContainerData() {
            @Override public int get(int i) { return be.priority() + PRIORITY_OFFSET; }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(data);
    }

    /** Disk slots: 4 across, 2 rows, with room for a fill bar under each. */
    public static int slotX(int i) { return 8 + (i % 4) * 18; }

    public static int slotY(int i) { return 19 + (i / 4) * 24; }

    public int priority() {
        return data.get(0) - PRIORITY_OFFSET;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return true;
        switch (id) {
            case BTN_MINUS_1 -> be.setPriority(be.priority() - 1);
            case BTN_PLUS_1 -> be.setPriority(be.priority() + 1);
            case BTN_MINUS_10 -> be.setPriority(be.priority() - 10);
            case BTN_PLUS_10 -> be.setPriority(be.priority() + 10);
            case BTN_EDIT_FILTER -> {
                if (player instanceof ServerPlayer sp) {
                    FilterMenu.openFor(sp, () -> be.filterContainer().getItem(0), this::stillValid, () -> be.filterContainer().setChanged(),
                            () -> Menus.open(sp, be.getBlockState().getBlock().getName(), be.getBlockPos(), (id2, inv, p) -> new DiskDriveMenu(id2, inv, be)));
                }
            }
            case BTN_MOVE -> {
                DiskDriveBlockEntity.Moved moved = be.moveToDisks();
                if (player instanceof ServerPlayer sp) {
                    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
                    sp.sendOverlayMessage(moved.items() == 0 && moved.fluid() == 0
                            ? Component.translatable("message.nexium.disk.nothing_moved")
                            : Component.translatable("message.nexium.disk.moved", f.format(moved.items()),
                            f.format(moved.fluid() / FluidConstants.BUCKET)));
                }
            }
            case BTN_MOVE_OUT -> {
                DiskDriveBlockEntity.Moved moved = be.moveToDrawers();
                if (player instanceof ServerPlayer sp) {
                    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
                    sp.sendOverlayMessage(moved.items() == 0 && moved.fluid() == 0
                            ? Component.translatable("message.nexium.disk.nothing_moved_out")
                            : Component.translatable("message.nexium.disk.moved_out", f.format(moved.items()),
                            f.format(moved.fluid() / FluidConstants.BUCKET)));
                }
            }
            case BTN_MOVE_UP -> {
                DiskDriveBlockEntity.Moved moved = be.moveUp();
                if (player instanceof ServerPlayer sp) {
                    NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
                    sp.sendOverlayMessage(moved.items() == 0 && moved.fluid() == 0
                            ? Component.translatable("message.nexium.disk.nothing_moved_up")
                            : Component.translatable("message.nexium.disk.moved_up", f.format(moved.items()),
                            f.format(moved.fluid() / FluidConstants.BUCKET)));
                }
            }
            default -> {}
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 64);
    }
}
