package net.nexium.gametest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.nexium.essentials.client.NexiumEssentialsClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The Cycle Trades key in a real villager's trading screen: C (listed under Nexium) gives a villager nobody has traded with a new set of
 * trades, and does nothing once a trade has been used. Takes a screenshot of the trading screen after cycling.
 */
public class TradeCyclingClientTest implements FabricClientGameTest {
    private static String offers(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            StringBuilder sb = new StringBuilder();
            for (MerchantOffer offer : ((MerchantScreen) mc.gui.screen()).getMenu().getOffers()) {
                sb.append(offer.getCostA().getItem()).append('x').append(offer.getCostA().getCount()).append("->")
                        .append(offer.getResult().getItem()).append('x').append(offer.getResult().getCount()).append(';');
            }
            return sb.toString();
        });
    }

    /** Every trade the villager has on the server, as text. */
    private static String serverOffers(Villager villager) {
        StringBuilder sb = new StringBuilder();
        for (MerchantOffer offer : villager.getOffers()) sb.append(offer.getCostA()).append("->").append(offer.getResult()).append(';');
        return sb.toString();
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            check(Arrays.asList(mc.options.keyMappings).contains(NexiumEssentialsClient.CYCLE_TRADES), "the Cycle Trades key is in the game's key list");
            check(NexiumEssentialsClient.CYCLE_TRADES.getDefaultKey().getValue() == InputConstants.KEY_C, "it defaults to C");
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            ctx.waitTicks(20);
            Villager[] villager = new Villager[1];
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                ServerLevel level = player.level();
                BlockPos at = player.blockPosition().offset(0, 0, 2);
                for (int y = 0; y <= 3; y++) level.setBlockAndUpdate(at.above(y), Blocks.AIR.defaultBlockState());
                Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
                Holder<VillagerProfession> farmer = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.FARMER);
                v.setVillagerData(v.getVillagerData().withProfession(farmer));
                v.setNoAi(true);
                v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
                level.addFreshEntity(v);
                villager[0] = v;
                v.getOffers();
                v.mobInteract(player, InteractionHand.MAIN_HAND);
            });
            ctx.waitTicks(15);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof MerchantScreen), "right-clicking the villager opens the trading screen");
            ctx.runOnClient(mc -> mc.gui.toastManager().clear());

            // C: new trades, again and again until the set has changed (the farmer's trades are picked at random)
            Set<String> seen = new HashSet<>();
            seen.add(offers(ctx));
            for (int i = 0; i < 12 && seen.size() < 2; i++) {
                ctx.getInput().pressKey(NexiumEssentialsClient.CYCLE_TRADES);
                ctx.waitTicks(8);
                seen.add(offers(ctx));
            }
            check(seen.size() >= 2, "C gave the villager a different set of trades, shown in the open screen: " + seen);
            check(world.getServer().computeOnServer(server -> villager[0].getVillagerData().level() == 1 && villager[0].getVillagerXp() == 0), "it is still a Novice with no experience");
            System.out.println("[nexus] trade cycling: " + ctx.takeScreenshot("nexus_trade_cycling"));

            // Once a trade has been used, the trades stay
            world.getServer().runOnServer(server -> villager[0].getOffers().get(0).increaseUses());
            String before = world.getServer().computeOnServer(server -> serverOffers(villager[0]));
            for (int i = 0; i < 4; i++) {
                ctx.getInput().pressKey(NexiumEssentialsClient.CYCLE_TRADES);
                ctx.waitTicks(6);
            }
            String after = world.getServer().computeOnServer(server -> serverOffers(villager[0]));
            check(before.equals(after), "after a trade was used C changes nothing: " + before + " then " + after);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
