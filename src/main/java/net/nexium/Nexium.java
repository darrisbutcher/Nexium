package net.nexium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.nexium.aiot.module.AiotMining;
import net.nexium.aiot.module.NexusLink;
import net.nexium.dimension.AnchorRegistry;
import net.nexium.drawer.DrawerInteraction;
import net.nexium.essentials.NexiumEssentials;
import net.nexium.network.ModPayloads;
import net.nexium.network.StorageNetwork;
import net.nexium.nexium.NexiumArmor;
import net.nexium.nexium.NexiumHaste;
import net.nexium.nexium.NexusRestock;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;
import net.nexium.registry.ModMenus;
import net.nexium.registry.ModRecipes;
import net.nexium.registry.ModStorageLookups;
import net.nexium.registry.ModTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nexium implements ModInitializer {
    public static final String MOD_ID = "nexium";
    public static final Logger LOG = LoggerFactory.getLogger("Nexium");
    private static final String GUIDE_GIVEN_TAG = "nexium.guide_given";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();
        ModRecipes.init();
        ModBlockEntities.init();
        ModMenus.init();
        ModTab.init();
        ModStorageLookups.init();
        ModPayloads.init();
        net.nexium.quest.Quests.init();
        DrawerInteraction.init();
        AnchorRegistry.init();
        NexiumHaste.init();
        NexiumArmor.init();
        NexusRestock.init();
        NexusLink.init();
        AiotMining.init();
        net.nexium.builder.BuilderService.init();
        // The quality-of-life features that used to be the Essentials mod
        NexiumEssentials.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> AnchorRegistry.get(server).reconcile(server));
        // Hand every player the guide once, the first time they join a world.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player.addTag(GUIDE_GIVEN_TAG)) DrawerInteraction.give(player, new ItemStack(ModItems.GUIDE));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> StorageNetwork.clearCache());
        LOG.info("Nexium loaded");
    }
}
