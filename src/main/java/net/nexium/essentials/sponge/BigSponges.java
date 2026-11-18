package net.nexium.essentials.sponge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.nexium.essentials.NexusEssentials;

import java.util.ArrayList;
import java.util.List;

/**
 * Combined sponges: nine sponges craft a Large Sponge (reach 14), nine of those a Huge Sponge (reach 32), nine of those a Giant
 * Sponge (reach 64), and each crafts back into nine of the one below. They soak up water like a sponge, as much as the sponges
 * they stand for would (65 for each: 585, 5,265 and 47,385) and as far as their reach, then turn wet and dry in a furnace
 * (or in the Nether). Textures, models, recipes, loot tables and names are data files.
 */
public final class BigSponges {
    public enum Size {
        LARGE("large", 14, 9), HUGE("huge", 32, 81), GIANT("giant", 64, 729);

        private final String id;
        private final int reach;
        private final int standsFor;

        Size(String id, int reach, int standsFor) {
            this.id = id;
            this.reach = reach;
            this.standsFor = standsFor;
        }

        public String id() {
            return id;
        }

        /** How far, through the water, it takes water from: 14, 32 or 64 blocks (a plain sponge: 6). */
        public int reach() {
            return reach;
        }

        /** How many plain sponges it stands for: 9, 81 or 729. */
        public int standsFor() {
            return standsFor;
        }

        /** The most water blocks it takes in one go: 65 for each sponge it stands for, as a plain sponge's 65. */
        public int capacity() {
            return 65 * standsFor;
        }
    }

    private static final Block[] DRY = new Block[Size.values().length];
    private static final Block[] WET = new Block[Size.values().length];
    private static final Item[] DRY_ITEMS = new Item[Size.values().length];
    private static final Item[] WET_ITEMS = new Item[Size.values().length];
    private static final List<Item> ALL_ITEMS = new ArrayList<>();

    static {
        for (Size size : Size.values()) {
            int i = size.ordinal();
            String dryName = size.id() + "_sponge";
            String wetName = "wet_" + dryName;
            ResourceKey<Block> dryKey = ResourceKey.create(Registries.BLOCK, NexusEssentials.id(dryName));
            ResourceKey<Block> wetKey = ResourceKey.create(Registries.BLOCK, NexusEssentials.id(wetName));
            // The wet block has to be known to the dry one, and the dry one to the wet: each looks the other up when it is needed
            DRY[i] = new BigSpongeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPONGE).setId(dryKey), size.reach(), size.capacity(), () -> WET[size.ordinal()]);
            WET[i] = new BigWetSpongeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WET_SPONGE).setId(wetKey), () -> DRY[size.ordinal()]);
            Registry.register(BuiltInRegistries.BLOCK, dryKey, DRY[i]);
            Registry.register(BuiltInRegistries.BLOCK, wetKey, WET[i]);
            DRY_ITEMS[i] = item(dryName, DRY[i]);
            WET_ITEMS[i] = item(wetName, WET[i]);
            ALL_ITEMS.add(DRY_ITEMS[i]);
            ALL_ITEMS.add(WET_ITEMS[i]);
        }
    }

    private static Item item(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, NexusEssentials.id(name));
        Item item = new BigSpongeItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static Block block(Size size) {
        return DRY[size.ordinal()];
    }

    public static Block wetBlock(Size size) {
        return WET[size.ordinal()];
    }

    public static Item item(Size size) {
        return DRY_ITEMS[size.ordinal()];
    }

    public static Item wetItem(Size size) {
        return WET_ITEMS[size.ordinal()];
    }

    /** All six items, dry and wet, size by size. */
    public static List<Item> items() {
        return ALL_ITEMS;
    }

    public static void init() {}

    private BigSponges() {}
}
