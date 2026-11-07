package net.storagenexus.essentials.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.assembler.FarmAssemblerBlock;
import net.storagenexus.essentials.farm.FarmBlock;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.ironfarm.IronFarmBlock;

import java.util.EnumMap;
import java.util.Map;

import java.util.function.Function;

public final class ModBlocks {
    /** Makes the one-block farms and the Master Librarian. */
    public static final FarmAssemblerBlock FARM_ASSEMBLER = register("farm_assembler", FarmAssemblerBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /**
     * Made at the Farm Assembler. A pickaxe mines it faster, but it drops with any tool (or none),
     * because losing one would cost all its materials.
     */
    public static final IronFarmBlock IRON_FARM = register("iron_farm", IronFarmBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL));

    /** The other one-block farms, made at the Farm Assembler like the Iron Farm. */
    public static final Map<FarmKind, FarmBlock> FARMS = new EnumMap<>(FarmKind.class);

    static {
        for (FarmKind kind : FarmKind.values()) {
            FARMS.put(kind, register(kind.id(), p -> new FarmBlock(kind, p), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL)));
        }
    }

    private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, NexusEssentials.id(name));
        T block = factory.apply(props.setId(key));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        return block;
    }

    public static void init() {}

    private ModBlocks() {}
}
