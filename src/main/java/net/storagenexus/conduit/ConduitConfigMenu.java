package net.storagenexus.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.filter.FilterItem;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.upgrade.UpgradeItem;
import net.storagenexus.util.BaseMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Per-side settings for one conduit bundle. Pick a side and a conduit type, then set mode,
 * colour channel, redstone control, priority and round-robin; the two slots hold that side's
 * filter and speed upgrades.
 */
public class ConduitConfigMenu extends BaseMenu {
    public static final int INVENTORY_TOP = 112;

    public static final int BTN_SIDE = 0;      // 0..5
    public static final int BTN_TYPE = 10;     // 10..12
    public static final int BTN_MODE = 20;
    public static final int BTN_CHANNEL_NEXT = 21;
    public static final int BTN_CHANNEL_PREV = 22;
    public static final int BTN_REDSTONE = 23;
    public static final int BTN_PRIORITY_UP = 24;
    public static final int BTN_PRIORITY_DOWN = 25;
    public static final int BTN_ROUND_ROBIN = 26;

    public static final int D_SIDE = 0, D_TYPE = 1, D_MODE = 2, D_CHANNEL = 3, D_REDSTONE = 4, D_PRIORITY = 5,
            D_RR = 6, D_TYPES = 7, D_CONNECTED = 8;

    public record OpenData(BlockPos pos, int side) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenData::pos,
                ByteBufCodecs.VAR_INT, OpenData::side,
                OpenData::new);
    }

    private final @Nullable ConduitBundleBlockEntity be;
    private final ContainerData data;
    private int side;
    private int type;

    /** Client constructor. */
    public ConduitConfigMenu(int id, Inventory inventory, OpenData open) {
        super(ModMenus.CONDUIT_CONFIG, id);
        this.be = null;
        this.data = new SimpleContainerData(9);
        layout(inventory, new SimpleContainer(2));
        addDataSlots(data);
    }

    public ConduitConfigMenu(int id, Inventory inventory, ConduitBundleBlockEntity be, int side) {
        super(ModMenus.CONDUIT_CONFIG, id);
        this.be = be;
        this.side = side;
        for (ConduitType t : ConduitType.VALUES) {
            if (be.has(t)) {
                type = t.ordinal();
                break;
            }
        }
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                SideConfig c = cfg();
                return switch (i) {
                    case D_SIDE -> ConduitConfigMenu.this.side;
                    case D_TYPE -> type;
                    case D_MODE -> c.mode.ordinal();
                    case D_CHANNEL -> c.channel;
                    case D_REDSTONE -> c.redstone.ordinal();
                    case D_PRIORITY -> c.priority + 100;
                    case D_RR -> c.roundRobin ? 1 : 0;
                    case D_TYPES -> be.typeMask();
                    case D_CONNECTED -> be.connectsTo(ConduitType.VALUES[type], Direction.from3DDataValue(ConduitConfigMenu.this.side)) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 9; }
        };
        layout(inventory, new SideSlots());
        addDataSlots(data);
    }

    private SideConfig cfg() {
        return be.config(ConduitType.VALUES[type], Direction.from3DDataValue(side));
    }

    private void layout(Inventory inventory, Container sideSlots) {
        addCustomSlot(new Slot(sideSlots, 0, 152, 62) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof FilterItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addCustomSlot(new Slot(sideSlots, 1, 152, 84) {
            @Override public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof UpgradeItem u && u.kind() == UpgradeItem.Kind.CONDUIT_SPEED;
            }
            @Override public int getMaxStackSize() { return 3; }
        });
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
    }

    public int get(int index) { return data.get(index); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return true;
        if (id >= BTN_SIDE && id < BTN_SIDE + 6) {
            side = id - BTN_SIDE;
            return true;
        }
        if (id >= BTN_TYPE && id < BTN_TYPE + 3) {
            if (be.has(ConduitType.VALUES[id - BTN_TYPE])) type = id - BTN_TYPE;
            return true;
        }
        if (!be.has(ConduitType.VALUES[type])) return true;
        SideConfig c = cfg();
        switch (id) {
            case BTN_MODE -> c.mode = c.mode.next();
            case BTN_CHANNEL_NEXT -> c.channel = (c.channel + 1) % 16;
            case BTN_CHANNEL_PREV -> c.channel = (c.channel + 15) % 16;
            case BTN_REDSTONE -> c.redstone = c.redstone.next();
            case BTN_PRIORITY_UP -> c.priority = Math.min(SideConfig.MAX_PRIORITY, c.priority + 1);
            case BTN_PRIORITY_DOWN -> c.priority = Math.max(-SideConfig.MAX_PRIORITY, c.priority - 1);
            case BTN_ROUND_ROBIN -> c.roundRobin = !c.roundRobin;
            default -> { return true; }
        }
        be.changed();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || be.isWithinReach(player);
    }

    /** Live view of the selected side's filter and upgrade stacks. */
    private class SideSlots implements Container {
        private ItemStack get(int slot) { return slot == 0 ? cfg().filter : cfg().upgrades; }

        private void set(int slot, ItemStack stack) {
            if (slot == 0) cfg().filter = stack;
            else cfg().upgrades = stack;
            be.changed();
        }

        @Override public int getContainerSize() { return 2; }
        @Override public boolean isEmpty() { return get(0).isEmpty() && get(1).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack s = get(slot);
            if (s.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = s.split(amount);
            set(slot, s.isEmpty() ? ItemStack.EMPTY : s);
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = get(slot);
            set(slot, ItemStack.EMPTY);
            return s;
        }

        @Override public void setItem(int slot, ItemStack stack) { set(slot, stack); }
        @Override public void setChanged() { be.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { set(0, ItemStack.EMPTY); set(1, ItemStack.EMPTY); }
    }
}
