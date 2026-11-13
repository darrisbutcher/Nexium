package net.storagenexus.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.storagenexus.fluid.FluidDrawerBlockEntity;

/**
 * The Honey fluid is gone (nothing has made it since 1.20.1, and it showed up in recipe viewers): a drawer that still
 * holds a fluid which no longer exists loads that slot empty instead of failing.
 */
public class RetiredFluidGameTest {
    @GameTest
    public void honeyIsNoLongerRegisteredAndALeftoverFluidLoadsAsAnEmptySlot(GameTestHelper h) {
        h.assertTrue(!BuiltInRegistries.FLUID.containsKey(Identifier.fromNamespaceAndPath("nexusessentials", "honey")), "no Honey fluid");
        var data = new FluidDrawerBlockEntity.SlotData(FluidVariant.of(Fluids.LAVA), 81_000L, FluidVariant.of(Fluids.LAVA));
        JsonElement json = FluidDrawerBlockEntity.SlotData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        String retired = json.toString().replace("minecraft:lava", "nexusessentials:honey");
        JsonElement parsed = com.google.gson.JsonParser.parseString(retired);
        var loaded = FluidDrawerBlockEntity.SlotData.CODEC.parse(JsonOps.INSTANCE, parsed).getOrThrow();
        h.assertTrue(loaded.variant().isBlank(), "the missing fluid loads as an empty slot");
        h.assertTrue(loaded.locked().isBlank(), "and an empty lock");
        // Lava still round-trips
        var back = FluidDrawerBlockEntity.SlotData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(back.variant().isOf(Fluids.LAVA) && back.amount() == 81_000L, "a real fluid still loads");
        h.succeed();
    }
}
