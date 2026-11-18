package net.nexium.essentials.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.nexium.dog.DogEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creepers run from the dog (as they do from cats) and cannot blow up while one is near: their fuse never gets going and an
 * explosion already under way is called off.
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {
    @Shadow private int swell;

    /** How close a dog has to be, in blocks, to stop a creeper. */
    private static final double NEXIUM_DOG_RANGE = 8.0;

    protected CreeperMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract void setSwellDir(int dir);

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void nexium$avoidDogs(CallbackInfo ci) {
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, DogEntity.class, (float) NEXIUM_DOG_RANGE, 1.0, 1.2));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void nexium$noFuseNearADog(CallbackInfo ci) {
        if (!level().isClientSide() && nexium$dogNear()) {
            swell = 0;
            setSwellDir(-1);
        }
    }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void nexium$noBangNearADog(CallbackInfo ci) {
        if (nexium$dogNear()) {
            swell = 0;
            ci.cancel();
        }
    }

    private boolean nexium$dogNear() {
        return !level().getEntitiesOfClass(DogEntity.class, getBoundingBox().inflate(NEXIUM_DOG_RANGE)).isEmpty();
    }
}
