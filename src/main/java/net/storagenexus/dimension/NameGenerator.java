package net.storagenexus.dimension;

import net.minecraft.util.RandomSource;

import java.util.function.Predicate;

/** Random two-word link names like "Crimson Falcon" or "Quiet Basalt". */
public final class NameGenerator {
    private static final String[] ADJECTIVES = {
            "Crimson", "Quiet", "Amber", "Silent", "Golden", "Hollow", "Ancient", "Swift", "Frozen", "Molten",
            "Velvet", "Iron", "Emerald", "Azure", "Obsidian", "Rusty", "Gilded", "Wandering", "Shadow", "Bright",
            "Mossy", "Copper", "Lunar", "Solar", "Ashen", "Coral", "Misty", "Stormy", "Humble", "Brave",
            "Warped", "Echoing", "Deep", "Pale", "Clever", "Lucky", "Hidden", "Wild", "Calm", "Distant"
    };

    private static final String[] NOUNS = {
            "Falcon", "Basalt", "Otter", "Lantern", "Beacon", "Golem", "Comet", "Glacier", "Ember", "Raven",
            "Quartz", "Willow", "Badger", "Harbor", "Anvil", "Fox", "Spire", "Meadow", "Cinder", "Tide",
            "Axolotl", "Barrel", "Strider", "Shulker", "Lodestone", "Nautilus", "Sniffer", "Allay", "Warden", "Breeze",
            "Compass", "Prism", "Canyon", "Mantle", "Bramble", "Orchid", "Pebble", "Thistle", "Heron", "Lynx"
    };

    public static String generate(RandomSource random, Predicate<String> taken) {
        for (int attempt = 0; attempt < 200; attempt++) {
            String name = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " + NOUNS[random.nextInt(NOUNS.length)];
            if (!taken.test(name)) return name;
        }
        // 1600 combinations used up: add a number.
        int n = 2;
        while (true) {
            String name = ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " " + NOUNS[random.nextInt(NOUNS.length)] + " " + n++;
            if (!taken.test(name)) return name;
        }
    }

    private NameGenerator() {}
}
