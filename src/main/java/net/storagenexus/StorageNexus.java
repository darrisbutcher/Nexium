package net.storagenexus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.storagenexus.dimension.AnchorRegistry;
import net.storagenexus.drawer.DrawerInteraction;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.network.ModPayloads;
import net.storagenexus.network.StorageNetwork;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.registry.ModStorageLookups;
import net.storagenexus.registry.ModTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageNexus implements ModInitializer {
    public static final String MOD_ID = "storagenexus";
    public static final Logger LOG = LoggerFactory.getLogger("Storage Nexus");
    private static final String GUIDE_GIVEN_TAG = "storagenexus.guide_given";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenus.init();
        ModTab.init();
        ModStorageLookups.init();
        ModPayloads.init();
        DrawerInteraction.init();
        AnchorRegistry.init();
        // The quality-of-life features that used to be the Nexus Essentials mod
        NexusEssentials.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> AnchorRegistry.get(server).reconcile(server));
        // Hand every player the guide once, the first time they join a world.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player.addTag(GUIDE_GIVEN_TAG)) DrawerInteraction.give(player, new ItemStack(ModItems.GUIDE));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> StorageNetwork.clearCache());
        LOG.info("Storage Nexus loaded");
    }
}
