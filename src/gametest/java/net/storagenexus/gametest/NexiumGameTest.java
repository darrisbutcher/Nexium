package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.nexium.NexiumHaste;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Nexium: making it, its tools, spear and armor, the smithing upgrades, the Nexium AIOT and its Haste II. */
public class NexiumGameTest {
    /** A new stack each time: item stacks can't be made before the game has loaded item components. */
    private static ItemStack template() {
        return new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static ItemStack smith(GameTestHelper h, ItemStack base, ItemStack addition) {
        SmithingRecipeInput input = new SmithingRecipeInput(template(), base, addition);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static double attribute(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot) {
        double base = Player.createAttributes().build().getBaseValue(attribute);
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(attribute, base, slot) - base;
    }

    private static ServerPlayer playerHolding(GameTestHelper h, ItemStack stack) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    @GameTest
    public void ingotsBlocksAndTheirRecipes(GameTestHelper h) {
        ItemStack ingots = craft(h, List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD),
                new ItemStack(Items.NETHERITE_INGOT)));
        h.assertTrue(ingots.is(ModItems.NEXIUM_INGOT) && ingots.getCount() == 3,
                "diamond + emerald + netherite ingot = 3 Nexium ingots, got " + ingots);
        h.assertTrue(craft(h, List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD),
                new ItemStack(Items.NETHERITE_INGOT), template())).isEmpty(), "the upgrade template is no longer part of it");
        h.assertTrue(craft(h, List.of(new ItemStack(Items.DIAMOND_BLOCK), new ItemStack(Items.EMERALD_BLOCK),
                new ItemStack(Items.NETHERITE_BLOCK), template())).isEmpty(), "the old recipe with blocks no longer works");

        List<ItemStack> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(new ItemStack(ModItems.NEXIUM_INGOT));
        h.assertTrue(craft(h, nine).is(ModItems.NEXIUM_BLOCK), "nine ingots make a Block of Nexium");
        ItemStack back = craft(h, List.of(new ItemStack(ModItems.NEXIUM_BLOCK)));
        h.assertTrue(back.is(ModItems.NEXIUM_INGOT) && back.getCount() == 9, "and back to nine ingots");

        // Like a netherite block: a diamond pickaxe or better
        BlockState block = ModBlocks.NEXIUM_BLOCK.defaultBlockState();
        h.assertTrue(new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(block), "a diamond pickaxe mines a Block of Nexium");
        h.assertFalse(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(block), "an iron one doesn't");
        h.assertTrue(block.is(BlockTags.BEACON_BASE_BLOCKS), "it is a beacon base");
        h.assertTrue(new ItemStack(ModItems.NEXIUM_INGOT).is(ItemTags.BEACON_PAYMENT_ITEMS), "beacons take Nexium ingots");
        h.succeed();
    }

    @GameTest
    public void toolStats(GameTestHelper h) {
        Map<Item, Double> damage = Map.of(ModItems.NEXIUM_SWORD, 9.0, ModItems.NEXIUM_AXE, 11.0, ModItems.NEXIUM_PICKAXE, 7.0,
                ModItems.NEXIUM_SHOVEL, 7.5, ModItems.NEXIUM_SPEAR, 6.0);
        for (var e : damage.entrySet()) {
            double total = 1 + attribute(new ItemStack(e.getKey()), Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
            h.assertTrue(Math.abs(total - e.getValue()) < 1e-6, e.getKey() + " hits for " + e.getValue() + ", got " + total);
        }
        for (Item tool : List.of(ModItems.NEXIUM_SWORD, ModItems.NEXIUM_PICKAXE, ModItems.NEXIUM_AXE, ModItems.NEXIUM_SHOVEL,
                ModItems.NEXIUM_HOE, ModItems.NEXIUM_SPEAR)) {
            ItemStack stack = new ItemStack(tool);
            h.assertTrue(stack.getMaxDamage() == 2500, tool + " lasts 2,500 uses");
            h.assertTrue(stack.has(DataComponents.DAMAGE_RESISTANT), tool + " survives lava");
        }
        h.assertTrue(new ItemStack(ModItems.NEXIUM_SPEAR).has(DataComponents.KINETIC_WEAPON), "the spear charges like vanilla spears");
        h.assertTrue(new ItemStack(ModItems.NEXIUM_SPEAR).is(ItemTags.SPEARS), "it is a spear (Lunge etc.)");
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        h.assertTrue(new ItemStack(ModItems.NEXIUM_PICKAXE).isCorrectToolForDrops(obsidian), "it mines what netherite mines");
        h.assertTrue(new ItemStack(ModItems.NEXIUM_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState())
                == new ItemStack(Items.NETHERITE_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()), "at netherite's base speed");
        for (Item tool : List.of(ModItems.NEXIUM_PICKAXE, ModItems.NEXIUM_AXE, ModItems.NEXIUM_SHOVEL, ModItems.NEXIUM_HOE,
                ModItems.AIOTS.get(AiotTier.NEXIUM))) {
            h.assertTrue(NexiumHaste.grants(new ItemStack(tool)), tool + " gives Haste II");
        }
        h.assertFalse(NexiumHaste.grants(new ItemStack(ModItems.NEXIUM_SWORD)), "the sword doesn't");
        h.assertFalse(NexiumHaste.grants(new ItemStack(ModItems.AIOTS.get(AiotTier.NETHERITE))), "a netherite AIOT doesn't");
        h.succeed();
    }

    @GameTest
    public void armorStats(GameTestHelper h) {
        record Piece(Item item, EquipmentSlot slot, int armor, int durability) {}
        List<Piece> set = List.of(new Piece(ModItems.NEXIUM_HELMET, EquipmentSlot.HEAD, 4, 484),
                new Piece(ModItems.NEXIUM_CHESTPLATE, EquipmentSlot.CHEST, 9, 704),
                new Piece(ModItems.NEXIUM_LEGGINGS, EquipmentSlot.LEGS, 7, 660),
                new Piece(ModItems.NEXIUM_BOOTS, EquipmentSlot.FEET, 4, 572));
        double armor = 0, toughness = 0;
        for (Piece p : set) {
            ItemStack stack = new ItemStack(p.item());
            double a = attribute(stack, Attributes.ARMOR, p.slot());
            double t = attribute(stack, Attributes.ARMOR_TOUGHNESS, p.slot());
            double k = attribute(stack, Attributes.KNOCKBACK_RESISTANCE, p.slot());
            h.assertTrue(a == p.armor(), p.item() + " gives " + p.armor() + " armor, got " + a);
            h.assertTrue(t == 5, p.item() + " gives 5 toughness, got " + t);
            h.assertTrue(Math.abs(k - 0.15) < 1e-6, p.item() + " gives 0.15 knockback resistance, got " + k);
            h.assertTrue(stack.getMaxDamage() == p.durability(), p.item() + " lasts " + p.durability() + ", got " + stack.getMaxDamage());
            h.assertTrue(stack.has(DataComponents.DAMAGE_RESISTANT), p.item() + " survives lava");
            h.assertTrue(stack.is(ItemTags.TRIMMABLE_ARMOR), p.item() + " takes armor trims");
            armor += a;
            toughness += t;
        }
        h.assertTrue(armor == 24, "a full set is 24 armor (netherite 20)");
        h.assertTrue(toughness == 20, "and 20 toughness, the game's maximum");
        h.succeed();
    }

    @GameTest
    public void smithingTurnsNetheriteGearIntoNexium(GameTestHelper h) {
        Map<Item, Item> upgrades = Map.of(Items.NETHERITE_SWORD, ModItems.NEXIUM_SWORD, Items.NETHERITE_PICKAXE, ModItems.NEXIUM_PICKAXE,
                Items.NETHERITE_AXE, ModItems.NEXIUM_AXE, Items.NETHERITE_SHOVEL, ModItems.NEXIUM_SHOVEL, Items.NETHERITE_HOE, ModItems.NEXIUM_HOE,
                Items.NETHERITE_SPEAR, ModItems.NEXIUM_SPEAR, Items.NETHERITE_HELMET, ModItems.NEXIUM_HELMET,
                Items.NETHERITE_CHESTPLATE, ModItems.NEXIUM_CHESTPLATE, Items.NETHERITE_LEGGINGS, ModItems.NEXIUM_LEGGINGS,
                Items.NETHERITE_BOOTS, ModItems.NEXIUM_BOOTS);
        for (var e : upgrades.entrySet()) {
            ItemStack base = new ItemStack(e.getKey());
            base.enchant(enchantment(h, Enchantments.UNBREAKING), 3);
            ItemStack result = smith(h, base, new ItemStack(ModItems.NEXIUM_INGOT));
            h.assertTrue(result.is(e.getValue()), e.getKey() + " + Nexium ingot = " + e.getValue() + ", got " + result);
            h.assertTrue(result.getEnchantments().getLevel(enchantment(h, Enchantments.UNBREAKING)) == 3, "enchantments stay on " + result);
        }
        h.assertTrue(smith(h, new ItemStack(Items.DIAMOND_SWORD), new ItemStack(ModItems.NEXIUM_INGOT)).isEmpty(),
                "diamond gear has to become netherite first");
        h.succeed();
    }

    @GameTest
    public void nexiumAiot(GameTestHelper h) {
        List<ItemStack> tools = AiotTier.NEXIUM.tools().stream().map(ItemStack::new).toList();
        ItemStack merged = craft(h, tools);
        h.assertTrue(merged.is(ModItems.AIOTS.get(AiotTier.NEXIUM)), "five Nexium tools make a Nexium AIOT, got " + merged);
        h.assertTrue(merged.getMaxDamage() == 18750, "it holds one and a half times their 12,500 uses");
        ItemStack upgraded = smith(h, new ItemStack(ModItems.AIOTS.get(AiotTier.NETHERITE)), new ItemStack(ModItems.NEXIUM_BLOCK));
        h.assertTrue(upgraded.is(ModItems.AIOTS.get(AiotTier.NEXIUM)), "netherite AIOT + Block of Nexium = Nexium AIOT, got " + upgraded);
        h.assertTrue(smith(h, new ItemStack(ModItems.AIOTS.get(AiotTier.NETHERITE)), new ItemStack(ModItems.NEXIUM_INGOT)).isEmpty(),
                "one ingot isn't enough for an AIOT");
        h.succeed();
    }

    private static MobEffectInstance haste(ServerPlayer player) {
        return player.getEffect(MobEffects.HASTE);
    }

    @GameTest
    public void hasteWhileHeldAndOffInTheMenu(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, new ItemStack(ModItems.NEXIUM_PICKAXE));
        NexiumHaste.update(player);
        MobEffectInstance effect = haste(player);
        h.assertTrue(effect != null && effect.getAmplifier() == 1 && effect.isInfiniteDuration(), "holding it gives Haste II, got " + effect);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        NexiumHaste.update(player);
        h.assertTrue(haste(player) == null, "putting it away takes Haste II off");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.AIOTS.get(AiotTier.NEXIUM)));
        NexiumHaste.update(player);
        h.assertTrue(haste(player) != null, "the Nexium AIOT gives it too");
        ToolMenu.toggleHaste(player, player.getMainHandItem());
        h.assertTrue(player.getMainHandItem().has(ModComponents.HASTE_OFF), "the tool menu turns it off on the tool");
        h.assertTrue(haste(player) == null, "and the effect goes at once");
        NexiumHaste.update(player);
        h.assertTrue(haste(player) == null, "and stays off");
        ToolMenu.toggleHaste(player, player.getMainHandItem());
        h.assertTrue(haste(player) != null, "turning it back on gives it back");

        // A beacon's Haste II (finite) is never taken away
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        NexiumHaste.update(player);
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 260, 1, true, true));
        NexiumHaste.update(player);
        h.assertTrue(haste(player) != null && !haste(player).isInfiniteDuration(), "a beacon's Haste II stays");
        h.succeed();
    }

    /** A Nexium pickaxe with Efficiency V mines exactly as fast as a netherite one with Efficiency V under a Haste II beacon. */
    @GameTest
    public void efficiencyFiveMinesLikeAHasteTwoBeacon(GameTestHelper h) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        ItemStack netherite = new ItemStack(Items.NETHERITE_PICKAXE);
        netherite.enchant(enchantment(h, Enchantments.EFFICIENCY), 5);
        ServerPlayer withBeacon = playerHolding(h, netherite);
        withBeacon.addEffect(new MobEffectInstance(MobEffects.HASTE, 260, 1, true, true));
        float beaconSpeed = withBeacon.getDestroySpeed(stone);

        ItemStack nexium = new ItemStack(ModItems.NEXIUM_PICKAXE);
        nexium.enchant(enchantment(h, Enchantments.EFFICIENCY), 5);
        ServerPlayer withNexium = playerHolding(h, nexium);
        NexiumHaste.update(withNexium);
        float nexiumSpeed = withNexium.getDestroySpeed(stone);
        h.assertTrue(Math.abs(nexiumSpeed - beaconSpeed) < 1e-3, "Nexium Efficiency V (" + nexiumSpeed
                + ") = netherite Efficiency V under Haste II (" + beaconSpeed + ")");

        withNexium.getMainHandItem().set(ModComponents.HASTE_OFF, Unit.INSTANCE);
        NexiumHaste.update(withNexium);
        h.assertTrue(withNexium.getDestroySpeed(stone) < nexiumSpeed, "slower with Haste II turned off");
        h.succeed();
    }
}
