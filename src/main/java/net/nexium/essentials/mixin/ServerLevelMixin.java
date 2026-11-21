package net.nexium.essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.nexium.essentials.MobGriefing;
import net.nexium.essentials.hostile.HostileMobSwitch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Creeper and ghast explosions keep blocks intact and don't light fires, but still hurt players and mobs. And the Hostile Mob Switch
 * turns off the spawns the spawn_monsters game rule governs (portal zombified piglins, reinforcement zombies, endermites, creakings).
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    private static final String EXPLODE = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;"
            + "Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;"
            + "Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;"
            + "Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V";

    @ModifyVariable(method = EXPLODE, at = @At("HEAD"), argsOnly = true)
    private Level.ExplosionInteraction nexium$keepBlocks(Level.ExplosionInteraction interaction, @Local(argsOnly = true) Entity source) {
        return MobGriefing.keepsBlocks(source) ? Level.ExplosionInteraction.NONE : interaction;
    }

    @ModifyVariable(method = EXPLODE, at = @At("HEAD"), argsOnly = true)
    private boolean nexium$noFire(boolean fire, @Local(argsOnly = true) Entity source) {
        return fire && !MobGriefing.keepsBlocks(source);
    }

    /** Keep the big explosion cloud: vanilla shows the small one for any explosion that breaks no blocks. */
    @WrapOperation(method = EXPLODE, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;isSmall()Z"))
    private boolean nexium$keepBigCloud(ServerExplosion explosion, Operation<Boolean> original, @Local(argsOnly = true) Entity source) {
        return MobGriefing.keepsBlocks(source) ? explosion.radius() < 2.0F : original.call(explosion);
    }

    @ModifyReturnValue(method = "isSpawningMonsters", at = @At("RETURN"))
    private boolean nexium$hostileMobSwitch(boolean spawning) {
        return HostileMobSwitch.spawnEnemies((ServerLevel) (Object) this, spawning);
    }
}
