package net.nexium.registry;

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
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.COMBUSTION_GENERATOR);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.COMBUSTION_GENERATOR);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.SOLAR_PANEL);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.THERMAL_GENERATOR);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.BATTERY);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.REDSTONE_REACTOR);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir), ModBlockEntities.REDSTONE_REACTOR);
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank(), ModBlockEntities.THERMAL_GENERATOR);
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.NEXUS_FURNACE);
        // Items go in from the top and sides (a stack shared out evenly over the eight slots) and out from the bottom
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> dir == net.minecraft.core.Direction.DOWN
                ? net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, dir) : be.inputs(), ModBlockEntities.NEXUS_FURNACE);
        // Nexus Energy: the machines that run on it (the farms register theirs with the Essentials)
        net.nexium.energy.NexusEnergy.SIDED.registerForBlockEntity((be, dir) -> be.energy(), ModBlockEntities.FREEZER);

        // Item conduits (and other lookups) reach the Freezer, the farms and the Iron Farm from any face, as the bottom: they give
        // their output out and take nothing in. Hoppers below, which use the container itself, are as before.
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, net.minecraft.core.Direction.DOWN), ModBlockEntities.FREEZER);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, net.minecraft.core.Direction.DOWN), net.nexium.essentials.registry.ModBlockEntities.FARM);
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be, net.minecraft.core.Direction.DOWN), net.nexium.essentials.registry.ModBlockEntities.IRON_FARM);
    }

    private ModStorageLookups() {}
}
