package net.storagenexus.essentials;

import net.minecraft.resources.Identifier;
import net.storagenexus.StorageNexus;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.elytra.ElytraEnchanting;
import net.storagenexus.essentials.loot.AncientDebrisLoot;
import net.storagenexus.essentials.magnet.MagnetLogic;
import net.storagenexus.essentials.registry.ModBlockEntities;
import net.storagenexus.essentials.registry.ModBlocks;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModEffects;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.registry.ModMenus;
import net.storagenexus.essentials.registry.ModPayloads;
import net.storagenexus.essentials.slots.ExtraSlotEvents;
import org.slf4j.Logger;

/**
 * The quality-of-life features that used to be the separate Nexus Essentials mod, set up by {@link StorageNexus}.
 * <p>
 * Everything here keeps the {@value #NAMESPACE} namespace it had as its own mod (item, block, effect, component and
 * tag ids, lang keys, {@code config/nexusessentials.json} and the player's saved slots), so worlds made with
 * Nexus Essentials load unchanged. fabric.mod.json lists the old mod id under "provides".
 * <p>
 * New Essentials content uses this namespace too (register it with {@link #id}), so every Essentials id stays
 * {@code nexusessentials:}; only the storage features use {@code storagenexus:}.
 */
public final class NexusEssentials {
    public static final String NAMESPACE = "nexusessentials";
    public static final Logger LOG = StorageNexus.LOG;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static void init() {
        NexusConfig.load();
        ModComponents.init();
        ModEffects.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        // The farms run on Nexus Energy
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.FARM);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.IRON_FARM);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((farm, direction) -> farm.energy(), ModBlockEntities.XP_FARM);
        ModMenus.init();
        ModPayloads.init();
        ExtraSlotEvents.init();
        net.storagenexus.essentials.backpack.AutoTorch.init();
        MagnetLogic.init();
        MobCapture.init();
        ElytraEnchanting.init();
        AncientDebrisLoot.init();
        net.storagenexus.essentials.harvest.CropHarvest.init();
    }

    private NexusEssentials() {}
}
