package net.storagenexus.essentials.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.storagenexus.essentials.NexusEssentials;

public final class ModEffects {
    /** While active, bedrock can be mined with a diamond or netherite pickaxe. */
    public static final Holder<MobEffect> BEDROCK_BREAKER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            NexusEssentials.id("bedrock_breaker"), new BedrockBreakerEffect());

    public static void init() {}

    private static final class BedrockBreakerEffect extends MobEffect {
        BedrockBreakerEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x4A4A55);
        }
    }

    private ModEffects() {}
}
