package net.storagenexus.essentials.slots;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.NexusEssentials;
import org.jspecify.annotations.Nullable;

/** One of the extra slots (trinket, wings, arrows, totem, backpack) as it appears in the player's inventory screen. */
public class ExtraSlotMenuSlot extends Slot {
    public static final Identifier EMPTY_TRINKET_ICON = NexusEssentials.id("container/slot/trinket");
    public static final Identifier EMPTY_WINGS_ICON = NexusEssentials.id("container/slot/wings");
    public static final Identifier EMPTY_ARROWS_ICON = NexusEssentials.id("container/slot/arrows");
    public static final Identifier EMPTY_TOTEM_ICON = NexusEssentials.id("container/slot/totem");
    public static final Identifier EMPTY_BACKPACK_ICON = NexusEssentials.id("container/slot/backpack");

    private final Player owner;

    public ExtraSlotMenuSlot(ExtraSlots container, Player owner, int index, int x, int y) {
        super(container, index, x, y);
        this.owner = owner;
    }

    public boolean isWingsSlot() {
        return getContainerSlot() == ExtraSlots.WINGS;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container.canPlaceItem(getContainerSlot(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return ExtraSlots.maxStack(getContainerSlot());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !ExtraSlots.isBound(getItem(), player) && super.mayPickup(player);
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        // Plays the elytra's equip sound, like putting it on in the chest slot
        if (isWingsSlot()) owner.onEquipItem(EquipmentSlot.CHEST, previous, stack);
        super.setByPlayer(stack, previous);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return switch (getContainerSlot()) {
            case ExtraSlots.WINGS -> EMPTY_WINGS_ICON;
            case ExtraSlots.ARROWS -> EMPTY_ARROWS_ICON;
            case ExtraSlots.TOTEM -> EMPTY_TOTEM_ICON;
            case ExtraSlots.BACKPACK -> EMPTY_BACKPACK_ICON;
            default -> EMPTY_TRINKET_ICON;
        };
    }
}
