package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitItem;
import net.nexium.conduit.ConduitType;
import net.nexium.conduit.ConduitUpgradeItem;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Conduit is one item that carries items, fluids and energy; the Advanced Conduit Upgrade makes it Tier 2; the six old conduit items
 * convert to them when carried; breaking a conduit gives one Conduit (and an upgrade for Tier 2).
 */
public class ConduitUnifiedGameTest {
    private static ItemStack craft(GameTestHelper h, String pattern, Map<Character, Item> key) {
        List<ItemStack> grid = new ArrayList<>();
        for (char c : pattern.toCharArray()) grid.add(c == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(c)));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        var recipe = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        return recipe.isPresent() ? recipe.get().value().assemble(input) : ItemStack.EMPTY;
    }

    @GameTest
    public void theRecipesMakeEightConduitsAndFourUpgrades(GameTestHelper h) {
        var key = Map.<Character, Item>of('N', Items.IRON_NUGGET, 'C', Items.COPPER_INGOT, 'R', Items.REDSTONE, 'H', Items.HOPPER,
                'B', Items.BUCKET, 'G', Items.GLASS);
        ItemStack conduits = craft(h, "NCNRHBNGN", key);
        h.assertTrue(conduits.is(ModItems.CONDUIT) && conduits.getCount() == 8, "the Conduit makes 8: " + conduits);
        var up = Map.<Character, Item>of('R', Items.REDSTONE, 'I', Items.IRON_INGOT, 'P', Items.ENDER_PEARL);
        ItemStack upgrades = craft(h, "RIRIPIRIR", up);
        h.assertTrue(upgrades.is(ModItems.ADVANCED_CONDUIT_UPGRADE) && upgrades.getCount() == 4, "the Advanced Conduit Upgrade makes 4: " + upgrades);
        var old = Map.<Character, Item>of('N', Items.IRON_NUGGET, 'G', Items.GLASS, 'H', Items.HOPPER);
        h.assertTrue(craft(h, "NNNGHGNNN", old).isEmpty(), "the old Item Conduit recipe is gone");
        h.succeed();
    }

    @GameTest
    public void aConduitCarriesAllThreeAndAnUpgradeMakesItTierTwo(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        h.assertTrue(ConduitItem.complete(be), "a new conduit gains its types");
        for (ConduitType t : ConduitType.VALUES) h.assertValueEqual(be.tier(t), 1, t + " at Tier 1");
        h.assertFalse(ConduitItem.complete(be), "a complete one has nothing to add");
        h.assertTrue(ConduitUpgradeItem.upgrade(be), "an upgrade raises it");
        for (ConduitType t : ConduitType.VALUES) h.assertValueEqual(be.tier(t), 2, t + " at Tier 2");
        h.assertFalse(ConduitUpgradeItem.upgrade(be), "and a second one does nothing");
        h.succeed();
    }

    @GameTest
    public void aConduitFromBeforeGetsOnlyWhatItLacks(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        be.addConduit(ConduitType.ITEM, 2);
        ConduitItem.complete(be);
        h.assertValueEqual(be.tier(ConduitType.ITEM), 2, "the Tier 2 item conduit keeps its tier");
        h.assertValueEqual(be.tier(ConduitType.FLUID), 1, "fluid is added");
        h.assertValueEqual(be.tier(ConduitType.ENERGY), 1, "and energy");
        h.succeed();
    }

    @GameTest
    public void oldConduitItemsTurnIntoTheNewOnes(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        ItemStack ender = new ItemStack(ModItems.ENDER_FLUID_CONDUIT, 3);
        player.getInventory().setItem(0, ender);
        ender.getItem().inventoryTick(ender, h.getLevel(), player, EquipmentSlot.MAINHAND);
        int conduits = 0, upgrades = 0, old = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(ModItems.CONDUIT)) conduits += s.getCount();
            else if (s.is(ModItems.ADVANCED_CONDUIT_UPGRADE)) upgrades += s.getCount();
            else if (s.is(ModItems.ENDER_FLUID_CONDUIT)) old += s.getCount();
        }
        h.assertValueEqual(conduits, 3, "three Conduits");
        h.assertValueEqual(upgrades, 3, "and three Advanced Conduit Upgrades for the Tier 2 ones");
        h.assertValueEqual(old, 0, "and none of the old item left");

        ItemStack plain = new ItemStack(ModItems.ITEM_CONDUIT, 2);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, plain);
        plain.getItem().inventoryTick(plain, h.getLevel(), player, EquipmentSlot.MAINHAND);
        int gotConduits = 0, gotUpgrades = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(ModItems.CONDUIT)) gotConduits += s.getCount();
            if (s.is(ModItems.ADVANCED_CONDUIT_UPGRADE)) gotUpgrades += s.getCount();
        }
        h.assertValueEqual(gotConduits, 2, "a Tier 1 old conduit gives a Conduit");
        h.assertValueEqual(gotUpgrades, 0, "and no upgrade");
        h.succeed();
    }

    @GameTest
    public void breakingAConduitGivesOneConduitAndAnUpgradeForTierTwo(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModBlocks.CONDUIT_BUNDLE);
        ConduitBundleBlockEntity be = h.getBlockEntity(pos, ConduitBundleBlockEntity.class);
        ConduitItem.complete(be);
        ConduitUpgradeItem.upgrade(be);
        BlockPos abs = h.absolutePos(pos);
        h.getLevel().destroyBlock(abs, false);
        int conduits = 0, upgrades = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2))) {
            if (e.getItem().is(ModItems.CONDUIT)) conduits += e.getItem().getCount();
            if (e.getItem().is(ModItems.ADVANCED_CONDUIT_UPGRADE)) upgrades += e.getItem().getCount();
        }
        h.assertValueEqual(conduits, 1, "one Conduit drops, not three");
        h.assertValueEqual(upgrades, 1, "and one Advanced Conduit Upgrade");
        h.succeed();
    }
}
