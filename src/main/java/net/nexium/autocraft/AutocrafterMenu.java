package net.nexium.autocraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.nexium.registry.ModMenus;
import net.nexium.util.BaseMenu;
import org.jspecify.annotations.Nullable;

/** The Autocrafter's screen: its nine pattern slots, what it holds, and the player inventory. */
public class AutocrafterMenu extends BaseMenu {
    public static final int INVENTORY_TOP = 108, GRID_X = 62, GRID_Y = 20;
    private static final int D_STORED = 0, D_COUNT = 2;

    private final ContainerData data;
    private final @Nullable AutocrafterBlockEntity crafter;

    /** Client side. */
    public AutocrafterMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (AutocrafterBlockEntity) null);
    }

    /** Server side. */
    public AutocrafterMenu(int id, Inventory inventory, @Nullable AutocrafterBlockEntity crafter) {
        super(ModMenus.AUTOCRAFTER, id);
        this.crafter = crafter;
        Container patterns = crafter != null ? crafter.patterns() : new SimpleContainer(AutocrafterBlockEntity.SLOTS);
        for (int i = 0; i < AutocrafterBlockEntity.SLOTS; i++) {
            addCustomSlot(new Slot(patterns, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return PatternItem.isEncoded(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        addPlayerSlots(inventory, 8, INVENTORY_TOP);
        data = crafter == null ? new SimpleContainerData(D_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return index == D_STORED ? (int) (crafter.energy().stored() & 0x7FFF) : (int) (crafter.energy().stored() >> 15);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() { return D_COUNT; }
        };
        addDataSlots(data);
    }

    public int stored() { return data.get(D_STORED) | data.get(D_STORED + 1) << 15; }

    @Override
    public boolean stillValid(Player player) {
        return crafter == null || (!crafter.isRemoved() && player.distanceToSqr(crafter.getBlockPos().getX() + 0.5,
                crafter.getBlockPos().getY() + 0.5, crafter.getBlockPos().getZ() + 0.5) <= 64);
    }
}
