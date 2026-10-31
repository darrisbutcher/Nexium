package net.storagenexus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.storagenexus.dimension.AnchorRegistry;
import net.storagenexus.drawer.DrawerInteraction;
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
        ServerLifecycleEvents.SERVER_STARTED.register(server -> AnchorRegistry.get(server).reconcile(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> StorageNetwork.clearCache());
        LOG.info("Storage Nexus loaded");
    }
}
