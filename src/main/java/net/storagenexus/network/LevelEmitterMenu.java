package net.storagenexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.util.BaseMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Level Emitter settings: a ghost slot for the item, buttons for the amount and for at-least /
 * fewer-than. Numbers go to the client in 15-bit halves, because a menu data slot is 16 bits.
 */
public class LevelEmitterMenu extends BaseMenu {
    public static final int BTN_MODE = 0;
    /** Menu button ids 1.. add these amounts; ids 101.. subtract them. */
    public static final int[] STEPS = {1, 10, 100, 1000};
    public static final int BTN_ADD = 1, BTN_SUB = 101;

    private final @Nullable LevelEmitterBlockEntity be;
    private final BlockPos pos;
    private final SimpleContainer ghost = new SimpleContainer(1);
    private final ContainerData data;

    public LevelEmitterMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, null, pos);
    }

    public LevelEmitterMenu(int id, Inventory inventory, LevelEmitterBlockEntity be) {
        this(id, inventory, be, be.getBlockPos());
    }

    private LevelEmitterMenu(int id, Inventory inventory, @Nullable LevelEmitterBlockEntity be, BlockPos pos) {
        super(ModMenus.LEVEL_EMITTER, id);
        this.be = be;
        this.pos = pos;
        if (be != null) ghost.setItem(0, be.item());
        addCustomSlot(new Slot(ghost, 0, 8, 19) {
            @Override public boolean mayPickup(Player p) { return false; }
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addPlayerSlots(inventory, 8, 100);
        // amount low, amount high, now low, now high, mode (0 at least, 1 fewer than), powered
        data = be == null ? new SimpleContainerData(6) : new ContainerData() {
            @Override public int get(int i) {
                long now = Math.min(be.lastCount(), Integer.MAX_VALUE);
                return switch (i) {
                    case 0 -> be.amount() & 0x7FFF;
                    case 1 -> be.amount() >> 15;
                    case 2 -> (int) (now & 0x7FFF);
                    case 3 -> (int) (now >> 15);
                    case 4 -> be.below() ? 1 : 0;
                    default -> be.getBlockState().getValue(LevelEmitterBlock.POWERED) ? 1 : 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 6; }
        };
        addDataSlots(data);
    }

    public BlockPos pos() { return pos; }
    public int amount() { return data.get(0) | data.get(1) << 15; }
    public long now() { return (long) data.get(2) | (long) data.get(3) << 15; }
    public boolean below() { return data.get(4) == 1; }
    public boolean powered() { return data.get(5) == 1; }
    public ItemStack item() { return ghost.getItem(0); }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player p) {
        if (slotId == 0) {
            ItemStack carried = getCarried();
            ghost.setItem(0, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            if (be != null && !p.level().isClientSide()) be.setItem(ghost.getItem(0));
            return;
        }
        super.clicked(slotId, button, input, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (index == 0) return ItemStack.EMPTY;
        ItemStack s = slots.get(index).getItem();
        if (!s.isEmpty()) {
            ghost.setItem(0, s.copyWithCount(1));
            if (be != null && !p.level().isClientSide()) be.setItem(ghost.getItem(0));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return true;
        if (id == BTN_MODE) be.toggleBelow();
        else if (id >= BTN_ADD && id < BTN_ADD + STEPS.length) be.setAmount((int) Math.min(LevelEmitterBlockEntity.MAX_AMOUNT, (long) be.amount() + STEPS[id - BTN_ADD]));
        else if (id >= BTN_SUB && id < BTN_SUB + STEPS.length) be.setAmount(be.amount() - STEPS[id - BTN_SUB]);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 64);
    }
}
