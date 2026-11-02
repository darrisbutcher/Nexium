package net.storagenexus.nexium;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.storagenexus.aiot.module.NexusLink;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModComponents;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nexium armor's Nexus Restock set bonus, with the chestplate linked to a Crafting Terminal:
 * <ul>
 *   <li>a hotbar stack being used up (blocks placed, torches, arrows...) is topped up to a full stack from the network
 *       when it gets down to {@value #TOP_UP_AT}, or refilled if the last one is used. Only use counts: a stack moved
 *       or thrown away as a whole is left alone;</li>
 *   <li>when the hunger bar is down by {@value #EAT_WHEN_MISSING} or more, the player eats the most filling plain food in
 *       the network (none with effects, such as golden apples, rotten flesh or pufferfish).</li>
 * </ul>
 * The network is reached as for the Nexus Link module (its terminal's chunk loaded, or an on-demand anchor woken).
 */
public final class NexusRestock {
    static final int TOP_UP_AT = 8;
    static final int EAT_WHEN_MISSING = 6;
    private static final int EAT_CHECK_TICKS = 40;

    /** Each player's hotbar a tick ago. */
    private static final Map<UUID, ItemStack[]> LAST_HOTBAR = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean eatCheck = server.getTickCount() % EAT_CHECK_TICKS == 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player, eatCheck);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_HOTBAR.remove(handler.getPlayer().getUUID()));
    }

    /** One tick of restocking for this player (public for the game tests); {@code eatCheck}: also see if they should eat. */
    public static void tick(ServerPlayer player, boolean eatCheck) {
        GlobalPos link = player.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.TERMINAL_LINK);
        if (link == null || player.isSpectator() || !NexiumArmor.active(player, ArmorBonus.NEXUS_RESTOCK)) {
            LAST_HOTBAR.remove(player.getUUID());
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack[] last = LAST_HOTBAR.get(player.getUUID());
        if (last != null) {
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) restockSlot(player, link, inventory, slot, last[slot]);
        }
        ItemStack[] now = new ItemStack[Inventory.getSelectionSize()];
        for (int slot = 0; slot < now.length; slot++) now[slot] = inventory.getItem(slot).copy();
        LAST_HOTBAR.put(player.getUUID(), now);
        if (eatCheck) eat(player, link);
    }

    private static void restockSlot(ServerPlayer player, GlobalPos link, Inventory inventory, int slot, ItemStack before) {
        if (before.isEmpty() || before.getMaxStackSize() <= 1) return;
        ItemStack now = inventory.getItem(slot);
        boolean same = ItemStack.isSameItemSameComponents(now, before);
        int used = before.getCount() - (same ? now.getCount() : 0);
        // Used, a little at a time: never a whole stack moved or thrown away
        boolean usedUp = now.isEmpty() && before.getCount() <= 2;
        boolean runningLow = same && used > 0 && used <= 2 && now.getCount() <= TOP_UP_AT;
        if (!usedUp && !runningLow) return;
        StorageNetwork network = NexusLink.network(player, link);
        if (network == null || Transaction.isOpen()) return;
        int want = before.getMaxStackSize() - (same ? now.getCount() : 0);
        long got;
        try (Transaction tx = Transaction.openOuter()) {
            got = network.extractItem(ItemVariant.of(before), want, tx);
            tx.commit();
        }
        if (got <= 0) return;
        if (same) now.grow((int) got);
        else inventory.setItem(slot, before.copyWithCount((int) got));
    }

    /** Eats the most filling plain food in the network if the player is hungry enough. */
    private static void eat(ServerPlayer player, GlobalPos link) {
        int missing = 20 - player.getFoodData().getFoodLevel();
        if (missing < EAT_WHEN_MISSING) return;
        StorageNetwork network = NexusLink.network(player, link);
        if (network == null || Transaction.isOpen()) return;
        ItemVariant best = null;
        int bestNutrition = 0;
        for (ItemVariant variant : network.listItems().keySet()) {
            int nutrition = plainNutrition(variant.toStack());
            if (nutrition > bestNutrition) {
                best = variant;
                bestNutrition = nutrition;
            }
        }
        if (best == null) return;
        try (Transaction tx = Transaction.openOuter()) {
            if (network.extractItem(best, 1, tx) != 1) return;
            tx.commit();
        }
        ItemStack food = best.toStack(1);
        ItemStack leftover = food.finishUsingItem(player.level(), player);
        // A bowl or bottle goes back into the network, else into the inventory
        if (!leftover.isEmpty()) {
            long back;
            try (Transaction tx = Transaction.openOuter()) {
                back = network.insertItem(ItemVariant.of(leftover), leftover.getCount(), tx);
                tx.commit();
            }
            leftover.shrink((int) back);
            if (!leftover.isEmpty() && !player.getInventory().add(leftover)) player.drop(leftover, false, Prediction.SERVER_ONLY);
        }
    }

    /** The hunger a food fills, or 0 if it isn't a plain food (it has effects, or isn't food at all). */
    static int plainNutrition(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (food == null || consumable == null || !consumable.onConsumeEffects().isEmpty()) return 0;
        if (stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)) return 0;
        return food.nutrition();
    }

    /** For tests: forget a player's last hotbar. */
    static void reset(@Nullable UUID player) {
        if (player == null) LAST_HOTBAR.clear();
        else LAST_HOTBAR.remove(player);
    }

    private NexusRestock() {}
}
