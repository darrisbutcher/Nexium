package net.storagenexus.essentials.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.config.NexusConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Ancient debris drops netherite scrap instead of itself: 2 per block, multiplied by Fortune with diamond ore's
 * formula, so Fortune III averages 4.4 and tops out at 8. Silk Touch still gives the block.
 * Only replaces vanilla's own table, so a data pack that changes it wins.
 */
public final class AncientDebrisLoot {
    private static final String TABLE = "/nexusessentials/ancient_debris_scrap.json";

    public static void init() {
        LootTableEvents.REPLACE.register((key, original, source, registries) -> {
            if (!source.isBuiltin() || !NexusConfig.get().ancientDebrisDropsScrap) return null;
            if (!Blocks.ANCIENT_DEBRIS.getLootTable().map(key::equals).orElse(false)) return null;
            return LootTable.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), readTable())
                    .resultOrPartial(error -> NexusEssentials.LOG.error("Bad ancient debris loot table: {}", error))
                    .orElse(null);
        });
    }

    private static JsonElement readTable() {
        try (InputStream in = AncientDebrisLoot.class.getResourceAsStream(TABLE)) {
            if (in == null) throw new IllegalStateException("Missing " + TABLE);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + TABLE, e);
        }
    }

    private AncientDebrisLoot() {}
}
