package net.storagenexus.essentials.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.ironfarm.IronFarmBlockEntity;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<IronFarmBlockEntity> IRON_FARM = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            NexusEssentials.id("iron_farm"), new BlockEntityType<>(IronFarmBlockEntity::new, Set.of(ModBlocks.IRON_FARM)));

    public static void init() {}

    private ModBlockEntities() {}
}
