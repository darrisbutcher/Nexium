package net.nexium.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.essentials.NexiumEssentials;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings in {@code config/nexium.json}. The file is created with the defaults on first launch
 * and rewritten on every launch so new settings show up. Changes take effect after a restart.
 */
public final class NexusConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 4;
    private static NexusConfig instance = new NexusConfig();

    // --- Mob griefing
    /** Creeper explosions break blocks (vanilla behaviour). */
    public boolean creepersBreakBlocks = false;
    /** Ghast fireballs break blocks and start fires (vanilla behaviour). */
    public boolean ghastFireballsBreakBlocks = false;
    /** Creeper and ghast explosions leave item frames, paintings, armor stands, dropped items, minecarts and boats alone. */
    public boolean explosionsSpareDecorations = true;
    /** Endermen pick up blocks (vanilla behaviour). */
    public boolean endermenPickUpBlocks = false;

    // --- Magnet
    /** Largest range a magnet can be set to, in blocks. Sneak + right-click cycles 4, 8, 12 ... up to this. */
    public int magnetMaxRange = 16;
    /** Magnets also pull experience orbs. */
    public boolean magnetPullsXp = true;

    // --- Elytra
    /** Elytra accept Protection, Fire Protection, Blast Protection and Projectile Protection, all at once. */
    public boolean elytraProtectionEnchantments = true;

    // --- Piglins
    /** Wearing any piece of netherite armor keeps piglins calm, like gold armor. */
    public boolean netheriteArmorCalmsPiglins = true;

    // --- Bedrock
    /** How long the Elixir of Bedrock Breaking lasts, in seconds. */
    public int bedrockBreakerSeconds = 300;

    // --- Mob pick-up
    /** Sneak + right-click a mob with an empty hand to pick it up as an item. */
    public boolean mobPickUp = true;
    /** Only the owner of a tamed mob may pick it up. */
    public boolean onlyOwnersPickUpPets = true;
    /** Experience levels it costs to pick up a boss mob (the Warden, the Elder Guardian). 0 makes them free. */
    public int bossPickUpLevels = 10;

    // --- Ancient debris
    /** Ancient debris drops netherite scrap: 2 per block, multiplied by Fortune like diamond ore. Silk Touch still gives the block. */
    public boolean ancientDebrisDropsScrap = true;

    // --- Crops
    /** Right-click a fully grown crop to harvest it and replant it; with the vein mining key held, the whole connected patch. */
    public boolean rightClickHarvest = true;
    /** Most crops one vein harvest takes. */
    public int harvestVeinMax = 128;

    // --- Anvil
    /** An anvil never says "Too Expensive!": any job can be done, for its full cost in levels. False keeps vanilla's limit of 40. */
    public boolean anvilNoCostLimit = true;

    // --- Master Librarian
    /** How many villagers (spawn eggs or Captured Mobs) the Farm Assembler takes to make a Master Librarian. */
    public int librarianVillagers = 12;

    // --- Iron farm
    /** Ticks between golems at normal speed. 150 (7.5 seconds) is about 1,900 iron an hour, like four 3-villager farms. */
    public int ironFarmGolemTicks = 150;

    // --- Other one-block farms (made at the Farm Assembler, like the Iron Farm)
    /** Ticks between baby villagers (needs 3 bread, or 12 carrots, potatoes or beetroot, each). 12000 is 10 minutes. */
    public int villagerBreederTicks = 12000;
    /** Ticks per barter (1 gold ingot each), like a real piglin's 6 seconds. */
    public int piglinBarterTicks = 120;
    /** Ticks between kills: gold nuggets and the rare gold ingot, as if Looting V (no player needed). */
    public int zombifiedPiglinTicks = 80;
    /** Ticks between kills: coal, bones and the wither skeleton skull (7.5%, Looting V odds). */
    public int witherSkeletonTicks = 120;
    /** Ticks between kills: a shulker shell about 4 times in 5 (Looting V odds). */
    public int shulkerFarmTicks = 600;
    /** Ticks between kills: a witch's drops, as if a player killed her (no Looting). */
    public int witchFarmTicks = 200;
    /** Ticks between harvests: 3 sugar cane. */
    public int sugarCaneFarmTicks = 400;
    /** Ticks between harvests: 4 bamboo. */
    public int bambooFarmTicks = 400;
    /** Ticks between harvests: 4 nether wart. */
    public int netherWartFarmTicks = 600;
    /** Ticks between kills: the loot of a zombie, skeleton, spider, creeper, pillager, vindicator and evoker, as if a player killed them. */
    public int monsterFarmTicks = 200;
    /** Ticks between kills: a blaze, a ghast and a magma cube. */
    public int netherMobFarmTicks = 200;
    /** Ticks between kills: a guardian, a drowned and a cod. */
    public int oceanFarmTicks = 200;
    /** Ticks between kills: a small slime. */
    public int slimeFarmTicks = 200;
    /** Ticks per cycle: the drops of the animals its feed brings (1 feed item a cycle). */
    public int livestockFarmTicks = 400;
    /** Ticks per cycle: all four crops. Bone meal halves it. */
    public int cropFarmTicks = 400;
    /** Ticks per cycle: melons, pumpkins, cactus or kelp. */
    public int gourdFarmTicks = 400;
    /** Ticks per cycle of the Generator (obsidian takes twice as long). */
    public int generatorFarmTicks = 200;
    /** Ticks per cycle: honeycomb and 2 bottles of honey. */
    public int beeFarmTicks = 400;
    /** Ticks per cycle: moss blocks, carpet and a little azalea (a bone meal lasts 3 cycles). */
    public int mossFarmTicks = 400;
    /** Ticks per cycle: one ochre, one verdant and one pearlescent froglight. */
    public int froglightFarmTicks = 400;

    /** Ticks per cycle: 8 logs of the chosen tree, or chorus fruit. */
    public int treeFarmTicks = 400;

    // --- Freezer
    /** Ticks the Freezer takes for 1 ice (1 bucket of water), 1 packed ice (9 buckets) and 1 blue ice (81 buckets). The water is used up steadily over that time. */
    public int freezerIceTicks = 100;
    public int freezerPackedIceTicks = 450;
    public int freezerBlueIceTicks = 900;

    // --- AIOT modules (server-side only)
    /** Vein mining works: with the Vein Mine key held, an AIOT mines a whole vein. Off, the key does nothing. No module is needed. */
    public boolean aiotVeinMining = true;
    /**
     * The extra blocks of a vein, 3x3 or tunnel swing drop their items where you broke the first block, so everything lands in one
     * place. Off, each block drops its own items where it was.
     */
    public boolean aiotDropsAtFirstBlock = true;
    /** Most blocks one vein mining swing mines, the first included. Players pick a smaller size per tool in the tool menu. */
    public int aiotVeinMaxBlocks = 256;
    /** Which version of this file wrote it, so a changed default can be carried over once. */
    public Integer configVersion;
    /**
     * Blocks vein mining leaves alone, for every tool: block ids ({@code minecraft:diamond_ore}) or block tags
     * ({@code #minecraft:logs}). Empty by default. The {@code #nexium:vein_excluded} tag (for data packs) is on top of this.
     */
    public List<String> veinBlacklist = new ArrayList<>();
    /** What {@link #connectorBlacklist} starts as: the vanilla machines, and Nexium's own ({@code #nexium:machines}). */
    private static List<String> defaultConnectorBlacklist() {
        return new ArrayList<>(List.of("minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand",
                "minecraft:hopper", "minecraft:dispenser", "minecraft:dropper", "minecraft:crafter", "#nexium:machines"));
    }
    /**
     * Blocks the Storage Connector and the Filtered Connector never connect to (block ids or {@code #tags}): a connector adds an
     * inventory to the network as storage, which suits chests and barrels, not machines with a different slot behind each face. Use an
     * Import/Export Bus to feed a machine. Starts with the vanilla machines and {@code #nexium:machines}; take an entry off to connect to it.
     */
    public List<String> connectorBlacklist = defaultConnectorBlacklist();
    /** The 3x3 Mining module works. */
    public boolean aiotAreaMining = true;
    /** Longest tunnel the 3x3 Mining module can dig, in 3x3 layers. Players pick a shorter one (2, 3, 8, 16, 24, 32) per tool, in the tool menu or by holding the tunnel key and scrolling. */
    public int aiotTunnelMaxLength = 32;
    /** Client side: outline the blocks the AIOT in your hand would mine along with the one you look at. */
    public boolean aiotMiningPreview = true;
    /** Client side: outline where the Construction Builder in your hand would put blocks. */
    public boolean builderPreview = true;
    /** Client side: outline the cell a diagonal placement (the Diagonal Placement key held) would put the block in. */
    public boolean diagonalPreview = true;
    /** Client side: show the AIOT's mode and module states in the corner of the screen while you hold it. */
    public boolean aiotHud = true;
    /** Client side: seconds the AIOT readout and mining outline stay up after you switch to an AIOT or press its 3x3, tunnel or vein key. 0 keeps them up. */
    public int aiotHudSeconds = 20;
    /** Client side: the Storage and Crafting Terminal's search and JEI's (or REI's) stay the same. */
    public boolean terminalSearchSync = true;
    /** The Nexus Link module works (drops go to the linked storage network). */
    public boolean aiotNexusLink = true;

    // --- Backpack (server-side only)
    /** A hotbar stack that is being used up is topped up from the Backpack's Restocking module when it gets down to this many. */
    public int restockTopUpAt = 8;

    public static NexusConfig get() {
        return instance;
    }

    /** Whether vein mining is told to leave this block alone: its id, or one of its tags, is on {@link #veinBlacklist}. */
    public boolean veinBlacklisted(BlockState state) {
        return listed(veinBlacklist, state);
    }

    /** Whether the storage connectors refuse to connect to this block: its id, or one of its tags, is on {@link #connectorBlacklist}. */
    public boolean connectorRefuses(BlockState state) {
        return listed(connectorBlacklist, state);
    }

    private static boolean listed(List<String> list, BlockState state) {
        if (list == null || list.isEmpty()) return false;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String entry : list) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                if (state.tags().anyMatch(tag -> entry.equals("#" + tag.location()))) return true;
            } else if (entry.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turns what a player typed into a blacklist entry: a block id ({@code diamond_ore} means {@code minecraft:diamond_ore}) of a block
     * that exists, or a {@code #tag}. Null if it is neither.
     */
    public static String blacklistEntry(String typed) {
        String text = typed == null ? "" : typed.trim().toLowerCase(java.util.Locale.ROOT);
        boolean tag = text.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? text.substring(1) : text);
        if (id == null) return null;
        if (tag) return "#" + id;
        return BuiltInRegistries.BLOCK.containsKey(id) ? id.toString() : null;
    }

    /** Carries a changed default over once for a file written by an older version (a fresh file already has the new defaults). */
    public static void migrate(NexusConfig instance, boolean fromFile) {
        // 1.6.0 raised the vein limit from 64 to 256: a file written before that, still on 64, moves up once
        if (fromFile && instance.configVersion == null && instance.aiotVeinMaxBlocks == 64) instance.aiotVeinMaxBlocks = 256;
        // 1.18.0 cut the Villager Breeder from 20 minutes to 10: a file still on the old 24000 moves down once
        if (fromFile && (instance.configVersion == null || instance.configVersion < 3) && instance.villagerBreederTicks == 24000) instance.villagerBreederTicks = 12000;
        // 1.22.1 halved the Freezer's packed ice (900 to 450) and blue ice (1800 to 900): a file still on the old numbers moves down once
        if (fromFile && (instance.configVersion == null || instance.configVersion < 4)) {
            if (instance.freezerPackedIceTicks == 900) instance.freezerPackedIceTicks = 450;
            if (instance.freezerBlueIceTicks == 1800) instance.freezerBlueIceTicks = 900;
        }
        instance.configVersion = VERSION;
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(NexiumEssentials.NAMESPACE + ".json");
    }

    public static void load() {
        instance = loadFrom(configFile());
    }

    /**
     * Reads the settings in {@code file}, tidies them (older versions' defaults carried over, numbers held to their limits) and writes
     * them back. A file that can't be read, say after a typo, is not thrown away: it is moved aside to {@code <name>.broken-<time>}
     * before the defaults are written in its place, so what the player had typed is still there to fix.
     */
    public static NexusConfig loadFrom(Path file) {
        NexusConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, NexusConfig.class);
            } catch (IOException | JsonParseException e) {
                NexiumEssentials.LOG.error("Could not read {}, using the defaults", file, e);
                setAside(file);
            }
        }
        NexusConfig config = loaded == null ? new NexusConfig() : loaded;
        migrate(config, loaded != null);
        config.clamp();
        write(file, config);
        return config;
    }

    /** Moves a settings file that could not be read out of the way, keeping it. */
    private static void setAside(Path file) {
        String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path aside = file.resolveSibling(file.getFileName() + ".broken-" + stamp);
        try {
            Files.move(file, aside, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            NexiumEssentials.LOG.error("The unreadable settings file was kept as {}; fix it there and copy it back, or delete it to keep the defaults", aside);
        } catch (IOException e) {
            NexiumEssentials.LOG.error("Could not keep a copy of the unreadable {}", file, e);
        }
    }

    /** Writes the current settings to the file. The settings screen calls this when a setting changes. */
    public static void save() {
        write(configFile(), instance);
    }

    private static void write(Path file, NexusConfig config) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            NexiumEssentials.LOG.warn("Could not write {}", file, e);
        }
    }

    private void clamp() {
        for (ConfigSchema.Entry e : ConfigSchema.ENTRIES) if (!e.isSwitch() && !e.isBlockList()) e.setInt(this, e.getInt(this));
    }
}
