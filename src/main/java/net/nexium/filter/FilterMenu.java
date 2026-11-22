package net.nexium.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
import net.nexium.util.Menus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Ghost-slot editor for filters. Clicking a slot copies the held item (it is not consumed). */
public class FilterMenu extends BaseMenu {
    public static final int BTN_WHITELIST = 0, BTN_COMPONENTS = 1, BTN_SPECIAL = 2;
    /** Where the player's inventory starts: lower on an Advanced Filter, whose screen has a row more for its tags. */
    public static final int INVENTORY_TOP = 84, ADVANCED_INVENTORY_TOP = 102;

    /**
     * Opening data. {@code hand} is the hand holding the filter, or -1 when the filter sits in a block's filter slot (a conduit side, Network
     * Bus, Connector or Disk Drive); then {@code stack} is a copy of it, since the client cannot look inside the block.
     */
    public record OpenData(int hand, ItemStack stack) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenData::hand,
                ItemStack.OPTIONAL_STREAM_CODEC, OpenData::stack,
                OpenData::new);
    }

    /** The slot of a block that the filter being edited sits in (a live view on the server, a copy on the client); null for a filter in hand. */
    private final @Nullable Container box;
    private final @Nullable InteractionHand hand;
    /** Server only, for a filter in a block: whether the block can still be used, what to run when the filter was changed, and the screen to return to. */
    private final @Nullable Predicate<Player> blockValid;
    private final @Nullable Runnable changed;
    private final @Nullable Runnable onClose;
    private final Player player;
    private final SimpleContainer ghost = new SimpleContainer(FilterData.SIZE);
    private final ContainerData flags;
    private final int lockedSlot;
    private final boolean advanced;

    /** Edits the filter in a hand. */
    public FilterMenu(int id, Inventory inventory, Integer handOrdinal) {
        this(id, inventory, InteractionHand.values()[handOrdinal], null, null, null, null);
    }

    /** Client side. */
    public FilterMenu(int id, Inventory inventory, OpenData open) {
        this(id, inventory, open.hand() < 0 ? null : InteractionHand.values()[open.hand()],
                open.hand() < 0 ? new SimpleContainer(open.stack()) : null, null, null, null);
    }

    /** Edits the filter in a block's filter slot (server side); closing returns to {@code reopen}. */
    public FilterMenu(int id, Inventory inventory, Container box, Predicate<Player> valid, Runnable changed, Runnable reopen) {
        this(id, inventory, null, box, valid, changed, reopen);
    }

    /**
     * Opens the editor for the filter {@code source} gives, which sits in a block. It stays valid while {@code valid} says so, saving calls
     * {@code changed} and closing it calls {@code reopen}.
     */
    public static void openFor(ServerPlayer player, Supplier<ItemStack> source, Predicate<Player> valid, Runnable changed, Runnable reopen) {
        ItemStack stack = source.get();
        if (!(stack.getItem() instanceof FilterItem)) return;
        Container view = new SimpleContainer(1) {
            @Override public ItemStack getItem(int slot) { return source.get(); }
            @Override public void setItem(int slot, ItemStack s) {}
            @Override public boolean stillValid(Player p) { return true; }
        };
        Menus.open(player, stack.getHoverName(), new OpenData(-1, stack.copy()),
                (id, inv, p) -> new FilterMenu(id, inv, view, valid, changed, reopen));
    }

    private FilterMenu(int id, Inventory inventory, @Nullable InteractionHand hand, @Nullable Container box,
                       @Nullable Predicate<Player> valid, @Nullable Runnable changed, @Nullable Runnable onClose) {
        super(ModMenus.FILTER, id);
        this.hand = hand;
        this.box = hand == null ? box : null;
        this.blockValid = valid;
        this.changed = changed;
        this.onClose = onClose;
        this.player = inventory.player;
        ItemStack stack = stack();
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
        // For a filter in a block, a hidden slot carries the filter to the client, so it sees the tags and settings as they change
        if (this.box != null) {
            addSlot(new Slot(this.box, 0, -2000, -2000) {
                @Override public boolean mayPlace(ItemStack s) { return false; }
                @Override public boolean mayPickup(Player p) { return false; }
            });
        }

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

    /** The filter being edited: the one in hand, or the one in the block's slot. */
    private ItemStack stack() {
        if (box != null) return box.getItem(0);
        return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    private FilterData current() {
        ItemStack stack = stack();
        return stack.getItem() instanceof FilterItem fi ? fi.data(stack) : FilterData.empty(false);
    }

    private void save(FilterData data) {
        ItemStack stack = stack();
        if (!(stack.getItem() instanceof FilterItem)) return;
        stack.set(ModComponents.FILTER, data);
        if (changed != null) changed.run();
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
        if (box != null) return stack().getItem() instanceof FilterItem && (blockValid == null || blockValid.test(p));
        return hand != null && p.getItemInHand(hand).getItem() instanceof FilterItem;
    }

    /** Closing the editor of a filter in a block goes back to the screen it was opened from (one tick later, once this menu has closed). */
    @Override
    public void removed(Player p) {
        super.removed(p);
        if (onClose != null && p instanceof ServerPlayer sp && sp.level().getServer() != null) {
            // Queued, never run on the spot (execute can run at once): the player's open menu is only replaced after this call returns
            var server = sp.level().getServer();
            server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
                if (!sp.hasDisconnected() && sp.isAlive() && sp.containerMenu == sp.inventoryMenu && blockValid != null && blockValid.test(sp)) onClose.run();
            }));
        }
    }
}
