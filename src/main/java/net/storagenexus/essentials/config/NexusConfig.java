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
    private static final int VERSION = 2;
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

    // --- Ancient debris
    /** Ancient debris drops netherite scrap: 2 per block, multiplied by Fortune like diamond ore. Silk Touch still gives the block. */
    public boolean ancientDebrisDropsScrap = true;

    // --- Iron farm
    /** The name (set in an anvil) a double chest needs to turn into an Iron Farm. Upper and lower case don't matter. */
    public String ironFarmChestName = "Nexus Iron Farm";
    /** Ticks between golems at normal speed. 150 (7.5 seconds) is about 1,900 iron an hour, like four 3-villager farms. */
    public int ironFarmGolemTicks = 150;
    /** How many times faster the farm works while it has bread. */
    public int ironFarmBreadSpeedUp = 10;
    /** Seconds of boost one bread gives. */
    public int ironFarmBreadSeconds = 30;
    /** After the bread runs out, the bread slot is locked for this many times as long as the boost lasted. */
    public int ironFarmCooldownMultiplier = 10;
    /** Golems drop 0-2 poppies as well as 3-5 iron, like real ones. */
    public boolean ironFarmPoppies = true;

    // --- AIOT modules (server-side only)
    /** The Vein Mining module works. Off, installed modules do nothing. */
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
    /** Client side: show the AIOT's mode and module states in the corner of the screen while you hold it. */
    public boolean aiotHud = true;
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
        if (ironFarmChestName == null || ironFarmChestName.isBlank()) ironFarmChestName = "Nexus Iron Farm";
        ironFarmGolemTicks = Math.max(1, Math.min(24000, ironFarmGolemTicks));
        ironFarmBreadSpeedUp = Math.max(1, Math.min(100, ironFarmBreadSpeedUp));
        ironFarmBreadSeconds = Math.max(1, Math.min(60 * 60, ironFarmBreadSeconds));
        ironFarmCooldownMultiplier = Math.max(0, Math.min(100, ironFarmCooldownMultiplier));
        aiotVeinMaxBlocks = Math.max(1, Math.min(512, aiotVeinMaxBlocks));
        aiotTunnelMaxLength = Math.max(1, Math.min(64, aiotTunnelMaxLength));
        restockTopUpAt = Math.max(1, Math.min(63, restockTopUpAt));
        restockEatWhenMissing = Math.max(1, Math.min(20, restockEatWhenMissing));
    }
}
