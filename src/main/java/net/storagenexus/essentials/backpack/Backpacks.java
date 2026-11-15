package net.storagenexus.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.util.Menus;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Opening a Backpack's screen, and reaching its contents from the modules, on the server. */
public final class Backpacks {
    /** The menu's source: the Backpack worn in the backpack slot. Any other source is the inventory slot holding it. */
    public static final int EQUIPPED = -1;

    /**
     * The Backpack this player wears if the module of this type is in it and switched on and the player is alive and not a
     * spectator: otherwise an empty stack. It works with the Backpack's screen open too (see {@link #read}, {@link #write}).
     */
    public static ItemStack activeFor(ServerPlayer player, BackpackModuleType type) {
        ItemStack backpack = ExtraSlots.of(player).backpack();
        if (!(backpack.getItem() instanceof BackpackItem) || !BackpackModules.active(backpack, type)) return ItemStack.EMPTY;
        if (!player.isAlive() || player.isSpectator()) return ItemStack.EMPTY;
        return backpack;
    }

    /**
     * The screen of this Backpack, if the player has it open. Edits to its contents have to go through the screen: it keeps
     * its own copy of the slots and writes all of it back on every change, so an edit made to the saved data would be put
     * back over (and the screen would not show it).
     */
    public static @Nullable BackpackMenu openMenu(ServerPlayer player, ItemStack backpack) {
        return player.containerMenu instanceof BackpackMenu menu && menu.backpack() == backpack ? menu : null;
    }

    /** The Backpack's slots as a module should see them: as the open screen holds them, otherwise as saved. Copies. */
    public static NonNullList<ItemStack> read(ServerPlayer player, ItemStack backpack) {
        BackpackMenu menu = openMenu(player, backpack);
        return menu != null ? menu.contentsCopy() : BackpackContents.read(backpack);
    }

    /** Stores these slots in the Backpack: through the open screen (which saves and shows them) if there is one, otherwise straight into the saved data. */
    public static void write(ServerPlayer player, ItemStack backpack, List<ItemStack> slots) {
        BackpackMenu menu = openMenu(player, backpack);
        if (menu != null) menu.replaceContents(slots);
        else BackpackContents.write(backpack, slots);
    }

    /** Takes up to this many of the item out of the Backpack (last slots first), through the open screen if there is one. Returns how many came out. */
    public static int take(ServerPlayer player, ItemStack backpack, Item item, int amount) {
        NonNullList<ItemStack> slots = read(player, backpack);
        int took = BackpackContents.takeFrom(slots, item, amount);
        if (took > 0) write(player, backpack, slots);
        return took;
    }

    /** The Open Backpack key: opens the Backpack worn in the backpack slot, or says there is none. */
    public static void openEquipped(ServerPlayer player) {
        if (player.isSpectator()) return;
        if (!ExtraSlots.isBackpack(ExtraSlots.of(player).backpack())) {
            player.sendOverlayMessage(Component.translatable("message.nexusessentials.backpack_missing").withStyle(ChatFormatting.RED));
            return;
        }
        open(player, EQUIPPED);
    }

    /** Opens the Backpack at this source ({@link #EQUIPPED} or an inventory slot), if there is one. */
    public static void open(ServerPlayer player, int source) {
        if (player.isSpectator()) return;
        ItemStack backpack = BackpackMenu.stackAt(player, source);
        if (!(backpack.getItem() instanceof BackpackItem)) return;
        Menus.open(player, backpack.getHoverName(), source, (id, inventory, p) -> new BackpackMenu(id, inventory, source));
    }

    private Backpacks() {}
}
