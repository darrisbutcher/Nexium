package net.storagenexus.essentials.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<IronFarmBlockEntity> IRON_FARM = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            NexusEssentials.id("iron_farm"), new BlockEntityType<>(IronFarmBlockEntity::new, Set.of(ModBlocks.IRON_FARM)));

    /** One type for every farm kind (the kind comes from the block). */
    public static final BlockEntityType<FarmBlockEntity> FARM = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            NexusEssentials.id("farm"), new BlockEntityType<>(FarmBlockEntity::new, new java.util.HashSet<>(ModBlocks.FARMS.values())));

    public static final BlockEntityType<FarmAssemblerBlockEntity> FARM_ASSEMBLER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            NexusEssentials.id("farm_assembler"), new BlockEntityType<>(FarmAssemblerBlockEntity::new, Set.of(ModBlocks.FARM_ASSEMBLER)));

    public static void init() {
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((be, side) -> be.storage(), FARM_ASSEMBLER);
    }

    private ModBlockEntities() {}
}
