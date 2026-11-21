package net.nexium.essentials.hostile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.nexium.Nexium;
import net.nexium.essentials.ironfarm.IronFarmRecipe;

/**
 * The world's one Hostile Mob Switch setting: while it is on, hostile mobs do not spawn near any player, in any dimension. Every
 * Hostile Mob Switch block shows and changes this same setting, which is saved with the world (not with any block), so it does not
 * depend on where a switch stands or whether its chunk is loaded.
 * <p>
 * "Do not spawn" is exactly what vanilla's Peaceful difficulty does to spawning, without removing the mobs that are already there:
 * the periodic spawn cycle (including phantoms and patrols) skips hostile mobs, and the spawns the {@code spawn_monsters} game rule
 * governs (zombified piglins from a portal, reinforcement zombies, endermites from a pearl, creakings) are off. Mob spawners and
 * spawn eggs are not affected.
 */
public class HostileMobSwitch extends SavedData {
    /** How many hostile mobs (spawn eggs or Captured Mobs, of any kind) the Farm Assembler takes to make a switch. */
    public static final int MOBS_NEEDED = 140;

    private static final Codec<HostileMobSwitch> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("blocking", false).forGetter(s -> s.blocking)
    ).apply(i, HostileMobSwitch::new));

    public static final SavedDataType<HostileMobSwitch> TYPE =
            new SavedDataType<>(Nexium.id("hostile_mob_switch"), HostileMobSwitch::new, CODEC, null);

    private boolean blocking;

    public HostileMobSwitch() {}

    private HostileMobSwitch(boolean blocking) {
        this.blocking = blocking;
    }

    /** The setting for the whole world: kept with the overworld's data, like every other dimension-spanning mod data. */
    public static HostileMobSwitch get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Whether hostile spawning is switched off in this level's world. */
    public static boolean blocking(ServerLevel level) {
        return get(level.getServer()).blocking;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean on) {
        if (blocking == on) return;
        blocking = on;
        setDirty();
    }

    /** What the spawner is told about spawning enemies: vanilla's answer, unless the switch is on. */
    public static boolean spawnEnemies(ServerLevel level, boolean vanilla) {
        return vanilla && !blocking(level);
    }

    /** Whether this stack is a hostile mob (a spawn egg or a Captured Mob of any monster), which is what the Farm Assembler counts. */
    public static boolean isHostileMob(ItemStack stack) {
        EntityType<?> mob = IronFarmRecipe.mobOf(stack);
        return mob != null && mob.getCategory() == MobCategory.MONSTER;
    }
}
