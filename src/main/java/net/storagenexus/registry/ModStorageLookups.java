package net.storagenexus.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

/** Exposes our block entities to hoppers, pipes and other mods through the Fabric Transfer API. */
public final class ModStorageLookups {
    public static void init() {
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.TIERED_BARREL);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.getItemStorage(), ModBlockEntities.DRAWER);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.getItemStorage(), ModBlockEntities.COMPACTING_DRAWER);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.getItemStorage(), ModBlockEntities.DRAWER_CONTROLLER);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.getFluidStorage(), ModBlockEntities.DRAWER_CONTROLLER);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.getFluidStorage(), ModBlockEntities.FLUID_DRAWER);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.getFluidStorage(), ModBlockEntities.FLUID_INTAKE);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.getItemStorage(), ModBlockEntities.DIMENSIONAL_INPUT);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.getFluidStorage(), ModBlockEntities.DIMENSIONAL_INPUT);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank(), ModBlockEntities.FREEZER);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.COMBUSTION_GENERATOR);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.COMBUSTION_GENERATOR);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.SOLAR_PANEL);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.THERMAL_GENERATOR);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.BATTERY);
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.REDSTONE_REACTOR);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.REDSTONE_REACTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank(), ModBlockEntities.THERMAL_GENERATOR);
        // Nexus Energy: the machines that run on it (the farms register theirs with the Essentials)
        net.storagenexus.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.FREEZER);

        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.FREEZER);
    }

    private ModStorageLookups() {}
}
