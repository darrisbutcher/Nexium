package net.nexium.essentials.client;

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
import net.nexium.Nexium;
import net.nexium.essentials.placement.DiagonalPlacePayload;
import net.nexium.essentials.registry.ModMenus;
import net.nexium.essentials.registry.ModPayloads;
import net.nexium.essentials.slots.ExtraSlots;

/** Client side of the Essentials features, set up by {@link net.nexium.client.NexiumClient}. */
public final class NexiumEssentialsClient {
    /** Key binds are listed under Nexium. */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Nexium.id("main"));
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

    /** Opens the screen of the Jetpack you wear, to change its battery cell. J by default; change it under Controls > Key Binds. */
    public static final KeyMapping OPEN_JETPACK = new KeyMapping("key.nexium.open_jetpack",
            InputConstants.KEY_J, CATEGORY);

    /** Switches the worn Jetpack's engine off and on. Unbound by default; set it under Controls > Key Binds. */
    public static final KeyMapping JETPACK_ENGINE = new KeyMapping("key.nexium.jetpack_engine",
            InputConstants.UNKNOWN.getValue(), CATEGORY);

    /** Switches the worn Jetpack's hover off and on. Unbound by default; set it under Controls > Key Binds. */
    public static final KeyMapping JETPACK_HOVER = new KeyMapping("key.nexium.jetpack_hover",
            InputConstants.UNKNOWN.getValue(), CATEGORY);

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
        // The Jetpack on the back: its own model, drawn on the torso
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.nexium.client.jetpack.JetpackModel.LAYER,
                net.nexium.client.jetpack.JetpackModel::createLayer);
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?>) {
                @SuppressWarnings("unchecked")
                net.minecraft.client.renderer.entity.RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState,
                        net.minecraft.client.model.player.PlayerModel> parent = (net.minecraft.client.renderer.entity.RenderLayerParent<
                        net.minecraft.client.renderer.entity.state.AvatarRenderState, net.minecraft.client.model.player.PlayerModel>) renderer;
                helper.register(new net.nexium.client.jetpack.JetpackLayer(parent, context.getModelSet()));
            }
        });
        KeyMappingHelper.registerKeyMapping(TOGGLE_MAGNET);
        KeyMappingHelper.registerKeyMapping(OPEN_BACKPACK);
        KeyMappingHelper.registerKeyMapping(OPEN_JETPACK);
        KeyMappingHelper.registerKeyMapping(JETPACK_ENGINE);
        KeyMappingHelper.registerKeyMapping(JETPACK_HOVER);
        KeyMappingHelper.registerKeyMapping(DIAGONAL_PLACE);
        KeyMappingHelper.registerKeyMapping(CYCLE_TRADES);
        // The Cycle Trades key in a trading screen asks the server for new trades (it says no if the villager has been traded with)
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen)) return;
            net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((s, event) -> {
                if (CYCLE_TRADES.matches(event)) ClientPlayNetworking.send(net.nexium.essentials.trading.CycleTradesPayload.INSTANCE);
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
        MenuScreens.register(ModMenus.BACKPACK, net.nexium.client.screen.BackpackScreen::new);
        MenuScreens.register(ModMenus.IRON_FARM, IronFarmScreen::new);
        MenuScreens.register(ModMenus.XP_FARM, XpFarmScreen::new);
        MenuScreens.register(ModMenus.FARM, FarmScreen::new);
        MenuScreens.register(ModMenus.FARM_ASSEMBLER, FarmAssemblerScreen::new);
        MenuScreens.register(ModMenus.ENCHANTED_GRINDSTONE, EnchantedGrindstoneScreen::new);
        MenuScreens.register(ModMenus.TRASH_CAN, TrashCanScreen::new);
        MenuScreens.register(ModMenus.HOSTILE_MOB_SWITCH, HostileMobSwitchScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_MAGNET.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(ModPayloads.ToggleMagnet.INSTANCE);
            }
            while (JETPACK_ENGINE.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(new ModPayloads.JetpackSwitch(net.nexium.jetpack.JetpackFlight.ENGINE));
            }
            while (JETPACK_HOVER.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(new ModPayloads.JetpackSwitch(net.nexium.jetpack.JetpackFlight.HOVER_SWITCH));
            }
            while (OPEN_JETPACK.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(ModPayloads.OpenJetpack.INSTANCE);
            }
            while (OPEN_BACKPACK.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) ClientPlayNetworking.send(ModPayloads.OpenBackpack.INSTANCE);
            }
        });

        // A Captured Mob's age, owner, gear and status show while Shift is held (what it looks like is always shown), then
        // the hint on letting it out. It runs ahead of the other tooltip hooks, so those hints stay below it.
        net.minecraft.resources.Identifier early = Nexium.id("captured_mob_details");
        ItemTooltipCallback.EVENT.addPhaseOrdering(early, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        ItemTooltipCallback.EVENT.register(early, (stack, context, flag, lines) -> {
            net.nexium.essentials.capture.CapturedMob mob = stack.get(net.nexium.essentials.registry.ModComponents.CAPTURED_MOB);
            if (mob == null) return;
            var details = net.nexium.essentials.capture.CapturedMobDetails.details(mob, NexiumEssentialsClient::playerName);
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

    private NexiumEssentialsClient() {}
}
