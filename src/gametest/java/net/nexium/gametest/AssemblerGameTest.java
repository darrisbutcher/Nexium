package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.nexium.essentials.assembler.AssemblyTarget;
import net.nexium.essentials.assembler.FarmAssemblerBlockEntity;
import net.nexium.essentials.assembler.Requirement;
import net.nexium.essentials.capture.CapturedMob;
import net.nexium.essentials.capture.MobCapture;
import net.nexium.essentials.ironfarm.IronFarmRecipe;
import net.nexium.essentials.librarian.Librarian;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Farm Assembler: it builds every farm and the Master Librarian from their materials (mobs as spawn eggs or Captured
 * Mobs), takes only what is still needed, hands things back when switched, takes items from hoppers, keeps everything
 * when saved, and drops it when broken. The farms cost what their old double chests did.
 */
public class AssemblerGameTest {
    private static final BlockPos AT = new BlockPos(2, 1, 2);

    // ---------------------------------------------------------------- helpers

    private static FarmAssemblerBlockEntity assembler(GameTestHelper h) {
        h.setBlock(AT, ModBlocks.FARM_ASSEMBLER);
        return h.getBlockEntity(AT, FarmAssemblerBlockEntity.class);
    }

    private static ItemStack captured(EntityType<?> type) {
        return MobCapture.toItem(new CapturedMob(type, new CompoundTag(), Optional.empty(), 20.0F, 20.0F, false));
    }

    private static ItemStack capturedFrog(String variant) {
        CompoundTag data = new CompoundTag();
        data.putString("variant", "minecraft:" + variant);
        return MobCapture.toItem(new CapturedMob(EntityTypes.FROG, data, Optional.empty(), 10.0F, 10.0F, false));
    }

    private static void add(List<ItemStack> list, Item item, int count) {
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            list.add(new ItemStack(item, n));
            count -= n;
        }
    }

    private static void mobs(List<ItemStack> list, boolean capturedMobs, int count, EntityType<?> type, Item egg) {
        if (capturedMobs) for (int i = 0; i < count; i++) list.add(captured(type));
        else list.add(new ItemStack(egg, count));
    }

    /** Everything a target needs, with mobs as spawn eggs or as Captured Mobs, and materials mixed between kinds. */
    private static List<ItemStack> materials(AssemblyTarget target, boolean capturedMobs) {
        List<ItemStack> list = new ArrayList<>();
        switch (target) {
            case IRON_FARM -> {
                if (capturedMobs) {
                    list.add(captured(EntityTypes.ZOMBIE));
                    list.add(captured(EntityTypes.HUSK));
                    list.add(captured(EntityTypes.DROWNED));
                    list.add(captured(EntityTypes.ZOMBIE_VILLAGER));
                } else {
                    list.add(new ItemStack(Items.ZOMBIE_SPAWN_EGG, 3));
                    list.add(new ItemStack(Items.HUSK_SPAWN_EGG, 1));
                }
                mobs(list, capturedMobs, 12, EntityTypes.VILLAGER, Items.VILLAGER_SPAWN_EGG);
                add(list, Items.TORCH, 32);
                add(list, Items.SOUL_TORCH, 1);
                add(list, Items.OAK_SLAB, 10);
                add(list, Items.SMOOTH_STONE_SLAB, 10);
                add(list, Items.OAK_SIGN, 16);
                add(list, Items.SPRUCE_TRAPDOOR, 12);
                for (int i = 0; i < 12; i++) list.add(new ItemStack(i % 2 == 0 ? Items.BED.red() : Items.BED.white()));
                add(list, Items.SMOOTH_STONE, 576);
            }
            case VILLAGER_BREEDER -> {
                mobs(list, capturedMobs, 2, EntityTypes.VILLAGER, Items.VILLAGER_SPAWN_EGG);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(i % 2 == 0 ? Items.BED.red() : Items.BED.white()));
                add(list, Items.COMPOSTER, 2);
                add(list, Items.HAY_BLOCK, 8);
                add(list, Items.EMERALD_BLOCK, 2);
                add(list, Items.OAK_PLANKS, 288);
                add(list, Items.BIRCH_PLANKS, 288);
            }
            case PIGLIN_BARTER -> {
                mobs(list, capturedMobs, 2, EntityTypes.PIGLIN, Items.PIGLIN_SPAWN_EGG);
                add(list, Items.GOLD_BLOCK, 9);
                add(list, Items.SOUL_SAND, 16);
                add(list, Items.CRIMSON_PLANKS, 8);
                add(list, Items.BIRCH_PLANKS, 8);
                add(list, Items.NETHERRACK, 576);
            }
            case ZOMBIFIED_PIGLIN -> {
                mobs(list, capturedMobs, 4, EntityTypes.ZOMBIFIED_PIGLIN, Items.ZOMBIFIED_PIGLIN_SPAWN_EGG);
                add(list, Items.MAGMA_BLOCK, 8);
                add(list, Items.NETHER_BRICKS, 20);
                add(list, Items.GOLD_INGOT, 16);
                add(list, Items.NETHERRACK, 576);
            }
            case WITHER_SKELETON -> {
                mobs(list, capturedMobs, 4, EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SPAWN_EGG);
                add(list, Items.NETHER_BRICK_FENCE, 16);
                add(list, Items.SOUL_SAND, 8);
                add(list, Items.COAL_BLOCK, 4);
                add(list, Items.MAGMA_BLOCK, 4);
                add(list, Items.NETHERRACK, 576);
            }
            case SHULKER -> {
                mobs(list, capturedMobs, 4, EntityTypes.SHULKER, Items.SHULKER_SPAWN_EGG);
                add(list, Items.PURPUR_BLOCK, 32);
                add(list, Items.END_ROD, 8);
                add(list, Items.ENDER_PEARL, 8);
                add(list, Items.END_STONE_BRICKS, 576);
            }
            case WITCH -> {
                mobs(list, capturedMobs, 2, EntityTypes.WITCH, Items.WITCH_SPAWN_EGG);
                add(list, Items.CAULDRON, 8);
                add(list, Items.SPRUCE_PLANKS, 16);
                add(list, Items.LILY_PAD, 8);
                add(list, Items.MOSSY_COBBLESTONE, 576);
            }
            case SUGAR_CANE -> {
                add(list, Items.SUGAR_CANE, 32);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.PISTON, 8);
                add(list, Items.OBSERVER, 16);
                add(list, Items.SAND, 576);
            }
            case BAMBOO -> {
                add(list, Items.BAMBOO, 32);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.PISTON, 8);
                add(list, Items.OBSERVER, 16);
                add(list, Items.DIRT, 576);
            }
            case NETHER_WART -> {
                add(list, Items.NETHER_WART, 32);
                add(list, Items.SOUL_SAND, 16);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.LAVA_BUCKET));
                add(list, Items.MAGMA_BLOCK, 8);
                add(list, Items.NETHERRACK, 576);
            }
            case MONSTER -> {
                mobs(list, capturedMobs, 1, EntityTypes.ZOMBIE, Items.ZOMBIE_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.PILLAGER, Items.PILLAGER_SPAWN_EGG);
                add(list, Items.HOPPER, 8);
                add(list, Items.IRON_BARS, 16);
                add(list, Items.PISTON, 8);
                add(list, Items.EMERALD_BLOCK, 4);
                add(list, Items.COBBLESTONE, 576);
            }
            case NETHER_MOB -> {
                mobs(list, capturedMobs, 1, EntityTypes.BLAZE, Items.BLAZE_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.GHAST, Items.GHAST_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.MAGMA_CUBE, Items.MAGMA_CUBE_SPAWN_EGG);
                add(list, Items.NETHER_BRICKS, 16);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.LAVA_BUCKET));
                add(list, Items.MAGMA_BLOCK, 8);
                add(list, Items.NETHERRACK, 576);
            }
            case OCEAN -> {
                mobs(list, capturedMobs, 1, EntityTypes.GUARDIAN, Items.GUARDIAN_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.DROWNED, Items.DROWNED_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.COD, Items.COD_SPAWN_EGG);
                add(list, Items.PRISMARINE_BRICKS, 16);
                add(list, Items.SEA_LANTERN, 8);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.SAND, 576);
            }
            case SLIME -> {
                mobs(list, capturedMobs, 2, EntityTypes.SLIME, Items.SLIME_SPAWN_EGG);
                add(list, Items.SLIME_BLOCK, 4);
                add(list, Items.STICKY_PISTON, 8);
                add(list, Items.HOPPER, 8);
                add(list, Items.MUD, 576);
            }
            case LIVESTOCK -> {
                mobs(list, capturedMobs, 1, EntityTypes.COW, Items.COW_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.PIG, Items.PIG_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.SHEEP, Items.SHEEP_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.CHICKEN, Items.CHICKEN_SPAWN_EGG);
                mobs(list, capturedMobs, 1, EntityTypes.RABBIT, Items.RABBIT_SPAWN_EGG);
                add(list, Items.HAY_BLOCK, 16);
                add(list, Items.OAK_FENCE, 16);
                add(list, Items.DIRT, 576);
            }
            case CROP -> {
                add(list, Items.WHEAT, 32);
                add(list, Items.CARROT, 32);
                add(list, Items.POTATO, 32);
                add(list, Items.BEETROOT, 32);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.DIRT, 576);
            }
            case GOURD -> {
                add(list, Items.MELON, 16);
                add(list, Items.PUMPKIN, 16);
                add(list, Items.CACTUS, 16);
                add(list, Items.KELP, 16);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.SAND, 576);
            }
            case GENERATOR -> {
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.LAVA_BUCKET));
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.HOPPER, 8);
                for (int i = 0; i < 2; i++) list.add(new ItemStack(Items.DIAMOND_PICKAXE));
                add(list, Items.COBBLESTONE, 576);
            }
            case BEE -> {
                mobs(list, capturedMobs, 2, EntityTypes.BEE, Items.BEE_SPAWN_EGG);
                add(list, Items.BEEHIVE, 4);
                add(list, Items.CAMPFIRE, 4);
                add(list, Items.POPPY, 16);
                add(list, Items.GLASS_BOTTLE, 8);
                add(list, Items.OAK_PLANKS, 576);
            }
            case MOSS -> {
                add(list, Items.MOSS_BLOCK, 32);
                add(list, Items.AZALEA, 8);
                for (int i = 0; i < 4; i++) list.add(new ItemStack(Items.WATER_BUCKET));
                add(list, Items.BONE_BLOCK, 8);
                add(list, Items.CLAY, 576);
            }
            case TREE -> add(list, Items.OAK_LOG, 576 * 2);
            case FROGLIGHT -> {
                // Frogs only count as Captured Mobs: a spawn egg doesn't say which variant it makes
                for (String variant : new String[]{"temperate", "warm", "cold"}) {
                    for (int i = 0; i < 3; i++) list.add(capturedFrog(variant));
                }
                mobs(list, capturedMobs, 9, EntityTypes.SLIME, Items.SLIME_SPAWN_EGG);
                for (int i = 0; i < 9; i++) { add(list, Items.GLASS, 32); add(list, i % 2 == 0 ? Items.STAINED_GLASS.red() : Items.TINTED_GLASS, 32); }
            }
            case XP_FARM -> add(list, Items.EXPERIENCE_BOTTLE, AssemblyTarget.XP_FARM_BOTTLES);
            case LIBRARIAN -> mobs(list, capturedMobs, 12, EntityTypes.VILLAGER, Items.VILLAGER_SPAWN_EGG);
            case HOSTILE_SWITCH -> {
                // Any hostile mob counts: a mix of kinds as Captured Mobs, or spawn eggs of two kinds
                if (capturedMobs) {
                    EntityType<?>[] kinds = {EntityTypes.ZOMBIE, EntityTypes.CREEPER, EntityTypes.SKELETON, EntityTypes.ENDERMAN, EntityTypes.SPIDER};
                    for (int i = 0; i < 140; i++) list.add(captured(kinds[i % kinds.length]));
                } else {
                    list.add(new ItemStack(Items.ZOMBIE_SPAWN_EGG, 64));
                    list.add(new ItemStack(Items.CREEPER_SPAWN_EGG, 64));
                    list.add(new ItemStack(Items.BLAZE_SPAWN_EGG, 12));
                }
            }
        }
        return list;
    }

    private static int count(FarmAssemblerBlockEntity a, Item icon) {
        List<Requirement> reqs = a.target().requirements();
        for (int i = 0; i < reqs.size(); i++) if (reqs.get(i).icon() == icon) return a.counts()[i];
        throw new AssertionError("no requirement shown as " + icon);
    }

    /** How many of {@code item} the assembler holds as items (not counting fluid held for a line of buckets). */
    private static int heldItems(FarmAssemblerBlockEntity a, Item item) {
        return a.materials().stream().filter(m -> m.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static int inInventory(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) if (player.getInventory().getItem(i).is(item)) n += player.getInventory().getItem(i).getCount();
        return n;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theHostileMobSwitchTakesOnlyHostileMobs(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        a.select(AssemblyTarget.HOSTILE_SWITCH, null);
        h.assertValueEqual(a.accept(new ItemStack(Items.COW_SPAWN_EGG, 10)), 0, "a cow is not hostile");
        h.assertValueEqual(a.accept(captured(EntityTypes.VILLAGER)), 0, "neither is a villager");
        h.assertValueEqual(a.accept(captured(EntityTypes.WOLF)), 0, "or a wolf");
        h.assertValueEqual(a.accept(new ItemStack(Items.STONE, 64)), 0, "or stone");
        h.assertValueEqual(a.accept(captured(EntityTypes.CREEPER)), 1, "a Captured Creeper counts");
        h.assertValueEqual(a.accept(new ItemStack(Items.ZOMBIE_SPAWN_EGG, 138)), 138, "and so do spawn eggs of hostile mobs");
        h.assertFalse(a.isComplete(), "139 is not enough");
        h.assertValueEqual(a.accept(captured(EntityTypes.PIGLIN_BRUTE)), 1, "a piglin brute is hostile too");
        h.assertTrue(a.isComplete(), "140 hostile mobs complete it");
        h.assertValueEqual(a.accept(captured(EntityTypes.ZOMBIE)), 0, "and it takes no more");
        h.assertTrue(a.build(h.getLevel()), "it builds");
        h.assertTrue(a.output().removeItemNoUpdate(0).is(net.nexium.essentials.registry.ModItems.HOSTILE_MOB_SWITCH), "a Hostile Mob Switch comes out");
        h.succeed();
    }

    // ---------------------------------------------------------------- the storage network

    private static int inChest(net.minecraft.world.level.block.entity.ChestBlockEntity chest, Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    /** A chest, a Storage Connector beside it and the assembler beside that: the assembler touches a network that holds the chest. */
    private static net.minecraft.world.level.block.entity.ChestBlockEntity networkedChest(GameTestHelper h) {
        h.setBlock(AT.west(2), Blocks.CHEST);
        h.setBlock(AT.west(), net.nexium.registry.ModBlocks.STORAGE_CONNECTOR);
        return h.getBlockEntity(AT.west(2), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aLinkedAssemblerTakesWhatTheTargetNeedsFromTheNetwork(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var chest = networkedChest(h);
        chest.setItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        chest.setItem(1, new ItemStack(Items.EXPERIENCE_BOTTLE, 36));
        chest.setItem(2, new ItemStack(Items.STICK, 30));
        a.select(AssemblyTarget.XP_FARM, null);
        h.assertTrue(a.network() != null, "a Storage Connector against it links it to the network");
        h.assertValueEqual(a.addFromNetwork(), 64, "it takes the 64 Bottles o' Enchanting it needs, from two stacks");
        h.assertTrue(a.isComplete(), "and is complete");
        h.assertValueEqual(inChest(chest, Items.EXPERIENCE_BOTTLE), 36, "36 bottles are left in the network");
        h.assertValueEqual(inChest(chest, Items.STICK), 30, "what it does not need is left alone");
        h.assertValueEqual(a.addFromNetwork(), 0, "and a second press takes nothing more");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theNetworkFillsMobsAsSpawnEggsAndCapturedMobsAndLeavesTheOthers(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var chest = networkedChest(h);
        chest.setItem(0, captured(EntityTypes.CREEPER));
        chest.setItem(1, captured(EntityTypes.SKELETON));
        chest.setItem(2, captured(EntityTypes.COW));
        chest.setItem(3, new ItemStack(Items.ZOMBIE_SPAWN_EGG, 10));
        chest.setItem(4, new ItemStack(Items.COW_SPAWN_EGG, 10));
        a.select(AssemblyTarget.HOSTILE_SWITCH, null);
        h.assertValueEqual(a.addFromNetwork(), 12, "two Captured Mobs and ten spawn eggs of hostile mobs");
        h.assertValueEqual(a.counts()[0], 12, "counted towards the 140");
        h.assertTrue(chest.getItem(2).is(net.minecraft.world.item.Items.AIR) == false && IronFarmRecipe.mobOf(chest.getItem(2)) == EntityTypes.COW, "the Captured Cow is left in the network");
        h.assertValueEqual(inChest(chest, Items.COW_SPAWN_EGG), 10, "and the cow spawn eggs");
        h.assertValueEqual(inChest(chest, Items.ZOMBIE_SPAWN_EGG), 0, "the zombie eggs were taken");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void anAssemblerAwayFromTheNetworkTakesNothing(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        h.setBlock(AT.west(2), Blocks.CHEST);
        h.getBlockEntity(AT.west(2), net.minecraft.world.level.block.entity.ChestBlockEntity.class).setItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        a.select(AssemblyTarget.XP_FARM, null);
        h.assertTrue(a.network() == null, "a chest is not a network: nothing links it");
        h.assertValueEqual(a.addFromNetwork(), 0, "so there is nothing to take");
        h.setBlock(AT.west(), net.nexium.registry.ModBlocks.STORAGE_CONNECTOR);
        h.assertTrue(a.network() != null, "the connector put between them links it");
        h.assertValueEqual(a.addFromNetwork(), 64, "and now it can");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuButtonTakesFromTheNetworkAndKnowsWhetherItIsLinked(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        net.nexium.essentials.assembler.FarmAssemblerMenu menu = new net.nexium.essentials.assembler.FarmAssemblerMenu(5, player.getInventory(), a);
        h.assertFalse(menu.linked(), "alone, it says it is not linked");
        var chest = networkedChest(h);
        chest.setItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
        a.select(AssemblyTarget.XP_FARM, null);
        h.assertTrue(menu.linked(), "beside a network, it says it is");
        h.assertTrue(menu.clickMenuButton(player, net.nexium.essentials.assembler.FarmAssemblerMenu.BUTTON_ADD_NETWORK), "the button works");
        h.assertTrue(a.isComplete(), "and filled it");
        h.succeed();
    }

    // ---------------------------------------------------------------- building

    @GameTest(environment = EssentialsEnvironment.ID)
    public void everyTargetBuildsFromSpawnEggsAndFromCapturedMobs(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        for (AssemblyTarget target : AssemblyTarget.values()) {
            for (boolean capturedMobs : new boolean[]{false, true}) {
                a.select(target, null);
                for (ItemStack stack : materials(target, capturedMobs)) {
                    int took = a.accept(stack);
                    h.assertValueEqual(took, stack.getCount(), target + " takes all of " + stack + " (captured mobs: " + capturedMobs + ")");
                }
                h.assertTrue(a.isComplete(), target + " is complete with its recipe (captured mobs: " + capturedMobs + ")");
                h.assertTrue(a.build(h.getLevel()), target + " builds");
                ItemStack made = a.output().removeItemNoUpdate(0);
                if (target == AssemblyTarget.LIBRARIAN) h.assertTrue(Librarian.isMade(made), "a Master Librarian came out");
                else h.assertTrue(ItemStack.isSameItem(made, target.icon()), target + " came out, got " + made);
                h.assertTrue(a.materials().isEmpty(), target + " used up its materials");
            }
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theFarmsCostWhatTheirDoubleChestsDid(GameTestHelper h) {
        // The filler: 54 slots of full stacks, less one slot per Captured Mob or bed and a stack's worth of each other part
        // (The four newer farms ask a quarter of that: 12 stacks.)
        // Every farm asks 9 stacks of its filler block (the Tree Farm 18 of logs)
        Map<AssemblyTarget, Integer> filler = new java.util.EnumMap<>(AssemblyTarget.class);
        for (AssemblyTarget target : AssemblyTarget.values()) {
            if (target != AssemblyTarget.LIBRARIAN && target != AssemblyTarget.XP_FARM && target != AssemblyTarget.HOSTILE_SWITCH) filler.put(target, target == AssemblyTarget.TREE ? 1152 : 576);
        }
        filler.forEach((target, amount) -> {
            List<Requirement> reqs = target.requirements();
            h.assertValueEqual(reqs.getLast().needed(), amount, target + "'s filler");
        });
        h.assertValueEqual(AssemblyTarget.LIBRARIAN.requirements().getFirst().needed(), 12, "the librarian takes 12 villagers");
        h.assertValueEqual(AssemblyTarget.HOSTILE_SWITCH.requirements().size(), 1, "the Hostile Mob Switch has one line");
        h.assertValueEqual(AssemblyTarget.HOSTILE_SWITCH.requirements().getFirst().needed(), 140, "it takes 140 hostile mobs");
        // The Experience Farm takes one stack of Bottles o' Enchanting and nothing else
        h.assertValueEqual(AssemblyTarget.XP_FARM.requirements().size(), 1, "the Experience Farm has one line");
        h.assertValueEqual(AssemblyTarget.XP_FARM.requirements().getFirst().needed(), 64, "64 Bottles o' Enchanting");
        h.assertTrue(AssemblyTarget.XP_FARM.requirements().getFirst().icon() == Items.EXPERIENCE_BOTTLE, "shown as the bottle");
        h.assertTrue(AssemblyTarget.IRON_FARM.requirements().size() == AssemblyTarget.MAX_REQUIREMENTS, "the Iron Farm has the most lines");
        for (AssemblyTarget t : AssemblyTarget.values()) h.assertTrue(t.requirements().size() <= AssemblyTarget.MAX_REQUIREMENTS, t + " fits the screen");
        // What counts as what
        h.assertTrue(IronFarmRecipe.partOf(captured(EntityTypes.COW)) == null, "captured cow isn't a material");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.ZOMBIE_HORSE_SPAWN_EGG)) == null, "zombie horse isn't a zombie");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.DROWNED_SPAWN_EGG)) == IronFarmRecipe.Part.ZOMBIES, "drowned egg is a zombie");
        h.assertTrue(IronFarmRecipe.partOf(new ItemStack(Items.CRIMSON_HANGING_SIGN)) == IronFarmRecipe.Part.SIGNS, "hanging signs count");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void itTakesOnlyWhatIsStillNeeded(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);   // starts on the Iron Farm
        h.assertValueEqual(a.accept(new ItemStack(Items.VILLAGER_SPAWN_EGG, 64)), 12, "12 of 64 villagers");
        h.assertValueEqual(a.accept(new ItemStack(Items.VILLAGER_SPAWN_EGG, 1)), 0, "no 13th villager");
        h.assertValueEqual(a.accept(new ItemStack(Items.DIRT, 64)), 0, "no dirt");
        h.assertValueEqual(a.accept(captured(EntityTypes.COW)), 0, "no cows");
        h.assertValueEqual(a.accept(new ItemStack(Items.TORCH, 64)), 33, "33 torches");
        int stone = 0;
        for (int i = 0; i < 30; i++) stone += a.accept(new ItemStack(Items.SMOOTH_STONE, 64));
        h.assertValueEqual(stone, 576, "smooth stone up to 576");
        h.assertValueEqual(count(a, Items.VILLAGER_SPAWN_EGG), 12, "villagers counted");
        h.assertValueEqual(count(a, Items.TORCH), 33, "torches counted");
        h.assertTrue(!a.isComplete(), "not complete without the rest");
        h.assertTrue(!a.build(h.getLevel()), "it won't build");
        // A Master Librarian isn't a villager for a recipe
        a.select(AssemblyTarget.LIBRARIAN, null);
        h.assertValueEqual(a.accept(Librarian.create(h.getLevel())), 0, "a Master Librarian doesn't count as a villager");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void itWaitsForTheFinishedFarmToBeTaken(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        a.select(AssemblyTarget.SHULKER, null);
        for (ItemStack s : materials(AssemblyTarget.SHULKER, false)) a.accept(s);
        h.assertTrue(a.build(h.getLevel()), "builds");
        for (ItemStack s : materials(AssemblyTarget.SHULKER, false)) a.accept(s);
        h.assertTrue(a.isComplete(), "complete again");
        h.assertTrue(!a.build(h.getLevel()), "but not while the last one is still in the slot");
        a.output().removeItemNoUpdate(0);
        h.assertTrue(a.build(h.getLevel()), "builds once it is taken");
        h.succeed();
    }

    // ---------------------------------------------------------------- the screen's buttons

    @SuppressWarnings("removal")
    @GameTest(environment = EssentialsEnvironment.ID)
    public void addFromInventoryAndSwitchingMoveTheRightThings(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.VILLAGER_SPAWN_EGG, 20));
        player.getInventory().setItem(1, new ItemStack(Items.TORCH, 64));
        player.getInventory().setItem(2, new ItemStack(Items.DIRT, 10));
        player.getInventory().setItem(30, new ItemStack(Items.ZOMBIE_SPAWN_EGG, 2));
        a.addFrom(player);
        h.assertValueEqual(count(a, Items.VILLAGER_SPAWN_EGG), 12, "12 villagers went in");
        h.assertValueEqual(count(a, Items.TORCH), 33, "33 torches went in");
        h.assertValueEqual(count(a, Items.ZOMBIE_SPAWN_EGG), 2, "zombies from the main inventory went in");
        h.assertValueEqual(inInventory(player, Items.VILLAGER_SPAWN_EGG), 8, "8 villagers stayed");
        h.assertValueEqual(inInventory(player, Items.TORCH), 31, "31 torches stayed");
        h.assertValueEqual(inInventory(player, Items.DIRT), 10, "dirt stayed");
        // Switching hands it all back
        a.select(AssemblyTarget.SHULKER, player);
        h.assertTrue(a.target() == AssemblyTarget.SHULKER, "switched");
        h.assertTrue(a.materials().isEmpty(), "nothing left inside");
        h.assertValueEqual(inInventory(player, Items.VILLAGER_SPAWN_EGG), 20, "villagers back");
        h.assertValueEqual(inInventory(player, Items.TORCH), 64, "torches back");
        h.assertValueEqual(inInventory(player, Items.ZOMBIE_SPAWN_EGG), 2, "zombies back");
        h.succeed();
    }

    // ---------------------------------------------------------------- hoppers, saving, breaking

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void hoppersAndOtherModsCanFillIt(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(h.getLevel(), h.absolutePos(AT), Direction.UP);
        h.assertTrue(storage != null, "it offers an item storage");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual((int) storage.insert(ItemVariant.of(Items.TORCH), 50, tx), 33, "a mod can put 33 torches in");
            // not committed
        }
        h.assertValueEqual(count(a, Items.TORCH), 0, "an aborted insert leaves nothing");
        try (Transaction tx = Transaction.openOuter()) {
            storage.insert(ItemVariant.of(Items.OAK_SIGN), 16, tx);
            tx.commit();
        }
        h.assertValueEqual(count(a, Items.OAK_SIGN), 16, "a committed insert stays");
        try (Transaction tx = Transaction.openOuter()) {
            h.assertValueEqual((int) storage.extract(ItemVariant.of(Items.OAK_SIGN), 16, tx), 0, "nothing can be taken out");
        }
        // A real hopper above it
        BlockPos hopper = AT.above();
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(Items.TORCH, 4));
        h.succeedWhen(() -> h.assertValueEqual(count(a, Items.TORCH), 4, "the hopper's torches went in"));
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void itKeepsItsContentsWhenSavedAndDropsThemWhenBroken(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        a.select(AssemblyTarget.WITHER_SKELETON, null);
        a.accept(captured(EntityTypes.WITHER_SKELETON));
        a.accept(new ItemStack(Items.NETHERRACK, 64));
        a.output().setItem(0, new ItemStack(ModItems.IRON_FARM));
        CompoundTag saved = a.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(a.getBlockPos(), a.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(loaded instanceof FarmAssemblerBlockEntity, "loads as an assembler");
        FarmAssemblerBlockEntity b = (FarmAssemblerBlockEntity) loaded;
        h.assertTrue(b.target() == AssemblyTarget.WITHER_SKELETON, "target kept");
        h.assertValueEqual(count(b, Items.WITHER_SKELETON_SPAWN_EGG), 1, "the captured skeleton kept");
        h.assertValueEqual(count(b, Items.NETHERRACK), 64, "netherrack kept");
        h.assertTrue(b.output().getItem(0).is(ModItems.IRON_FARM), "finished farm kept");

        h.getLevel().destroyBlock(h.absolutePos(AT), true);
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(AT)).inflate(2));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.FARM_ASSEMBLER)), "the assembler drops itself");
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.CAPTURED_MOB)), "the skeleton drops");
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.NETHERRACK)), "the netherrack drops");
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.IRON_FARM)), "the finished farm drops");
        h.succeed();
    }

    // ---------------------------------------------------------------- fluid in the storage network

    /** A Fluid Drawer on the network (the connector beside the assembler touches it) holding {@code droplets} of {@code fluid}. */
    private static net.nexium.fluid.FluidDrawerBlockEntity networkedFluid(GameTestHelper h, net.minecraft.world.level.material.Fluid fluid, long droplets) {
        h.setBlock(AT.west(), net.nexium.registry.ModBlocks.STORAGE_CONNECTOR);
        BlockPos at = AT.west().north();
        h.setBlock(at, net.nexium.registry.ModBlocks.FLUID_DRAWER_1X1);
        net.nexium.fluid.FluidDrawerBlockEntity drawer = h.getBlockEntity(at, net.nexium.fluid.FluidDrawerBlockEntity.class);
        insertFluid(drawer, fluid, droplets);
        return drawer;
    }

    private static void insertFluid(net.nexium.fluid.FluidDrawerBlockEntity drawer, net.minecraft.world.level.material.Fluid fluid, long droplets) {
        try (Transaction tx = Transaction.openOuter()) {
            drawer.slot(0).insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid), droplets, tx);
            tx.commit();
        }
    }

    /** The index of the line of {@code bucket}s among the target's requirements. */
    private static int lineOf(AssemblyTarget target, Item bucket) {
        List<Requirement> reqs = target.requirements();
        for (int i = 0; i < reqs.size(); i++) if (reqs.get(i).icon() == bucket) return i;
        throw new AssertionError(target + " has no line of " + bucket);
    }

    private static final long BUCKET = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;

    @GameTest(environment = EssentialsEnvironment.ID)
    public void waterInTheNetworkFillsALineOfWaterBuckets(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        int water = lineOf(AssemblyTarget.SUGAR_CANE, Items.WATER_BUCKET);
        h.assertValueEqual(a.counts()[water], 0, "nothing in yet");
        h.assertValueEqual(a.addFromNetwork(), 4, "it takes 4 buckets' worth of water for the 4 water buckets it needs");
        h.assertValueEqual(a.counts()[water], 4, "the line counts them");
        h.assertValueEqual(heldItems(a, Items.WATER_BUCKET), 0, "no bucket items were made: it holds the water as fluid");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.WATER), 4 * BUCKET, "4 buckets of water held");
        h.assertValueEqual(drawer.slot(0).getAmount(), 6 * BUCKET, "6 are left in the network");
        h.assertValueEqual(a.addFromNetwork(), 0, "a second press takes nothing more");
        h.assertValueEqual(drawer.slot(0).getAmount(), 6 * BUCKET, "and the network keeps its 6");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void realBucketsInTheNetworkAreUsedBeforeFluid(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var chest = networkedChest(h);
        chest.setItem(0, new ItemStack(Items.WATER_BUCKET));
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.BAMBOO, null);
        int water = lineOf(AssemblyTarget.BAMBOO, Items.WATER_BUCKET);
        h.assertValueEqual(a.addFromNetwork(), 4, "one real bucket and three buckets' worth of water");
        h.assertValueEqual(heldItems(a, Items.WATER_BUCKET), 1, "the real bucket was taken");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.WATER), 3 * BUCKET, "and water for the other three");
        h.assertValueEqual(a.counts()[water], 4, "4 of 4");
        h.assertValueEqual(inChest(chest, Items.WATER_BUCKET), 0, "the chest's bucket is gone");
        h.assertValueEqual(drawer.slot(0).getAmount(), 7 * BUCKET, "only three buckets of water left the network");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void onlyWholeBucketsOfFluidAreTaken(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, BUCKET + BUCKET / 2);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        h.assertValueEqual(a.addFromNetwork(), 1, "a bucket and a half of water is one bucket");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.WATER), BUCKET, "held as one whole bucket");
        h.assertValueEqual(drawer.slot(0).getAmount(), BUCKET / 2, "the half bucket stays in the network");
        h.assertFalse(a.isComplete(), "it is not complete");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void lavaFillsLavaBucketsAndOnlyThoseLines(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.NETHER_WART, null);
        h.assertValueEqual(a.addFromNetwork(), 0, "water does not fill a line of lava buckets");
        a.select(AssemblyTarget.XP_FARM, null);
        h.assertValueEqual(a.addFromNetwork(), 0, "and a target with no bucket line takes no fluid");
        h.assertValueEqual(drawer.slot(0).getAmount(), 10 * BUCKET, "the network keeps all its water");

        h.getLevel().setBlock(h.absolutePos(AT.west().north()), Blocks.AIR.defaultBlockState(), 3); // the water drawer goes, a lava one takes its place
        var lava = networkedFluid(h, net.minecraft.world.level.material.Fluids.LAVA, 6 * BUCKET);
        a.select(AssemblyTarget.NETHER_WART, null);
        h.assertValueEqual(a.addFromNetwork(), 4, "lava fills the 4 lava buckets");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.LAVA), 4 * BUCKET, "held as lava");
        h.assertValueEqual(lava.slot(0).getAmount(), 2 * BUCKET, "2 buckets of lava are left");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void switchingTargetGivesTheFluidBackToTheNetwork(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        a.addFromNetwork();
        h.assertValueEqual(drawer.slot(0).getAmount(), 6 * BUCKET, "4 buckets went to the assembler");
        a.select(AssemblyTarget.BAMBOO, player);
        h.assertValueEqual(drawer.slot(0).getAmount(), 10 * BUCKET, "switching gave all 4 back to the network as fluid");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.WATER), 0L, "the assembler holds none");
        h.assertValueEqual(inInventory(player, Items.WATER_BUCKET), 0, "and no buckets came to the player: no free buckets");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void whatTheNetworkCannotTakeBackComesAsFilledBuckets(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 32 * BUCKET); // a Fluid Drawer holds 32 buckets
        a.select(AssemblyTarget.SUGAR_CANE, null);
        a.addFromNetwork();
        insertFluid(drawer, net.minecraft.world.level.material.Fluids.WATER, 2 * BUCKET); // room for two of the four
        h.assertValueEqual(drawer.slot(0).getAmount(), 30 * BUCKET, "the drawer has room for two buckets");
        a.select(AssemblyTarget.BAMBOO, player);
        h.assertValueEqual(drawer.slot(0).getAmount(), 32 * BUCKET, "two buckets went back as fluid");
        h.assertValueEqual(inInventory(player, Items.WATER_BUCKET), 2, "and the two it could not take came as filled buckets");

        // With no network touching it at all, all of it comes as filled buckets
        player.getInventory().clearContent();
        a.select(AssemblyTarget.SUGAR_CANE, null);
        a.addFromNetwork();
        h.getLevel().setBlock(h.absolutePos(AT.west()), Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(a.network() == null, "no network touches it now");
        a.select(AssemblyTarget.BAMBOO, player);
        h.assertValueEqual(inInventory(player, Items.WATER_BUCKET), 4, "so all 4 came as filled buckets");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void buildingUsesTheFluidUp(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        var drawer = networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        for (ItemStack stack : materials(AssemblyTarget.SUGAR_CANE, false)) if (!stack.is(Items.WATER_BUCKET)) a.accept(stack);
        h.assertFalse(a.isComplete(), "without the water it is not complete");
        a.addFromNetwork();
        h.assertTrue(a.isComplete(), "with the network's water it is");
        h.assertTrue(a.build(h.getLevel()), "it builds");
        h.assertTrue(ItemStack.isSameItem(a.output().removeItemNoUpdate(0), AssemblyTarget.SUGAR_CANE.icon()), "a Sugar Cane Farm comes out");
        h.assertValueEqual(a.fluid(net.minecraft.world.level.material.Fluids.WATER), 0L, "the water is used up");
        h.assertValueEqual(drawer.slot(0).getAmount(), 6 * BUCKET, "and the network is not refunded");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theFluidIsKeptWhenSavedAndDropsAsFilledBucketsWhenBroken(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        a.addFromNetwork();
        CompoundTag saved = a.saveWithFullMetadata(h.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(a.getBlockPos(), a.getBlockState(), saved, h.getLevel().registryAccess());
        FarmAssemblerBlockEntity b = (FarmAssemblerBlockEntity) loaded;
        h.assertValueEqual(b.fluid(net.minecraft.world.level.material.Fluids.WATER), 4 * BUCKET, "the 4 buckets of water are kept");
        h.assertValueEqual(b.counts()[lineOf(AssemblyTarget.SUGAR_CANE, Items.WATER_BUCKET)], 4, "and still count");

        h.getLevel().destroyBlock(h.absolutePos(AT), true);
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(AT)).inflate(2));
        h.assertValueEqual(drops.stream().filter(e -> e.getItem().is(Items.WATER_BUCKET)).count(), 4L, "breaking it drops the 4 buckets of water as filled buckets");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuCountsFluidAsBuckets(GameTestHelper h) {
        FarmAssemblerBlockEntity a = assembler(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        net.nexium.essentials.assembler.FarmAssemblerMenu menu = new net.nexium.essentials.assembler.FarmAssemblerMenu(5, player.getInventory(), a);
        networkedFluid(h, net.minecraft.world.level.material.Fluids.WATER, 10 * BUCKET);
        a.select(AssemblyTarget.SUGAR_CANE, null);
        int water = lineOf(AssemblyTarget.SUGAR_CANE, Items.WATER_BUCKET);
        h.assertTrue(menu.clickMenuButton(player, net.nexium.essentials.assembler.FarmAssemblerMenu.BUTTON_ADD_NETWORK), "the From network button works");
        h.assertValueEqual(menu.count(water), 4, "the screen's water bucket line shows 4 of 4");
        h.assertValueEqual(menu.needed(water), 4, "of 4 needed");
        h.succeed();
    }

}
