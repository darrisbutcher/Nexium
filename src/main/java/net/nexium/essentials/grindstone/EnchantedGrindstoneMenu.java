package net.nexium.essentials.grindstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModMenus;

import java.util.List;

/**
 * The Enchanted Grindstone's menu: a slot for the item to take enchantments off, a slot for plain Books, and the player's
 * inventory. Its screen lists the enchantments on the item; clicking one is a menu button click whose id is that
 * enchantment's registry number, so it names the same enchantment however the two sides' lists have moved on. The click takes
 * the enchantment off the item, uses up one Book and gives the player a new Enchanted Book that holds just that enchantment.
 * It costs nothing.
 */
public class EnchantedGrindstoneMenu extends AbstractContainerMenu {
    // Layout, shared with the screen
    public static final int WIDTH = 176, HEIGHT = 184;
    public static final int INPUT_X = 12, INPUT_Y = 26, BOOK_X = 12, BOOK_Y = 62;
    public static final int LIST_X = 40, LIST_Y = 18, LIST_W = 128, ROW_H = 14, VISIBLE_ROWS = 5;
    public static final int INVENTORY_X = 8, INVENTORY_Y = 102;

    public static final int INPUT_SLOT = 0, BOOK_SLOT = 1;
    private static final int FIRST_INVENTORY_SLOT = 2, HOTBAR_START = FIRST_INVENTORY_SLOT + 27, END = HOTBAR_START + 9;

    private final Container container = new SimpleContainer(2);
    private final ContainerLevelAccess access;
    private final Player player;

    /** Client side. */
    public EnchantedGrindstoneMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    /** Server side. */
    public EnchantedGrindstoneMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ENCHANTED_GRINDSTONE, id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new Slot(container, INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !EnchantmentPulling.entries(stack).isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, BOOK_SLOT, BOOK_X, BOOK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
    }

    /** The enchantments on the item in the input slot (what the screen lists). */
    public List<EnchantmentPulling.Entry> entries() {
        return EnchantmentPulling.entries(container.getItem(INPUT_SLOT));
    }

    /** Whether there is an item with something to take off (not an enchanted book that holds just one enchantment). */
    public boolean hasPullable() {
        return EnchantmentPulling.pullable(container.getItem(INPUT_SLOT));
    }

    /** Whether a plain Book is in its slot, ready to be used. */
    public boolean hasBook() {
        return container.getItem(BOOK_SLOT).is(Items.BOOK);
    }

    public ItemStack inputStack() {
        return container.getItem(INPUT_SLOT);
    }

    /** The button id that pulls this enchantment: its registry number. */
    public int buttonFor(Holder<Enchantment> enchantment) {
        return registry().getId(enchantment.value());
    }

    private Registry<Enchantment> registry() {
        return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Enchantment enchantment = registry().byId(id);
        if (enchantment == null || !hasBook()) return false;
        EnchantmentPulling.Pulled pulled = EnchantmentPulling.pull(container.getItem(INPUT_SLOT), registry().wrapAsHolder(enchantment));
        if (pulled == null) return false;

        container.setItem(INPUT_SLOT, pulled.source());
        container.removeItem(BOOK_SLOT, 1);
        player.getInventory().placeItemBackInInventory(pulled.book(), Prediction.SERVER_ONLY);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F));
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved;
        if (index < FIRST_INVENTORY_SLOT) {
            moved = moveItemStackTo(stack, FIRST_INVENTORY_SLOT, END, true);
        } else if (slots.get(INPUT_SLOT).mayPlace(stack)) {
            moved = moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false);
        } else if (slots.get(BOOK_SLOT).mayPlace(stack)) {
            moved = moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false);
        } else if (index < HOTBAR_START) {
            moved = moveItemStackTo(stack, HOTBAR_START, END, false);
        } else {
            moved = moveItemStackTo(stack, FIRST_INVENTORY_SLOT, HOTBAR_START, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, container));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ENCHANTED_GRINDSTONE);
    }
}
