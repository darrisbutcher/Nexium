package net.storagenexus.essentials;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.storagenexus.essentials.config.NexusConfig;
import org.jspecify.annotations.Nullable;

/** Which explosions leave the world alone. Ghast fireballs are the only large fireballs in vanilla. */
public final class MobGriefing {
    /** Creeper and ghast-fireball explosions don't break blocks or start fires. */
    public static boolean keepsBlocks(@Nullable Entity explosionSource) {
        NexusConfig config = NexusConfig.get();
        if (explosionSource instanceof Creeper) return !config.creepersBreakBlocks;
        if (explosionSource instanceof LargeFireball) return !config.ghastFireballsBreakBlocks;
        return false;
    }

    /** ...and don't break item frames, paintings, armor stands, dropped items, minecarts or boats either. */
    public static boolean sparesDecorations(@Nullable Entity explosionSource) {
        return keepsBlocks(explosionSource) && NexusConfig.get().explosionsSpareDecorations;
    }

    public static boolean endermenTakeBlocks() {
        return NexusConfig.get().endermenPickUpBlocks;
    }

    private MobGriefing() {}
}
