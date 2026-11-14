package net.storagenexus.essentials.slots;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.essentials.backpack.BackpackItem;
import net.storagenexus.essentials.magnet.MagnetItem;

/**
 * The four slots this mod adds to every player: a trinket slot for the magnet, a wings slot for an elytra
 * (or any other item with the glider component), so a chestplate can be worn at the same time, an arrow slot
 * (a stack of arrows that bows and crossbows shoot from) and a totem slot (a Totem of Undying that saves you
 * from the slot). Saved with the player, kept through the End exit portal and on death with keepInventory, dropped on death otherwise.
 */
public final class ExtraSlots extends SimpleContainer {
    public static final int TRINKET = 0;
    public static final int WINGS = 1;
    public static final int ARROWS = 2;
    public static final int TOTEM = 3;
    public static final int BACKPACK = 4;
    /** Where the slots sit in the player's inventory menu, after vanilla's 46 (0-45). */
    public static final int MENU_TRINKET = 46;
    public static final int MENU_WINGS = 47;
    public static final int MENU_ARROWS = 48;
    public static final int MENU_TOTEM = 49;
    public static final int MENU_BACKPACK = 50;
    /** The arrow slot holds one stack; every other slot holds one item. */
    public static final int ARROWS_MAX = 64;

    private static final String TRINKET_KEY = "NexusEssentialsTrinket";
    private static final String WINGS_KEY = "NexusEssentialsWings";
    private static final String ARROWS_KEY = "NexusEssentialsArrows";
    private static final String TOTEM_KEY = "NexusEssentialsTotem";
    private static final String BACKPACK_KEY = "NexusEssentialsBackpack";

    /** Server only: the wings last sent to the players who can see the owner. */
    private ItemStack sentWings = ItemStack.EMPTY;

    public ExtraSlots() {
        super(5);
    }

    public static ExtraSlots of(Player player) {
        return ((ExtraSlotsHolder) player).nexusessentials$extraSlots();
    }

    public ItemStack trinket() {
        return getItem(TRINKET);
    }

    public ItemStack wings() {
        return getItem(WINGS);
    }

    public ItemStack arrows() {
        return getItem(ARROWS);
    }

    public ItemStack totem() {
        return getItem(TOTEM);
    }

    public ItemStack backpack() {
        return getItem(BACKPACK);
    }

    /** How many items fit in this slot. */
    public static int maxStack(int slot) {
        return slot == ARROWS ? ARROWS_MAX : 1;
    }

    public static boolean isArrows(ItemStack stack) {
        return stack.is(ItemTags.ARROWS);
    }

    /** Anything that saves you from death the way a Totem of Undying does. */
    public static boolean isTotem(ItemStack stack) {
        return stack.has(DataComponents.DEATH_PROTECTION);
    }

    /**
     * Moves as many of these arrows as fit into the arrow slot: all of them if it is empty, or those that match what
     * is in it. What is left stays in {@code stack}.
     */
    public void moveArrowsIn(ItemStack stack) {
        if (!isArrows(stack) || stack.isEmpty()) return;
        ItemStack held = arrows();
        if (held.isEmpty()) {
            setItem(ARROWS, stack.split(Math.min(stack.getMaxStackSize(), ARROWS_MAX)));
        } else if (ItemStack.isSameItemSameComponents(held, stack)) {
            int room = Math.min(held.getMaxStackSize(), ARROWS_MAX) - held.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved > 0) {
                held.grow(moved);
                stack.shrink(moved);
            }
        }
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    public static boolean isTrinket(ItemStack stack) {
        return stack.getItem() instanceof MagnetItem;
    }

    public static boolean isWings(ItemStack stack) {
        return stack.has(DataComponents.GLIDER);
    }

    /** Same rule as vanilla elytra: a glider stops working one point before it breaks. */
    public static boolean canGlideWith(ItemStack stack) {
        return isWings(stack) && !stack.nextDamageWillBreak();
    }

    /** Curse of Binding keeps an item in the slot, as it does in armor slots. */
    public static boolean isBound(ItemStack stack, Player player) {
        return !stack.isEmpty() && !player.isCreative() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case TRINKET -> isTrinket(stack);
            case WINGS -> isWings(stack);
            case ARROWS -> isArrows(stack);
            case TOTEM -> isTotem(stack);
            case BACKPACK -> isBackpack(stack);
            default -> false;
        };
    }

    @Override
    public int getMaxStackSize() {
        return ARROWS_MAX;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public void copyFrom(ExtraSlots other) {
        for (int i = 0; i < getContainerSize(); i++) setItem(i, other.getItem(i).copy());
    }

    public void save(ValueOutput output) {
        if (!trinket().isEmpty()) output.store(TRINKET_KEY, ItemStack.CODEC, trinket());
        if (!wings().isEmpty()) output.store(WINGS_KEY, ItemStack.CODEC, wings());
        if (!arrows().isEmpty()) output.store(ARROWS_KEY, ItemStack.CODEC, arrows());
        if (!totem().isEmpty()) output.store(TOTEM_KEY, ItemStack.CODEC, totem());
        if (!backpack().isEmpty()) output.store(BACKPACK_KEY, ItemStack.CODEC, backpack());
    }

    public void load(ValueInput input) {
        setItem(TRINKET, input.read(TRINKET_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setItem(WINGS, input.read(WINGS_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setItem(ARROWS, input.read(ARROWS_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setItem(TOTEM, input.read(TOTEM_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setItem(BACKPACK, input.read(BACKPACK_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    /** True once if the wings changed since they were last sent to other players. */
    boolean wingsNeedSending() {
        if (ItemStack.isSameItemSameComponents(sentWings, wings())) return false;
        sentWings = wings().copy();
        return true;
    }
}
