package net.storagenexus.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.storagenexus.essentials.NexusEssentials;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings in {@code config/nexusessentials.json}. The file is created with the defaults on first launch
 * and rewritten on every launch so new settings show up. Changes take effect after a restart.
 */
public final class NexusConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 3;
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
    public int freezerPackedIceTicks = 900;
    public int freezerBlueIceTicks = 1800;

    // --- AIOT modules (server-side only)
    /** Vein mining works: with the Vein Mine key held, an AIOT mines a whole vein. Off, the key does nothing. No module is needed. */
    public boolean aiotVeinMining = true;
    /** Most blocks one vein mining swing mines, the first included. Players pick a smaller size per tool in the tool menu. */
    public int aiotVeinMaxBlocks = 256;
    /** Which version of this file wrote it, so a changed default can be carried over once. */
    public Integer configVersion;
    /** The 3x3 Mining module works. */
    public boolean aiotAreaMining = true;
    /** Longest tunnel the 3x3 Mining module can dig, in 3x3 layers. Players pick a shorter one (2, 4, 8, 16, 24, 32) per tool in the tool menu. */
    public int aiotTunnelMaxLength = 32;
    /** Client side: outline the blocks the AIOT in your hand would mine along with the one you look at. */
    public boolean aiotMiningPreview = true;
    /** Client side: outline where the Construction Builder in your hand would put blocks. */
    public boolean builderPreview = true;
    /** Client side: show the AIOT's mode and module states in the corner of the screen while you hold it. */
    public boolean aiotHud = true;
    /** Client side: seconds the AIOT readout and mining outline stay up after you switch to an AIOT or press its 3x3, tunnel or vein key. 0 keeps them up. */
    public int aiotHudSeconds = 20;
    /** Client side: the Storage and Crafting Terminal's search and JEI's (or REI's) stay the same. */
    public boolean terminalSearchSync = true;
    /** The Nexus Link module works (drops go to the linked storage network). */
    public boolean aiotNexusLink = true;

    // --- Nexium armor (server-side only)
    /** The Nexus restock set bonus works (top-ups and eating from the linked network). */
    public boolean nexusRestock = true;
    /** A hotbar stack that is being used up is topped up when it gets down to this many. */
    public int restockTopUpAt = 8;
    /** The wearer eats from the network when the hunger bar is down by this many points. */
    public int restockEatWhenMissing = 6;

    public static NexusConfig get() {
        return instance;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(NexusEssentials.NAMESPACE + ".json");
        NexusConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, NexusConfig.class);
            } catch (IOException | JsonParseException e) {
                NexusEssentials.LOG.error("Could not read {}, using the defaults", file, e);
            }
        }
        instance = loaded == null ? new NexusConfig() : loaded;
        // 1.6.0 raised the vein limit from 64 to 256: a file written before that, still on 64, moves up once
        if (loaded != null && instance.configVersion == null && instance.aiotVeinMaxBlocks == 64) instance.aiotVeinMaxBlocks = 256;
        // 1.18.0 cut the Villager Breeder from 20 minutes to 10: a file still on the old 24000 moves down once
        if (loaded != null && (instance.configVersion == null || instance.configVersion < 3) && instance.villagerBreederTicks == 24000) instance.villagerBreederTicks = 12000;
        instance.configVersion = VERSION;
        instance.clamp();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            NexusEssentials.LOG.warn("Could not write {}", file, e);
        }
    }

    private void clamp() {
        magnetMaxRange = Math.max(4, Math.min(64, magnetMaxRange));
        bedrockBreakerSeconds = Math.max(1, Math.min(24 * 60 * 60, bedrockBreakerSeconds));
        bossPickUpLevels = Math.max(0, Math.min(1000, bossPickUpLevels));
        librarianVillagers = Math.max(1, Math.min(54, librarianVillagers));
        ironFarmGolemTicks = Math.max(1, Math.min(24000, ironFarmGolemTicks));
        villagerBreederTicks = Math.max(1, Math.min(1_000_000, villagerBreederTicks));
        piglinBarterTicks = Math.max(1, Math.min(1_000_000, piglinBarterTicks));
        zombifiedPiglinTicks = Math.max(1, Math.min(1_000_000, zombifiedPiglinTicks));
        witherSkeletonTicks = Math.max(1, Math.min(1_000_000, witherSkeletonTicks));
        witchFarmTicks = Math.max(1, Math.min(1_000_000, witchFarmTicks));
        sugarCaneFarmTicks = Math.max(1, Math.min(1_000_000, sugarCaneFarmTicks));
        bambooFarmTicks = Math.max(1, Math.min(1_000_000, bambooFarmTicks));
        netherWartFarmTicks = Math.max(1, Math.min(1_000_000, netherWartFarmTicks));
        monsterFarmTicks = Math.max(1, Math.min(1_000_000, monsterFarmTicks));
        netherMobFarmTicks = Math.max(1, Math.min(1_000_000, netherMobFarmTicks));
        oceanFarmTicks = Math.max(1, Math.min(1_000_000, oceanFarmTicks));
        slimeFarmTicks = Math.max(1, Math.min(1_000_000, slimeFarmTicks));
        livestockFarmTicks = Math.max(1, Math.min(1_000_000, livestockFarmTicks));
        cropFarmTicks = Math.max(1, Math.min(1_000_000, cropFarmTicks));
        gourdFarmTicks = Math.max(1, Math.min(1_000_000, gourdFarmTicks));
        generatorFarmTicks = Math.max(1, Math.min(1_000_000, generatorFarmTicks));
        beeFarmTicks = Math.max(1, Math.min(1_000_000, beeFarmTicks));
        mossFarmTicks = Math.max(1, Math.min(1_000_000, mossFarmTicks));
        froglightFarmTicks = Math.max(1, Math.min(1_000_000, froglightFarmTicks));
        treeFarmTicks = Math.max(1, Math.min(1_000_000, treeFarmTicks));
        shulkerFarmTicks = Math.max(1, Math.min(1_000_000, shulkerFarmTicks));
        freezerIceTicks = Math.max(1, Math.min(1_000_000, freezerIceTicks));
        freezerPackedIceTicks = Math.max(1, Math.min(1_000_000, freezerPackedIceTicks));
        freezerBlueIceTicks = Math.max(1, Math.min(1_000_000, freezerBlueIceTicks));
        aiotVeinMaxBlocks = Math.max(1, Math.min(512, aiotVeinMaxBlocks));
        aiotHudSeconds = Math.max(0, Math.min(600, aiotHudSeconds));
        harvestVeinMax = Math.max(1, Math.min(1024, harvestVeinMax));
        aiotTunnelMaxLength = Math.max(1, Math.min(64, aiotTunnelMaxLength));
        restockTopUpAt = Math.max(1, Math.min(63, restockTopUpAt));
        restockEatWhenMissing = Math.max(1, Math.min(20, restockEatWhenMissing));
    }
}
