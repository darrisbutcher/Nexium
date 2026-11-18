package net.nexium.essentials.trading;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.nexium.essentials.mixin.VillagerInvoker;
import org.jspecify.annotations.Nullable;

/**
 * Trade cycling, on the server: the Cycle Trades key in a villager's trading screen gives a villager nobody has traded with
 * a fresh set of trades, the way breaking and replacing its job site block does, without the block. Only a Novice who has
 * earned no experience and has had none of its trades used can be cycled: once you trade with a villager its trades stay.
 */
public final class TradeCycling {
    public enum Result { CYCLED, NOT_TRADING, NO_TRADES, ALREADY_TRADED }

    /** The villager this player has a trading screen open with, or null. */
    public static @Nullable Villager tradingWith(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu)) return null;
        for (Villager villager : player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0), v -> v.getTradingPlayer() == player)) {
            return villager;
        }
        return null;
    }

    /** Whether nobody has traded with this villager: a Novice with no experience and no trade used. */
    public static boolean untraded(Villager villager) {
        if (villager.getVillagerData().level() != 1 || villager.getVillagerXp() != 0) return false;
        for (MerchantOffer offer : villager.getOffers()) if (offer.getUses() > 0) return false;
        return true;
    }

    /** Gives the villager the player is trading with a new set of trades, if it has not been traded with. */
    public static Result cycle(ServerPlayer player) {
        Villager villager = tradingWith(player);
        if (villager == null) return Result.NOT_TRADING;
        if (villager.getOffers().isEmpty()) return Result.NO_TRADES;
        if (!untraded(villager)) {
            player.sendOverlayMessage(Component.translatable("message.nexium.trades_traded").withStyle(ChatFormatting.RED));
            return Result.ALREADY_TRADED;
        }
        // Forget the trades: asking for them again makes a new set for this profession and level
        villager.setOffers(null);
        MerchantOffers offers = villager.getOffers();
        ((VillagerInvoker) villager).nexium$updateSpecialPrices(player);
        player.sendMerchantOffers(player.containerMenu.containerId, offers, villager.getVillagerData().level(), villager.getVillagerXp(),
                villager.showProgressBar(), villager.canRestock());
        player.sendOverlayMessage(Component.translatable("message.nexium.trades_cycled").withStyle(ChatFormatting.GREEN));
        return Result.CYCLED;
    }

    private TradeCycling() {}
}
