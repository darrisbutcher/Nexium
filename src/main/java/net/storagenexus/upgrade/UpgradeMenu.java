package net.storagenexus.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;
import net.storagenexus.util.SyncedBlockEntity;

/** Upgrade slots for drawers, fluid drawers and tanks: four general ones plus the lock slot. */
public class UpgradeMenu extends BaseMenu {
    public static final int LOCK_SLOT_X = 8, SLOT_Y = 20, INVENTORY_Y = 73;

    private final Upgradeable target;
    private final BlockEntity blockEntity;
    /** Multiplier, total capacity in four 15-bit parts, slot count, and 1 for fluids. */
    private final ContainerData data;

    /** Client constructor. */
    public UpgradeMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null, null);
    }

    public UpgradeMenu(int id, Inventory inventory, Upgradeable target, BlockEntity be) {
        super(ModMenus.UPGRADES, id);
        this.target = target;
        this.blockEntity = be;
        Container container = target != null ? new UpgradeContainer(target) : new SimpleContainer(Upgradeable.UPGRADE_SLOTS);
        for (int i = 0; i < Upgradeable.UPGRADE_SLOTS; i++) {
            final int slotIndex = i;
            // General slots right-aligned; the lock slot on the far left
            int x = i == Upgradeable.LOCK_SLOT ? LOCK_SLOT_X : 96 + i * 18;
            addCustomSlot(new Slot(container, i, x, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return UpgradeMenu.this.target == null
                            ? stack.getItem() instanceof UpgradeItem && Upgradeable.fitsSlot(stack, slotIndex)
                            : UpgradeMenu.this.target.acceptsUpgrade(stack, slotIndex);
                }

                @Override
                public boolean mayPickup(Player player) {
                    if (UpgradeMenu.this.target == null || UpgradeMenu.this.target.canRemoveUpgrade(slotIndex)) return true;
                    // A full drawer can still swap an upgrade for a stronger capacity upgrade (one at a time)
                    ItemStack carried = getCarried();
                    return carried.getCount() == 1 && Upgradeable.isStrongerCapacity(carried, getItem());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addPlayerSlots(inventory, 8, INVENTORY_Y);
        data = target == null ? new SimpleContainerData(7) : new ContainerData() {
            @Override
            public int get(int i) {
                long total = target.totalCapacity();
                return switch (i) {
                    case 0 -> target.capacityMultiplier();
                    case 1, 2, 3, 4 -> (int) (total >> (15 * (i - 1)) & 0x7FFF);
                    case 5 -> target.capacitySlots();
                    default -> target.holdsFluid() ? 1 : 0;
                };
            }

            @Override public void set(int i, int value) {}
            @Override public int getCount() { return 7; }
        };
        addDataSlots(data);
    }

    public int multiplier() { return Math.max(1, data.get(0)); }

    /** Total capacity: stacks for item drawers, buckets for fluid drawers. */
    public long totalCapacity() {
        long total = 0;
        for (int i = 0; i < 4; i++) total |= (long) data.get(1 + i) << (15 * i);
        return total;
    }

    public int capacitySlots() { return Math.max(1, data.get(5)); }

    public boolean holdsFluid() { return data.get(6) == 1; }

    /**
     * Clicking a filled upgrade slot with a stack of higher capacity upgrades swaps one in,
     * even when the drawer is full; the old upgrade goes to the inventory (or drops if that's full).
     */
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (target != null && input == ContainerInput.PICKUP && slotIndex >= 0 && slotIndex < Upgradeable.LOCK_SLOT) {
            Slot slot = slots.get(slotIndex);
            ItemStack carried = getCarried();
            if (carried.getCount() > 1 && Upgradeable.isHigherCapacity(carried, slot.getItem())) {
                ItemStack old = slot.getItem();
                slot.set(carried.split(1));
                giveBack(player, old);
                return;
            }
        }
        super.clicked(slotIndex, button, input, player);
    }

    /** Shift-click with every slot taken: the upgrade replaces the weakest capacity upgrade, if any is weaker. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = super.quickMoveStack(player, index);
        if (moved.isEmpty() && target != null && index >= playerSlotStart) {
            Slot slot = slots.get(index);
            ItemStack old = target.tryReplaceCapacity(slot.getItem());
            if (!old.isEmpty()) {
                if (slot.getItem().isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
                else slot.setChanged();
                giveBack(player, old);
            }
        }
        return moved;
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity instanceof SyncedBlockEntity synced) return synced.isWithinReach(player);
        return blockEntity == null || !blockEntity.isRemoved();
    }

    /** Live view of the block entity's upgrade list. */
    private static class UpgradeContainer implements Container {
        private final Upgradeable target;

        UpgradeContainer(Upgradeable target) {
            this.target = target;
        }

        private NonNullList<ItemStack> list() {
            return target.getUpgrades();
        }

        @Override public int getContainerSize() { return list().size(); }
        @Override public boolean isEmpty() { return list().stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return list().get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = list().get(slot);
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = s.split(amount);
            if (s.isEmpty()) list().set(slot, ItemStack.EMPTY);
            target.onUpgradesChanged();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = list().get(slot);
            list().set(slot, ItemStack.EMPTY);
            target.onUpgradesChanged();
            return s;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            list().set(slot, stack);
            target.onUpgradesChanged();
        }

        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() { target.onUpgradesChanged(); }
        @Override public boolean stillValid(Player player) { return true; }

        @Override
        public void clearContent() {
            for (int i = 0; i < list().size(); i++) list().set(i, ItemStack.EMPTY);
            target.onUpgradesChanged();
        }
    }
}
