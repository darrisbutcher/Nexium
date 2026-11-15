package net.storagenexus.essentials.compressed;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.storagenexus.essentials.NexusEssentials;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Compressed blocks: for each of sixteen bulk blocks three blocks that stand for 9, 81 and 729 of it. Nine of a block craft
 * into its Compressed block, nine Compressed blocks into a Double Compressed one, and nine of those into a Triple Compressed
 * one; each uncrafts back into nine of the one below. A Backpack's Compacting module packs them by itself, as the
 * crafting recipes are the ones it finds (see {@code BackpackCompacting}).
 *
 * <p>Each is mined like the block it stands for (same hardness, blast resistance, sound and tool), and drops itself.
 * Textures, models, recipes, loot tables and names are data files.
 */
public final class CompressedBlocks {
    /** The bulk blocks that have compressed versions. */
    public enum Base {
        STONE(Blocks.STONE, false), COBBLESTONE(Blocks.COBBLESTONE, false), DEEPSLATE(Blocks.DEEPSLATE, false),
        COBBLED_DEEPSLATE(Blocks.COBBLED_DEEPSLATE, false), TUFF(Blocks.TUFF, false), GRANITE(Blocks.GRANITE, false),
        DIORITE(Blocks.DIORITE, false), ANDESITE(Blocks.ANDESITE, false),
        NETHERRACK(Blocks.NETHERRACK, false), BLACKSTONE(Blocks.BLACKSTONE, false), BASALT(Blocks.BASALT, false), END_STONE(Blocks.END_STONE, false),
        DIRT(Blocks.DIRT, true), SAND(Blocks.SAND, true), RED_SAND(Blocks.RED_SAND, true), GRAVEL(Blocks.GRAVEL, true);

        private final Block block;
        private final boolean shovel;

        Base(Block block, boolean shovel) {
            this.block = block;
            this.shovel = shovel;
        }

        /** The vanilla block this stands for. */
        public Block block() {
            return block;
        }

        /** Whether a shovel is the tool for it (the rest take a pickaxe, which they need for drops). */
        public boolean shovel() {
            return shovel;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int LEVELS = 3;
    /** How many of the base block one block of each level stands for: 9, 81, 729. */
    public static final int[] STANDS_FOR = {9, 81, 729};
    private static final String[] PREFIXES = {"compressed_", "double_compressed_", "triple_compressed_"};

    private static final Map<Base, Block[]> BLOCKS = new EnumMap<>(Base.class);
    private static final Map<Base, Item[]> ITEMS = new EnumMap<>(Base.class);
    private static final List<Item> ALL_ITEMS = new ArrayList<>();

    static {
        for (Base base : Base.values()) {
            Block[] blocks = new Block[LEVELS];
            Item[] items = new Item[LEVELS];
            for (int level = 1; level <= LEVELS; level++) {
                String name = PREFIXES[level - 1] + base.id();
                ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, NexusEssentials.id(name));
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .mapColor(base.block.defaultMapColor())
                        .strength(base.block.defaultDestroyTime(), base.block.getExplosionResistance())
                        .sound(base.block.defaultBlockState().getSoundType());
                if (!base.shovel) props.requiresCorrectToolForDrops();
                Block block = new Block(props);
                Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, NexusEssentials.id(name));
                Item item = new CompressedBlockItem(block, base, level, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
                Registry.register(BuiltInRegistries.ITEM, itemKey, item);
                blocks[level - 1] = block;
                items[level - 1] = item;
                ALL_ITEMS.add(item);
            }
            BLOCKS.put(base, blocks);
            ITEMS.put(base, items);
        }
    }

    /** The block of this base at this level (1 to 3). */
    public static Block block(Base base, int level) {
        return BLOCKS.get(base)[level - 1];
    }

    /** The item of this base at this level (1 to 3). */
    public static Item item(Base base, int level) {
        return ITEMS.get(base)[level - 1];
    }

    /** All 48, in order: each base's three levels together. */
    public static List<Item> items() {
        return ALL_ITEMS;
    }

    public static void init() {}

    private CompressedBlocks() {}
}
