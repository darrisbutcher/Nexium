package net.storagenexus.dimension;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.ArrayList;
import java.util.List;

/** Screen for Dimensional Inputs (buffer + speed upgrade) and Outputs (filter), with the link name. */
public class DimensionalMenu extends BaseMenu {
    public static final int INVENTORY_TOP = 102;

    public record OpenData(BlockPos pos, String name, boolean input) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenData::pos,
                ByteBufCodecs.STRING_UTF8, OpenData::name,
                ByteBufCodecs.BOOL, OpenData::input,
                OpenData::new);
    }

    private final boolean input;
    private final @Nullable LinkedBlockEntity be;
    private final ContainerData data;
    private String clientName;
    private BlockPos clientPos = BlockPos.ZERO;

    /** Client constructor. */
    public DimensionalMenu(int id, Inventory inventory, OpenData open) {
        super(ModMenus.DIMENSIONAL, id);
        this.clientPos = open.pos();
        this.input = open.input();
        this.be = null;
        this.clientName = open.name();
        this.data = new SimpleContainerData(3);
        layout(inventory, input ? new SimpleContainer(9) : null, input ? new SimpleContainer(1) : new SimpleContainer(1));
        addDataSlots(data);
    }

    public DimensionalMenu(int id, Inventory inventory, DimensionalInputBlockEntity in) {
        super(ModMenus.DIMENSIONAL, id);
        this.input = true;
        this.be = in;
        this.clientName = in.displayName();
        this.data = new ContainerData() {
            @Override public int get(int i) { return i == 0 ? in.onlineOutputs() : in.totalOutputs(); }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 2; }
        };
        layout(inventory, in.buffer(), in.upgradeSlot());
        addDataSlots(data);
    }

    public DimensionalMenu(int id, Inventory inventory, DimensionalOutputBlockEntity out) {
        super(ModMenus.DIMENSIONAL, id);
        this.input = false;
        this.be = out;
        this.clientName = out.displayName();
        this.data = new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> out.inputOnline() ? 1 : 0;
                    case 1 -> out.linkId() != null ? 1 : 0;
                    default -> out.sideMask();
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 3; }
        };
        layout(inventory, null, out.filterSlot());
        addDataSlots(data);
    }

    private void layout(Inventory inventory, @Nullable Container buffer, Container side) {
        if (buffer != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    addCustomSlot(new Slot(buffer, col + row * 3, 62 + col * 18, 36 + row * 18));
                }
            }
            addCustomSlot(new Slot(side, 0, 152, 72) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof UpgradeItem u && u.kind() == UpgradeItem.Kind.TRANSFER_SPEED;
                }
                @Override public int getMaxStackSize() { return 2; }
            });
        } else {
            addCustomSlot(new Slot(side, 0, 152, 54) {
                @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof FilterItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
    }

    public boolean isInput() { return input; }
    public String name() { return clientName; }
    public BlockPos pos() { return clientPos; }
    public void setClientName(String name) { clientName = name; }

    /** Input: outputs online. Output: 1 if input online. */
    public int statusA() { return data.get(0); }
    /** Input: total outputs. Output: 1 if linked. */
    public int statusB() { return data.get(1); }
    /** Output only: bit per Direction (3D data value) for the sides it pushes into. */
    public int sideMask() { return data.get(2); }

    /** Output only: buttons 0-5 toggle pushing into that side (Direction 3D data value). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be instanceof DimensionalOutputBlockEntity out && id >= 0 && id < 6) {
            out.toggleSide(Direction.from3DDataValue(id));
            return true;
        }
        return false;
    }

    public void rename(ServerPlayer player, String newName) {
        if (be == null || be.linkId() == null || !(player.level() instanceof ServerLevel)) return;
        LinkRegistry reg = LinkRegistry.get(player.level().getServer());
        if (!reg.rename(be.linkId(), newName)) {
            player.sendSystemMessage(Component.translatable("message.storagenexus.name_taken", newName).withStyle(ChatFormatting.RED));
            return;
        }
        // Refresh every loaded end of this link so labels update right away.
        LinkRegistry.Link link = reg.get(be.linkId());
        List<GlobalPos> ends = new ArrayList<>(link.outputs());
        if (link.input() != null) ends.add(link.input());
        for (GlobalPos gp : ends) {
            ServerLevel l = player.level().getServer().getLevel(gp.dimension());
            if (l != null && l.isLoaded(gp.pos()) && l.getBlockEntity(gp.pos()) instanceof LinkedBlockEntity end) {
                if (end.refreshName()) end.sync();
            }
        }
        player.sendOverlayMessage(Component.translatable("message.storagenexus.renamed", link.name()));
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || be.isWithinReach(player);
    }
}
