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
    }

    private ModStorageLookups() {}
}
