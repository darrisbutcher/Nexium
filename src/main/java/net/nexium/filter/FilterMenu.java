package net.nexium.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;

import java.util.ArrayList;
import java.util.List;

/** Ghost-slot editor for filters. Clicking a slot copies the held item (it is not consumed). */
public class FilterMenu extends BaseMenu {
    public static final int BTN_WHITELIST = 0, BTN_COMPONENTS = 1, BTN_SPECIAL = 2;
    /** Where the player's inventory starts: lower on an Advanced Filter, whose screen has a row more for its tags. */
    public static final int INVENTORY_TOP = 84, ADVANCED_INVENTORY_TOP = 102;

    private final InteractionHand hand;
    private final Player player;
    private final SimpleContainer ghost = new SimpleContainer(FilterData.SIZE);
    private final ContainerData flags;
    private final int lockedSlot;
    private final boolean advanced;

    public FilterMenu(int id, Inventory inventory, Integer handOrdinal) {
        super(ModMenus.FILTER, id);
        this.hand = InteractionHand.values()[handOrdinal];
        this.player = inventory.player;
        ItemStack stack = player.getItemInHand(hand);
        this.advanced = stack.getItem() instanceof FilterItem filterItem && filterItem.isAdvanced();
        FilterData data = stack.getItem() instanceof FilterItem fi ? fi.data(stack) : FilterData.empty(false);
        List<ItemStack> entries = data.toList();
        for (int i = 0; i < FilterData.SIZE; i++) ghost.setItem(i, entries.get(i));

        for (int i = 0; i < FilterData.SIZE; i++) {
            addCustomSlot(new Slot(ghost, i, 8 + i * 18, 20) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }
        addPlayerSlots(inventory, 8, advanced ? ADVANCED_INVENTORY_TOP : INVENTORY_TOP);
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? playerSlotStart + 27 + inventory.getSelectedSlot() : -1;

        if (player.level().isClientSide()) {
            flags = new SimpleContainerData(3);
        } else {
            flags = new ContainerData() {
                @Override public int get(int i) {
                    FilterData d = current();
                    return switch (i) {
                        case 0 -> d.whitelist() ? 1 : 0;
                        case 1 -> d.matchComponents() ? 1 : 0;
                        default -> d.special().ordinal();
                    };
                }
                @Override public void set(int i, int v) {}
                @Override public int getCount() { return 3; }
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
    /** Whether this is an Advanced Filter, which has tags and the special matches. */
    public boolean advanced() { return advanced; }
    public FilterData.Special special() { return FilterData.Special.values()[Math.floorMod(flags.get(2), FilterData.Special.values().length)]; }
    /** The tags chosen for the filter in hand (read from the item, so the client has them too). */
    public java.util.List<Identifier> tags() { return current().tags(); }

    /**
     * Replaces the filter's chosen tags (the tag picker sends them). Only an Advanced Filter takes tags; unknown tags and repeats are
     * dropped, and no more than {@link FilterData#MAX_TAGS} are kept.
     */
    public void setTags(java.util.List<Identifier> ids) {
        if (!advanced) return;
        List<Identifier> clean = new ArrayList<>();
        for (Identifier id : ids) {
            if (clean.size() >= FilterData.MAX_TAGS) break;
            if (!clean.contains(id) && BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).isPresent()) clean.add(id);
        }
        save(current().withTags(clean));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player p) {
        if (slotId == lockedSlot) return;
        if (clickType == ContainerInput.SWAP && hand == InteractionHand.MAIN_HAND && button == p.getInventory().getSelectedSlot()) return;
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
        else if (id == BTN_SPECIAL && advanced) save(d.withSpecial(d.special().next()));
        return true;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getItemInHand(hand).getItem() instanceof FilterItem;
    }
}
