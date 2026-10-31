package net.storagenexus.filter;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;

import java.util.ArrayList;
import java.util.List;

/** Ghost-slot editor for filters. Clicking a slot copies the held item (it is not consumed). */
public class FilterMenu extends BaseMenu {
    public static final int BTN_WHITELIST = 0, BTN_COMPONENTS = 1;

    private final InteractionHand hand;
    private final Player player;
    private final SimpleContainer ghost = new SimpleContainer(FilterData.SIZE);
    private final ContainerData flags;
    private final int lockedSlot;

    public FilterMenu(int id, Inventory inventory, Integer handOrdinal) {
        super(ModMenus.FILTER, id);
        this.hand = InteractionHand.values()[handOrdinal];
        this.player = inventory.player;
        ItemStack stack = player.getItemInHand(hand);
        FilterData data = stack.getItem() instanceof FilterItem fi ? fi.data(stack) : FilterData.empty(false);
        List<ItemStack> entries = data.toList();
        for (int i = 0; i < FilterData.SIZE; i++) ghost.setItem(i, entries.get(i));

        for (int i = 0; i < FilterData.SIZE; i++) {
            addCustomSlot(new Slot(ghost, i, 8 + i * 18, 20) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }
        addPlayerSlots(inventory, 8, 84);
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? playerSlotStart + 27 + inventory.getSelectedSlot() : -1;

        if (player.level().isClientSide()) {
            flags = new SimpleContainerData(2);
        } else {
            flags = new ContainerData() {
                @Override public int get(int i) {
                    FilterData d = current();
                    return i == 0 ? (d.whitelist() ? 1 : 0) : (d.matchComponents() ? 1 : 0);
                }
                @Override public void set(int i, int v) {}
                @Override public int getCount() { return 2; }
            };
        }
        addDataSlots(flags);
    }

    private FilterData current() {
        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof FilterItem fi ? fi.data(stack) : FilterData.empty(false);
    }

    private void save(FilterData data) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof FilterItem) stack.set(ModComponents.FILTER, data);
    }

    private void saveGhost() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < FilterData.SIZE; i++) list.add(ghost.getItem(i).copy());
        save(current().withEntries(list));
    }

    public boolean whitelist() { return flags.get(0) == 1; }
    public boolean matchComponents() { return flags.get(1) == 1; }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        if (slotId == lockedSlot) return;
        if (clickType == ClickType.SWAP && hand == InteractionHand.MAIN_HAND && button == p.getInventory().getSelectedSlot()) return;
        if (slotId >= 0 && slotId < FilterData.SIZE) {
            ItemStack carried = getCarried();
            ghost.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            if (!p.level().isClientSide()) saveGhost();
            return;
        }
        super.clicked(slotId, button, clickType, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (index < FilterData.SIZE || index == lockedSlot) return ItemStack.EMPTY;
        ItemStack s = slots.get(index).getItem();
        if (s.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < FilterData.SIZE; i++) {
            if (ItemStack.isSameItemSameComponents(ghost.getItem(i), s)) return ItemStack.EMPTY;
        }
        for (int i = 0; i < FilterData.SIZE; i++) {
            if (ghost.getItem(i).isEmpty()) {
                ghost.setItem(i, s.copyWithCount(1));
                if (!p.level().isClientSide()) saveGhost();
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        FilterData d = current();
        if (id == BTN_WHITELIST) save(d.withWhitelist(!d.whitelist()));
        else if (id == BTN_COMPONENTS) save(d.withMatchComponents(!d.matchComponents()));
        return true;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand).getItem() instanceof FilterItem;
    }
}
