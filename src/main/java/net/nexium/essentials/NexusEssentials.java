package net.nexium.essentials;

import net.minecraft.resources.Identifier;
import net.nexium.StorageNexus;
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
 * The quality-of-life features that used to be the separate Nexus Essentials mod, set up by {@link StorageNexus}.
 * <p>
 * Everything here keeps the {@value #NAMESPACE} namespace it had as its own mod (item, block, effect, component and
 * tag ids, lang keys, {@code config/nexium.json} and the player's saved slots), so worlds made with
 * Nexus Essentials load unchanged. fabric.mod.json lists the old mod id under "provides".
 * <p>
 * New Essentials content uses this namespace too (register it with {@link #id}), so every Essentials id stays
 * {@code nexium:}; only the storage features use {@code nexium:}.
 */
public final class NexusEssentials {
    public static final String NAMESPACE = "nexium";
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

    private NexusEssentials() {}
}
