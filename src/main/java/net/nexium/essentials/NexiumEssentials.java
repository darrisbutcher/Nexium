package net.nexium.essentials;

import net.minecraft.resources.Identifier;
import net.nexium.Nexium;
import net.nexium.essentials.capture.MobCapture;
import net.nexium.essentials.config.NexusConfig;
import net.nexium.essentials.elytra.ElytraEnchanting;
import net.nexium.essentials.loot.AncientDebrisLoot;
import net.nexium.essentials.magnet.MagnetLogic;
import net.nexium.essentials.registry.ModBlockEntities;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModComponents;
import net.nexium.essentials.registry.ModEffects;
import net.nexium.essentials.registry.ModItems;
import net.nexium.essentials.registry.ModMenus;
import net.nexium.essentials.registry.ModPayloads;
import net.nexium.essentials.slots.ExtraSlotEvents;
import org.slf4j.Logger;

/**
 * The quality-of-life features (magnet, backpack, farms, mob pick-up and so on), set up by {@link Nexium}. They share the
 * {@value #NAMESPACE} namespace with the storage features, so every id, lang key and the config file
 * ({@code config/nexium.json}) is under it.
 */
public final class NexiumEssentials {
    public static final String NAMESPACE = "nexium";
    public static final Logger LOG = Nexium.LOG;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static void init() {
        NexusConfig.load();
        ModComponents.init();
        ModEffects.init();
        ModBlocks.init();
        ModItems.init();
        net.nexium.essentials.compressed.CompressedBlocks.init();
        net.nexium.essentials.sponge.BigSponges.init();
        ModBlockEntities.init();
        // The farms run on Nexus Energy
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.FARM);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.IRON_FARM);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.XP_FARM);
        ModMenus.init();
        ModPayloads.init();
        ExtraSlotEvents.init();
        net.nexium.essentials.backpack.AutoTorch.init();
        net.nexium.essentials.backpack.BackpackFeeding.init();
        net.nexium.essentials.backpack.BackpackCompacting.init();
        net.nexium.essentials.backpack.BackpackRestocking.init();
        MagnetLogic.init();
        MobCapture.init();
        ElytraEnchanting.init();
        AncientDebrisLoot.init();
        net.nexium.essentials.harvest.CropHarvest.init();
    }

    private NexiumEssentials() {}
}
