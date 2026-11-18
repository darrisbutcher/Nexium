package net.nexium.essentials.mixin;

import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the dog set its collar colour (a wolf's own setter is private). */
@Mixin(Wolf.class)
public interface WolfAccess {
    @Invoker("setCollarColor")
    void nexium$setCollarColor(DyeColor color);
}
