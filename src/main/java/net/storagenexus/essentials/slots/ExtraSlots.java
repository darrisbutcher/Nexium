package net.storagenexus.essentials.slots;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.storagenexus.essentials.magnet.MagnetItem;

/**
 * The two slots this mod adds to every player: a trinket slot for the magnet and a wings slot for an elytra
 * (or any other item with the glider component), so a chestplate can be worn at the same time.
 * Saved with the player, kept through the End exit portal and on death with keepInventory, dropped on death otherwise.
 */
public final class ExtraSlots extends SimpleContainer {
    public static final int TRINKET = 0;
    public static final int WINGS = 1;
    /** Where the two slots sit in the player's inventory menu, after vanilla's 46 (0-45). */
    public static final int MENU_TRINKET = 46;
    public static final int MENU_WINGS = 47;

    private static final String TRINKET_KEY = "NexusEssentialsTrinket";
    private static final String WINGS_KEY = "NexusEssentialsWings";

    /** Server only: the wings last sent to the players who can see the owner. */
    private ItemStack sentWings = ItemStack.EMPTY;

    public ExtraSlots() {
        super(2);
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
        return slot == TRINKET ? isTrinket(stack) : isWings(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
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
    }

    public void load(ValueInput input) {
        setItem(TRINKET, input.read(TRINKET_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        setItem(WINGS, input.read(WINGS_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    /** True once if the wings changed since they were last sent to other players. */
    boolean wingsNeedSending() {
        if (ItemStack.isSameItemSameComponents(sentWings, wings())) return false;
        sentWings = wings().copy();
        return true;
    }
}
