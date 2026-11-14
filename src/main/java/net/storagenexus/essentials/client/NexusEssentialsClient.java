package net.storagenexus.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.storagenexus.StorageNexus;
import net.storagenexus.essentials.registry.ModMenus;
import net.storagenexus.essentials.registry.ModPayloads;
import net.storagenexus.essentials.slots.ExtraSlots;

/** Client side of the Essentials features, set up by {@link net.storagenexus.client.StorageNexusClient}. */
public final class NexusEssentialsClient {
    /** Key binds are listed under Nexium. */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(StorageNexus.id("main"));
    /** Switches the magnet in the trinket slot on or off. M by default; change it under Controls > Key Binds. */
    public static final KeyMapping TOGGLE_MAGNET = new KeyMapping("key.nexusessentials.toggle_magnet",
            InputConstants.KEY_M, CATEGORY);

    /** Opens the Backpack worn in the backpack slot. B by default; change it under Controls > Key Binds. */
    public static final KeyMapping OPEN_BACKPACK = new KeyMapping("key.nexusessentials.open_backpack",
            InputConstants.KEY_B, CATEGORY);

    /** The name of a player in the world around you (the owner of a tamed mob), or null if they are not near. */
    private static @org.jspecify.annotations.Nullable String playerName(java.util.UUID id) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        Player player = level == null ? null : level.getPlayerByUUID(id);
        return player == null ? null : player.getName().getString();
    }

    public static void init() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_MAGNET);
        KeyMappingHelper.registerKeyMapping(OPEN_BACKPACK);
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
                else lines.add(net.minecraft.network.chat.Component.translatable("tooltip.nexusessentials.captured_mob_shift")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.nexusessentials.captured_mob_release")
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
