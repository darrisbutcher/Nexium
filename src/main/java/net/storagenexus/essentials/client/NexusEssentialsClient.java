package net.storagenexus.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.storagenexus.StorageNexus;
import net.storagenexus.essentials.placement.DiagonalPlacePayload;
import net.storagenexus.essentials.registry.ModMenus;
import net.storagenexus.essentials.registry.ModPayloads;
import net.storagenexus.essentials.slots.ExtraSlots;

/** Client side of the Essentials features, set up by {@link net.storagenexus.client.StorageNexusClient}. */
public final class NexusEssentialsClient {
    /** Key binds are listed under Nexium. */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(StorageNexus.id("main"));
    /** Switches the magnet in the trinket slot on or off. M by default; change it under Controls > Key Binds. */
    public static final KeyMapping TOGGLE_MAGNET = new KeyMapping("key.nexium.toggle_magnet",
            InputConstants.KEY_M, CATEGORY);

    /**
     * Hold while right-clicking a block with a block in hand to place it diagonally: one out from the face you click and one over
     * toward the nearest edge of it. Left Alt by default; change it under Controls > Key Binds.
     */
    public static final KeyMapping DIAGONAL_PLACE = new KeyMapping("key.nexium.diagonal_place",
            InputConstants.KEY_LALT, CATEGORY);

    /** Opens the Backpack worn in the backpack slot. B by default; change it under Controls > Key Binds. */
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.nexium.open_backpack",
            InputConstants.KEY_B, CATEGORY);

    /**
     * In a villager's trading screen, gives a villager nobody has traded with a new set of trades. C by default; change it under
     * Controls > Key Binds.
     */
    public static final KeyMapping CYCLE_TRADES = new KeyMapping("key.nexium.cycle_trades",
            InputConstants.KEY_C, CATEGORY);

    /** The name of a player in the world around you (the owner of a tamed mob), or null if they are not near. */
    private static @org.jspecify.annotations.Nullable String playerName(java.util.UUID id) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        Player player = level == null ? null : level.getPlayerByUUID(id);
        return player == null ? null : player.getName().getString();
    }

    public static void init() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_MAGNET);
        KeyMappingHelper.registerKeyMapping(OPEN_BACKPACK);
        KeyMappingHelper.registerKeyMapping(DIAGONAL_PLACE);
        KeyMappingHelper.registerKeyMapping(CYCLE_TRADES);
        // The Cycle Trades key in a trading screen asks the server for new trades (it says no if the villager has been traded with)
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen)) return;
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> {
                if (CYCLE_TRADES.matches(event)) ClientPlayNetworking.send(net.storagenexus.essentials.trading.CycleTradesPayload.INSTANCE);
            });
        });
        // With the key held, a right-click on a block with a block in hand is sent as a diagonal placement instead of a normal click.
        // FAIL, not SUCCESS: SUCCESS would still send the normal click to the server (a second block in the usual cell); FAIL stops it.
        // The server swings the arm when it places.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !DIAGONAL_PLACE.isDown() || player.isSpectator()) return InteractionResult.PASS;
            if (!(player.getItemInHand(hand).getItem() instanceof BlockItem)) return InteractionResult.PASS;
            ClientPlayNetworking.send(new DiagonalPlacePayload(hit.getBlockPos(), hit.getDirection(), hit.getLocation(), hand));
            return InteractionResult.FAIL;
        });
        MenuScreens.register(ModMenus.BACKPACK, net.storagenexus.client.screen.BackpackScreen::new);
        MenuScreens.register(ModMenus.IRON_FARM, IronFarmScreen::new);
        MenuScreens.register(ModMenus.XP_FARM, XpFarmScreen::new);
        MenuScreens.register(ModMenus.FARM, FarmScreen::new);
        MenuScreens.register(ModMenus.FARM_ASSEMBLER, FarmAssemblerScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_MAGNET.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(ModPayloads.ToggleMagnet.INSTANCE);
            }
            while (OPEN_BACKPACK.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(ModPayloads.OpenBackpack.INSTANCE);
            }
        });

        // A Captured Mob's age, owner, gear and status show while Shift is held (what it looks like is always shown), then
        // the hint on letting it out. It runs ahead of the other tooltip hooks, so those hints stay below it.
        net.minecraft.resources.Identifier early = StorageNexus.id("captured_mob_details");
        ItemTooltipCallback.EVENT.addPhaseOrdering(early, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        ItemTooltipCallback.EVENT.register(early, (stack, context, flag, lines) -> {
            net.storagenexus.essentials.capture.CapturedMob mob = stack.get(net.storagenexus.essentials.registry.ModComponents.CAPTURED_MOB);
            if (mob == null) return;
            var details = net.storagenexus.essentials.capture.CapturedMobDetails.details(mob, NexusEssentialsClient::playerName);
            if (!details.isEmpty()) {
                if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) lines.addAll(details);
                else lines.add(net.minecraft.network.chat.Component.translatable("tooltip.nexium.captured_mob_shift")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.nexium.captured_mob_release")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        });

        // What other players wear in their wings slot, so their elytra can be drawn
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.WingsSync.TYPE, (payload, context) -> {
            if (context.client().level == null) return;
            Entity entity = context.client().level.getEntity(payload.entityId());
            if (entity instanceof Player player && player != context.client().player) {
                ExtraSlots.of(player).setItem(ExtraSlots.WINGS, payload.wings());
            }
        });
    }

    private NexusEssentialsClient() {}
}
