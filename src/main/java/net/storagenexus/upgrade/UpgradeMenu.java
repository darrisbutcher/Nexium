package net.storagenexus.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;
import net.storagenexus.util.SyncedBlockEntity;

/** Four upgrade slots for drawers, fluid drawers and tanks. */
public class UpgradeMenu extends BaseMenu {
    private final Upgradeable target;
    private final BlockEntity blockEntity;

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
            addCustomSlot(new Slot(container, i, 53 + i * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return UpgradeMenu.this.target == null
                            ? stack.getItem() instanceof UpgradeItem
                            : UpgradeMenu.this.target.acceptsUpgrade(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return UpgradeMenu.this.target == null || UpgradeMenu.this.target.canRemoveUpgrade(slotIndex);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addPlayerSlots(inventory, 8, 51);
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
