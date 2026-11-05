package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.aiot.AiotTier;
import net.storagenexus.aiot.MiningMode;
import net.storagenexus.aiot.ToolMenu;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The all-in-one tools: their stats, mining, right-click actions, the merge recipe and the netherite upgrade. */
public class AiotGameTest {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /** Blocks every kind of tool cares about, including the sword's special cases and ones that need a strong pickaxe. */
    private static final List<Block> SAMPLE_BLOCKS = List.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.OBSIDIAN, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE,
            Blocks.ANCIENT_DEBRIS, Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.PUMPKIN, Blocks.MELON, Blocks.DIRT,
            Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.GRAVEL, Blocks.SNOW_BLOCK, Blocks.HAY_BLOCK, Blocks.SCULK,
            Blocks.OAK_LEAVES, Blocks.COBWEB, Blocks.BAMBOO, Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.SPONGE,
            Blocks.GLASS, Blocks.BEDROCK);

    private static AiotItem aiot(AiotTier tier) {
        return ModItems.AIOTS.get(tier);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** The value a player holding this item gets, as the tooltip shows it (player base: 1 damage, 4 attacks a second). */
    private static double attribute(ItemStack stack, Holder<Attribute> attribute) {
        double playerBase = Player.createAttributes().build().getBaseValue(attribute);
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .compute(attribute, playerBase, EquipmentSlot.MAINHAND);
    }

    /** What the crafting grid makes from these items, or empty if no recipe matches. */
    private static ItemStack craft(GameTestHelper h, List<ItemStack> items) {
        List<ItemStack> grid = new ArrayList<>(items);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> freshTools(AiotTier tier) {
        return tier.tools().stream().map(ItemStack::new).toList();
    }

    @GameTest
    public void statsMatchTheChosenOptions(GameTestHelper h) {
        // Durability: five tools' worth of each material's vanilla durability, times 1.5 (stone, copper, iron, golden,
        // diamond, netherite, Nexium)
        int[] expectedDurability = {983, 1425, 1875, 240, 11708, 15233, 18750};
        for (AiotTier tier : AiotTier.values()) {
            ItemStack stack = new ItemStack(aiot(tier));
            String name = tier.id();
            h.assertTrue(stack.getMaxDamage() == expectedDurability[tier.ordinal()],
                    name + " AIOT durability is " + expectedDurability[tier.ordinal()] + ", got " + stack.getMaxDamage());

            // Combat option D: the axe's damage at the sword's speed, sweeping and disabling shields
            ItemStack axe = new ItemStack(tier.tools().get(1));
            ItemStack sword = new ItemStack(tier.tools().get(4));
            h.assertTrue(attribute(stack, Attributes.ATTACK_DAMAGE) == attribute(axe, Attributes.ATTACK_DAMAGE),
                    name + " AIOT hits as hard as the axe (" + attribute(axe, Attributes.ATTACK_DAMAGE) + "), got "
                            + attribute(stack, Attributes.ATTACK_DAMAGE));
            h.assertTrue(Math.abs(attribute(stack, Attributes.ATTACK_SPEED) - attribute(sword, Attributes.ATTACK_SPEED)) < 1e-6,
                    name + " AIOT attacks as fast as the sword (" + attribute(sword, Attributes.ATTACK_SPEED) + "), got "
                            + attribute(stack, Attributes.ATTACK_SPEED));
            h.assertTrue(attribute(stack, Attributes.ATTACK_DAMAGE) == tier.attackDamage(), name + " damage matches AiotTier");
            Weapon weapon = stack.get(DataComponents.WEAPON);
            h.assertTrue(weapon != null && weapon.itemDamagePerAttack() == 1, name + " AIOT loses 1 durability per hit");
            h.assertTrue(weapon.disableBlockingForSeconds() == Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS,
                    name + " AIOT disables shields like an axe");
            h.assertTrue(stack.get(DataComponents.TOOL).damagePerBlock() == 1, name + " AIOT loses 1 durability per block");
            h.assertTrue(stack.is(ItemTags.SWORDS), name + " AIOT is a sword, so it sweeps");
            for (var tag : List.of(ItemTags.PICKAXES, ItemTags.AXES, ItemTags.SHOVELS, ItemTags.HOES)) {
                h.assertTrue(stack.is(tag), name + " AIOT is in " + tag.location());
            }
            h.assertTrue(stack.has(DataComponents.DAMAGE_RESISTANT) == (tier == AiotTier.NETHERITE || tier == AiotTier.NEXIUM),
                    name + " AIOT survives lava only if it is netherite or Nexium");
        }
        h.assertTrue(new ItemStack(aiot(AiotTier.GOLDEN)).is(ItemTags.PIGLIN_LOVED), "piglins love the golden AIOT");
        h.succeed();
    }

    @GameTest
    public void takesEveryToolsEnchantments(GameTestHelper h) {
        ItemStack stack = new ItemStack(aiot(AiotTier.DIAMOND));
        for (var key : List.of(Enchantments.EFFICIENCY, Enchantments.FORTUNE, Enchantments.SILK_TOUCH, Enchantments.UNBREAKING,
                Enchantments.MENDING, Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.SWEEPING_EDGE,
                Enchantments.LOOTING, Enchantments.FIRE_ASPECT, Enchantments.KNOCKBACK)) {
            h.assertTrue(enchantment(h, key).value().isSupportedItem(stack), "the AIOT takes " + key.identifier());
        }
        h.succeed();
    }

    /** Every AIOT mines each block exactly as fast as the best of its five tools, and gets drops when any of them would. */
    @GameTest
    public void minesLikeTheBestOfItsTools(GameTestHelper h) {
        for (AiotTier tier : AiotTier.values()) {
            ItemStack stack = new ItemStack(aiot(tier));
            for (Block block : SAMPLE_BLOCKS) {
                BlockState state = block.defaultBlockState();
                float best = 0;
                boolean anyDrops = false;
                for (Item tool : tier.tools()) {
                    ItemStack toolStack = new ItemStack(tool);
                    best = Math.max(best, toolStack.getDestroySpeed(state));
                    anyDrops |= toolStack.isCorrectToolForDrops(state);
                }
                String what = tier.id() + " AIOT on " + block.getDescriptionId();
                h.assertTrue(stack.getDestroySpeed(state) == best, what + ": speed " + best + ", got " + stack.getDestroySpeed(state));
                h.assertTrue(stack.isCorrectToolForDrops(state) == anyDrops, what + ": gets drops " + anyDrops);
            }
        }
        // And the material matters, as it does for pickaxes
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        h.assertTrue(new ItemStack(aiot(AiotTier.DIAMOND)).isCorrectToolForDrops(obsidian), "a diamond AIOT mines obsidian");
        h.assertFalse(new ItemStack(aiot(AiotTier.IRON)).isCorrectToolForDrops(obsidian), "an iron AIOT can't mine obsidian");
        h.succeed();
    }

    private static Block vanillaBlock(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
    }

    private static ServerPlayer playerHolding(GameTestHelper h, ItemStack stack) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void rightClickTop(GameTestHelper h, ServerPlayer player, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.UP, 0.5), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit);
    }

    @GameTest
    public void rightClickStripsPathsAndTills(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, new ItemStack(aiot(AiotTier.IRON)));

        h.setBlock(POS, Blocks.OAK_LOG);
        rightClickTop(h, player, POS);
        h.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, POS);

        h.setBlock(POS, vanillaBlock("weathered_copper"));
        rightClickTop(h, player, POS);
        h.assertBlockPresent(vanillaBlock("exposed_copper"), POS);

        h.setBlock(POS, Blocks.GRASS_BLOCK);
        rightClickTop(h, player, POS);
        h.assertBlockPresent(Blocks.DIRT_PATH, POS);
        // A second right-click tills the path: the shovel has nothing more to do there, so the hoe gets its turn
        rightClickTop(h, player, POS);
        h.assertBlockPresent(Blocks.FARMLAND, POS);

        // Like a shovel, it puts out a campfire
        h.setBlock(POS, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        rightClickTop(h, player, POS);
        h.assertBlockProperty(POS, CampfireBlock.LIT, false);

        // Sneaking tills instead of making a path
        h.setBlock(POS, Blocks.GRASS_BLOCK);
        player.setShiftKeyDown(true);
        rightClickTop(h, player, POS);
        h.assertBlockPresent(Blocks.FARMLAND, POS);

        // Sneaking still strips a log: the hoe has nothing to do there, so the axe gets its turn
        h.setBlock(POS, Blocks.BIRCH_LOG);
        rightClickTop(h, player, POS);
        h.assertBlockPresent(Blocks.STRIPPED_BIRCH_LOG, POS);
        h.succeed();
    }

    @GameTest
    public void fiveFreshToolsMakeAFullAiot(GameTestHelper h) {
        for (AiotTier tier : AiotTier.values()) {
            List<ItemStack> tools = new ArrayList<>(freshTools(tier));
            java.util.Collections.reverse(tools); // any order works
            ItemStack result = craft(h, tools);
            h.assertTrue(result.is(aiot(tier)), "five " + tier.id() + " tools make a " + tier.id() + " AIOT, got " + result);
            h.assertTrue(result.getDamageValue() == 0, tier.id() + " AIOT from fresh tools is undamaged");
            h.assertTrue(result.getMaxDamage() == Math.round(5 * new ItemStack(tier.tools().getFirst()).getMaxDamage() * 1.5F),
                    tier.id() + " AIOT holds one and a half times the five tools' durability");
        }
        h.succeed();
    }

    @GameTest
    public void toolsMustBeOneMaterial(GameTestHelper h) {
        List<ItemStack> mixed = new ArrayList<>(freshTools(AiotTier.DIAMOND));
        mixed.set(0, new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(craft(h, mixed).isEmpty(), "an iron pickaxe with diamond tools makes nothing");
        h.assertTrue(craft(h, freshTools(AiotTier.DIAMOND).subList(0, 4)).isEmpty(), "four tools make nothing");
        h.succeed();
    }

    @GameTest
    public void mergeKeepsEnchantmentsWearAndRepairCost(GameTestHelper h) {
        List<ItemStack> tools = new ArrayList<>(freshTools(AiotTier.DIAMOND)); // pickaxe, axe, shovel, hoe, sword
        ItemStack pickaxe = tools.get(0), axe = tools.get(1), shovel = tools.get(2), hoe = tools.get(3), sword = tools.get(4);
        pickaxe.enchant(enchantment(h, Enchantments.EFFICIENCY), 5);
        pickaxe.enchant(enchantment(h, Enchantments.FORTUNE), 3);
        pickaxe.enchant(enchantment(h, Enchantments.UNBREAKING), 2);
        pickaxe.setDamageValue(1000);
        pickaxe.set(DataComponents.REPAIR_COST, 7);
        shovel.enchant(enchantment(h, Enchantments.SILK_TOUCH), 1);
        shovel.enchant(enchantment(h, Enchantments.EFFICIENCY), 4);
        shovel.enchant(enchantment(h, Enchantments.UNBREAKING), 3);
        shovel.setDamageValue(561);
        axe.enchant(enchantment(h, Enchantments.SHARPNESS), 3);
        sword.enchant(enchantment(h, Enchantments.SMITE), 5);
        sword.enchant(enchantment(h, Enchantments.SWEEPING_EDGE), 2);
        sword.set(DataComponents.REPAIR_COST, 3);
        hoe.enchant(enchantment(h, Enchantments.VANISHING_CURSE), 1);
        hoe.enchant(enchantment(h, Enchantments.MENDING), 1);

        ItemStack result = craft(h, tools);
        h.assertTrue(result.is(aiot(AiotTier.DIAMOND)), "the tools make a diamond AIOT, got " + result);
        ItemEnchantments active = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments inactive = result.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY);
        String all = "active " + active + ", inactive " + inactive;
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.EFFICIENCY)) == 5, "highest Efficiency (V) kept: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.UNBREAKING)) == 3, "highest Unbreaking (III) kept: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.FORTUNE)) == 3, "Fortune III is active: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 0, "Silk Touch isn't active: " + all);
        h.assertTrue(inactive.getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "Silk Touch waits, inactive: " + all);
        h.assertTrue(result.get(ModComponents.MINING_MODE) == MiningMode.FORTUNE, "the mining mode is Fortune (green gem)");
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.SMITE)) == 5, "Smite V beats Sharpness III: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.SHARPNESS)) == 0, "Sharpness III lost to Smite V: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.SWEEPING_EDGE)) == 2, "Sweeping Edge kept: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.MENDING)) == 1, "Mending kept: " + all);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.VANISHING_CURSE)) == 1, "the curse stays: " + all);

        // Uses left: 561 + 1561 + 1000 + 1561 + 1561 = 6244 of 7805; the AIOT holds 1.5 times that, 9366 of 11708, so 2342 used
        h.assertTrue(result.getDamageValue() == 2342, "the AIOT keeps the tools' wear, damage 2342, got " + result.getDamageValue());
        h.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 7, "the highest repair cost (7) carries over");
        h.succeed();
    }

    @GameTest
    public void sharpnessWinsATie(GameTestHelper h) {
        List<ItemStack> tools = new ArrayList<>(freshTools(AiotTier.IRON));
        tools.get(1).enchant(enchantment(h, Enchantments.BANE_OF_ARTHROPODS), 3); // axe
        tools.get(4).enchant(enchantment(h, Enchantments.SHARPNESS), 3); // sword
        tools.get(3).enchant(enchantment(h, Enchantments.SMITE), 3); // hoe (only by command, but it shouldn't matter)
        ItemEnchantments active = craft(h, tools).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        h.assertTrue(active.getLevel(enchantment(h, Enchantments.SHARPNESS)) == 3, "Sharpness wins the tie: " + active);
        h.assertTrue(active.size() == 1, "only one damage enchantment stays: " + active);
        h.succeed();
    }

    @GameTest
    public void silkTouchAloneStaysActive(GameTestHelper h) {
        List<ItemStack> tools = new ArrayList<>(freshTools(AiotTier.IRON));
        tools.get(2).enchant(enchantment(h, Enchantments.SILK_TOUCH), 1); // shovel
        ItemStack result = craft(h, tools);
        h.assertTrue(result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "Silk Touch is active when there is no Fortune");
        h.assertFalse(result.has(ModComponents.INACTIVE_ENCHANTMENTS), "nothing waits inactive");
        h.succeed();
    }

    @GameTest
    public void netheriteBlockUpgradesADiamondAiot(GameTestHelper h) {
        ItemStack diamond = withSilkTouchAndFortune(h, new ItemStack(aiot(AiotTier.DIAMOND)), MiningMode.FORTUNE);
        diamond.enchant(enchantment(h, Enchantments.EFFICIENCY), 5);
        diamond.setDamageValue(500);
        ItemStack template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        SmithingRecipeInput input = new SmithingRecipeInput(template, diamond, new ItemStack(Items.NETHERITE_BLOCK));
        ItemStack result = h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(result.is(aiot(AiotTier.NETHERITE)), "template + diamond AIOT + netherite block = netherite AIOT, got " + result);
        h.assertTrue(result.getEnchantments().getLevel(enchantment(h, Enchantments.EFFICIENCY)) == 5, "enchantments stay");
        h.assertTrue(result.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "the inactive Silk Touch stays");
        h.assertTrue(result.getDamageValue() == 500, "wear stays, as vanilla's netherite upgrade keeps it");
        h.assertTrue(result.get(ModComponents.MINING_MODE) == MiningMode.FORTUNE, "the mining mode stays");

        SmithingRecipeInput ingot = new SmithingRecipeInput(template, diamond, new ItemStack(Items.NETHERITE_INGOT));
        Optional<?> withIngot = h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING, ingot, h.getLevel());
        h.assertTrue(withIngot.isEmpty(), "a netherite ingot isn't enough");
        h.succeed();
    }

    @GameTest
    public void grindstoneRemovesInactiveEnchantments(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, ItemStack.EMPTY);
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(POS)));
        ItemStack stack = withSilkTouchAndFortune(h, new ItemStack(aiot(AiotTier.DIAMOND)), MiningMode.FORTUNE);
        menu.getSlot(GrindstoneMenu.INPUT_SLOT).set(stack);
        ItemStack result = menu.getSlot(GrindstoneMenu.RESULT_SLOT).getItem();
        h.assertTrue(result.is(aiot(AiotTier.DIAMOND)), "the grindstone takes the enchantments off, got " + result);
        h.assertTrue(result.getEnchantments().isEmpty(), "no active enchantments left");
        h.assertFalse(result.has(ModComponents.INACTIVE_ENCHANTMENTS), "no inactive Silk Touch left either");
        h.assertFalse(result.has(ModComponents.MINING_MODE), "and no mining mode (purple gem again)");
        h.succeed();
    }

    /** An AIOT holding Silk Touch I and Fortune III, the given one active. */
    static ItemStack withSilkTouchAndFortune(GameTestHelper h, ItemStack aiot, MiningMode active) {
        aiot.enchant(enchantment(h, Enchantments.FORTUNE), 3);
        aiot.enchant(enchantment(h, Enchantments.SILK_TOUCH), 1);
        AiotEnchanting.normalize(aiot, active);
        return aiot;
    }

    @GameTest
    public void toolMenuSwitchesSilkTouchAndFortune(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, withSilkTouchAndFortune(h, new ItemStack(aiot(AiotTier.DIAMOND)), MiningMode.FORTUNE));
        ToolMenu.swapSilkTouchFortune(player, player.getMainHandItem());
        ItemStack held = player.getMainHandItem();
        h.assertTrue(held.getEnchantments().getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "Silk Touch is active now");
        h.assertTrue(held.getEnchantments().getLevel(enchantment(h, Enchantments.FORTUNE)) == 0, "Fortune isn't");
        h.assertTrue(held.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment(h, Enchantments.FORTUNE)) == 3, "Fortune III waits, inactive");
        h.assertTrue(held.get(ModComponents.MINING_MODE) == MiningMode.SILK_TOUCH, "the gem turns blue (Silk Touch)");
        h.assertTrue(player.getMainHandItem().isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "still a pickaxe");

        ToolMenu.swapSilkTouchFortune(player, player.getMainHandItem());
        held = player.getMainHandItem();
        h.assertTrue(held.getEnchantments().getLevel(enchantment(h, Enchantments.FORTUNE)) == 3, "Fortune III is back");
        h.assertTrue(held.get(ModComponents.MINING_MODE) == MiningMode.FORTUNE, "the gem turns green (Fortune)");

        // An AIOT without both can't switch, and nothing changes
        ServerPlayer other = playerHolding(h, new ItemStack(aiot(AiotTier.IRON)));
        ToolMenu.swapSilkTouchFortune(other, other.getMainHandItem());
        h.assertFalse(other.getMainHandItem().has(ModComponents.MINING_MODE), "an AIOT without both has no mode");
        h.succeed();
    }

    /** Silk Touch drops the block, Fortune multiplies the ore: whichever is active decides, like on any pickaxe. */
    @GameTest
    public void activeEnchantmentDecidesTheDrops(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, withSilkTouchAndFortune(h, new ItemStack(aiot(AiotTier.DIAMOND)), MiningMode.SILK_TOUCH));
        h.setBlock(POS, Blocks.DIAMOND_ORE);
        List<ItemStack> silkDrops = Block.getDrops(h.getBlockState(POS), h.getLevel(), h.absolutePos(POS), null, player, player.getMainHandItem());
        h.assertTrue(silkDrops.size() == 1 && silkDrops.getFirst().is(Items.DIAMOND_ORE), "Silk Touch: the ore block drops, got " + silkDrops);
        ToolMenu.swapSilkTouchFortune(player, player.getMainHandItem());
        List<ItemStack> fortuneDrops = Block.getDrops(h.getBlockState(POS), h.getLevel(), h.absolutePos(POS), null, player, player.getMainHandItem());
        h.assertTrue(!fortuneDrops.isEmpty() && fortuneDrops.stream().allMatch(s -> s.is(Items.DIAMOND)), "Fortune: diamonds drop, got " + fortuneDrops);
        h.succeed();
    }

    @GameTest
    public void anvilAddsTheOtherOneInactive(GameTestHelper h) {
        ServerPlayer player = playerHolding(h, ItemStack.EMPTY);
        // Fortune III AIOT + Silk Touch book: Fortune stays active, Silk Touch is added inactive
        ItemStack fortune = new ItemStack(aiot(AiotTier.DIAMOND));
        fortune.enchant(enchantment(h, Enchantments.FORTUNE), 3);
        ItemStack result = anvil(h, player, fortune, book(h, Enchantments.SILK_TOUCH, 1));
        h.assertTrue(result.is(aiot(AiotTier.DIAMOND)), "the anvil takes Silk Touch onto a Fortune AIOT, got " + result);
        h.assertTrue(result.getEnchantments().getLevel(enchantment(h, Enchantments.FORTUNE)) == 3, "Fortune stays active");
        h.assertTrue(result.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment(h, Enchantments.SILK_TOUCH)) == 1, "Silk Touch is added, inactive");
        h.assertTrue(result.get(ModComponents.MINING_MODE) == MiningMode.FORTUNE, "mode: Fortune");

        // Silk Touch AIOT + Fortune book: Silk Touch stays active
        ItemStack silk = new ItemStack(aiot(AiotTier.DIAMOND));
        silk.enchant(enchantment(h, Enchantments.SILK_TOUCH), 1);
        result = anvil(h, player, silk, book(h, Enchantments.FORTUNE, 2));
        h.assertTrue(result.get(ModComponents.MINING_MODE) == MiningMode.SILK_TOUCH, "mode: Silk Touch, got " + result.get(ModComponents.MINING_MODE));
        h.assertTrue(result.getOrDefault(ModComponents.INACTIVE_ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(enchantment(h, Enchantments.FORTUNE)) == 2, "Fortune II is added, inactive");

        // A vanilla pickaxe still can't have both
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(enchantment(h, Enchantments.FORTUNE), 3);
        h.assertTrue(anvil(h, player, pickaxe, book(h, Enchantments.SILK_TOUCH, 1)).isEmpty(), "vanilla pickaxes keep the vanilla rule");
        h.succeed();
    }

    private static ItemStack book(GameTestHelper h, ResourceKey<Enchantment> key, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment(h, key), level);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private static ItemStack anvil(GameTestHelper h, ServerPlayer player, ItemStack left, ItemStack right) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(POS)));
        menu.getSlot(0).set(left.copy());
        menu.getSlot(1).set(right.copy());
        return menu.getSlot(2).getItem().copy();
    }
}
