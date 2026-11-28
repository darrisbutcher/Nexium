package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;

/** With JEI installed (run with -PwithJei): JEI has a crafting recipe for every battery and cell, the upgrade ones that keep charge included. */
public class JeiRecipesClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        // Without JEI (the usual run) there is nothing to check, and JeiCheck's JEI classes must not be touched
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
            System.out.println("[nexium] JEI is not installed: skipping");
            return;
        }
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(40);
            List<String> want = List.of("iron_battery", "gold_battery", "diamond_battery", "nexium_battery", "iron_cell", "gold_cell", "diamond_cell", "nexium_cell");
            List<String> found = ctx.computeOnClient(mc -> JeiCheck.craftingRecipesFor(want));
            if (found == null) {
                System.out.println("[nexium] JEI is not installed: skipping");
                return;
            }
            System.out.println("[nexium] JEI crafting recipes found: " + found);
            for (String id : want) if (!found.contains(id)) throw new AssertionError("Expected: JEI lists the recipe " + id + ", found " + found);

            // JEI's "+" with the Pattern Encoder open fills it from a crafting recipe
            net.minecraft.core.BlockPos[] at = new net.minecraft.core.BlockPos[1];
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                at[0] = player.blockPosition().offset(0, 0, 2);
                player.level().setBlockAndUpdate(at[0], net.nexium.registry.ModBlocks.PATTERN_ENCODER.defaultBlockState());
                var pos = at[0];
                player.level().getBlockState(pos).useWithoutItem(player.level(), player, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
            });
            ctx.waitTicks(10);
            var stick = world.getServer().computeOnServer(server -> server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.withDefaultNamespace("stick"))).orElseThrow());
            String result = ctx.computeOnClient(mc -> JeiCheck.pressPlusOnEncoder(mc, stick));
            System.out.println("[nexium] JEI + on the Pattern Encoder: " + result);
            if (!result.equals("ok")) throw new AssertionError("Expected: JEI's + works on the Pattern Encoder, got " + result);
            ctx.waitTicks(10);
            boolean filled = world.getServer().computeOnServer(server -> {
                var encoder = (net.nexium.autocraft.PatternEncoderBlockEntity) server.getPlayerList().getPlayers().get(0).level().getBlockEntity(at[0]);
                return encoder.slots().getItem(net.nexium.autocraft.PatternEncoderBlockEntity.INPUTS).is(net.minecraft.world.item.Items.STICK);
            });
            if (!filled) throw new AssertionError("Expected: the encoder holds the sticks recipe after JEI's +");
            System.out.println("[nexium] encoder after JEI +: " + ctx.takeScreenshot("autocraft_encoder_jei"));
        }
    }
}
