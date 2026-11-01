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
    }
}
