package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.nexium.registry.ModBlocks;

import java.util.List;

/** Reinforced Glass: tinted glass that blocks light, drops itself, and that the Wither, the Ender Dragon and explosions can't break. */
public class ReinforcedGlassGameTest {
    @GameTest
    public void itBlocksLightLikeTintedGlass(GameTestHelper h) {
        BlockState ours = ModBlocks.REINFORCED_GLASS.defaultBlockState();
        BlockState tinted = Blocks.TINTED_GLASS.defaultBlockState();
        h.assertTrue(ours.getLightDampening() == tinted.getLightDampening() && ours.getLightDampening() == 15,
                "it blocks light completely, dampening " + ours.getLightDampening());
        h.assertTrue(!ours.propagatesSkylightDown(), "skylight does not pass through it");
        h.succeed();
    }

    @GameTest
    public void theWitherAndTheDragonCannotBreakIt(GameTestHelper h) {
        BlockState ours = ModBlocks.REINFORCED_GLASS.defaultBlockState();
        h.assertTrue(ours.is(BlockTags.DRAGON_IMMUNE), "the Ender Dragon cannot break it");
        h.assertTrue(ours.is(BlockTags.WITHER_IMMUNE), "it is tagged wither immune");
        h.assertTrue(!WitherBoss.canDestroy(ours), "the Wither does not destroy it");
        h.assertTrue(WitherBoss.canDestroy(Blocks.GLASS.defaultBlockState()), "while plain glass it does destroy");
        h.succeed();
    }

    @GameTest
    public void anExplosionLeavesItStanding(GameTestHelper h) {
        BlockPos ours = new BlockPos(2, 2, 2);
        BlockPos plain = new BlockPos(4, 2, 2);
        h.setBlock(ours, ModBlocks.REINFORCED_GLASS);
        h.setBlock(plain, Blocks.GLASS);
        Vec3 centre = h.absoluteVec(new Vec3(3.5, 2.5, 2.5));
        h.getLevel().explode(null, centre.x, centre.y, centre.z, 4.0F, Level.ExplosionInteraction.TNT);
        h.assertTrue(h.getBlockState(ours).is(ModBlocks.REINFORCED_GLASS), "the Reinforced Glass survived the blast");
        h.assertTrue(h.getBlockState(plain).isAir(), "the control: plain glass beside it was blown away");
        h.succeed();
    }

    @GameTest
    public void itDropsItselfWithAnyTool(GameTestHelper h) {
        List<ItemStack> drops = Block.getDrops(ModBlocks.REINFORCED_GLASS.defaultBlockState(), h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), null);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(ModBlocks.REINFORCED_GLASS.asItem()), "it drops one Reinforced Glass, got " + drops);
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void itIsCraftedFromGlassAroundObsidian(GameTestHelper h) {
        RecipeHolder<?> holder = h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("nexium:reinforced_glass"))).orElse(null);
        h.assertTrue(holder != null && holder.value() instanceof ShapedRecipe, "the recipe is loaded");
        ShapedRecipe recipe = (ShapedRecipe) holder.value();
        ItemStack glass = new ItemStack(Items.GLASS);
        ItemStack obsidian = new ItemStack(Items.OBSIDIAN);
        CraftingInput ring = CraftingInput.of(3, 3, List.of(glass, glass, glass, glass, obsidian, glass, glass, glass, glass));
        h.assertTrue(recipe.matches(ring, h.getLevel()), "8 glass around an obsidian block match");
        ItemStack result = recipe.assemble(ring);
        h.assertTrue(result.is(ModBlocks.REINFORCED_GLASS.asItem()) && result.getCount() == 8, "it makes 8 Reinforced Glass, got " + result);
        CraftingInput wrong = CraftingInput.of(3, 3, List.of(glass, glass, glass, glass, glass, glass, glass, glass, glass));
        h.assertTrue(!recipe.matches(wrong, h.getLevel()), "9 glass do not");
        h.succeed();
    }
}
