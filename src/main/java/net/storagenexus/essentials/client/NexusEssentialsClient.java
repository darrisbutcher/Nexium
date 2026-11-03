package net.storagenexus.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

    public static void init() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_MAGNET);
        MenuScreens.register(ModMenus.IRON_FARM, IronFarmScreen::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_MAGNET.consumeClick()) {
                if (client.player != null) ClientPlayNetworking.send(ModPayloads.ToggleMagnet.INSTANCE);
            }
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
