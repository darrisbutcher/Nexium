package net.nexium.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.nexium.essentials.hostile.HostileMobSwitch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Hostile Mob Switch: each tick the chunk cache tells the natural spawner, and the custom spawners (phantoms, patrols), whether
 * enemies may spawn. Both read the same flag, so with the switch on they are told "no", as Peaceful does.
 */
@Mixin(ServerChunkCache.class)
abstract class ServerChunkCacheMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyExpressionValue(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerChunkCache;spawnEnemies:Z"))
    private boolean nexium$hostileMobSwitch(boolean spawnEnemies) {
        return HostileMobSwitch.spawnEnemies(level, spawnEnemies);
    }
}
