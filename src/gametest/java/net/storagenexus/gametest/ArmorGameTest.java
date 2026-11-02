package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.nexium.ArmorBonus;
import net.storagenexus.nexium.NexiumArmor;
import net.storagenexus.nexium.NexusRestock;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.registry.ModItems;

/** Nexium armor's bonuses: what each gives, the full-set rule, the armor menu's switches and Nexus restock. */
public class ArmorGameTest {
    private static ServerPlayer wearing(GameTestHelper h, boolean fullSet) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NEXIUM_HELMET));
        if (fullSet) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NEXIUM_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NEXIUM_LEGGINGS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NEXIUM_BOOTS));
        }
        NexiumArmor.update(player);
        return player;
    }

    private static boolean infinite(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance e = player.getEffect(effect);
        return e != null && e.isInfiniteDuration();
    }

    @GameTest
    public void fullSetBonusesAndTheHelmet(GameTestHelper h) {
        ServerPlayer player = wearing(h, true);
        h.assertTrue(infinite(player, MobEffects.FIRE_RESISTANCE), "Nether-forged: Fire Resistance");
        h.assertTrue(infinite(player, MobEffects.NIGHT_VISION), "the helmet: Night Vision");
        h.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "no knockback, got "
                + player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

        // Without the full set: only the helmet's Night Vision
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        NexiumArmor.update(player);
        h.assertFalse(player.hasEffect(MobEffects.FIRE_RESISTANCE), "no Fire Resistance without the boots");
        h.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0, "knockback again (the armor's own 0.45)");
        h.assertTrue(infinite(player, MobEffects.NIGHT_VISION), "the helmet still gives Night Vision");
        h.assertFalse(NexiumArmor.available(player, ArmorBonus.MINERS_SET), "set bonuses need all four");
        h.succeed();
    }

    @GameTest
    public void armorMenuSwitches(GameTestHelper h) {
        ServerPlayer player = wearing(h, true);
        NexiumArmor.toggle(player, ArmorBonus.NETHER_FORGED);
        h.assertFalse(player.hasEffect(MobEffects.FIRE_RESISTANCE), "switched off: no Fire Resistance");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.BONUSES_OFF).contains(ArmorBonus.NETHER_FORGED),
                "the setting is saved on the chestplate");
        NexiumArmor.toggle(player, ArmorBonus.NIGHT_VISION);
        h.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), "Night Vision off");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).get(ModComponents.BONUSES_OFF).contains(ArmorBonus.NIGHT_VISION),
                "saved on the helmet");
        NexiumArmor.toggle(player, ArmorBonus.NO_KNOCKBACK);
        h.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0, "knockback switched back on");
        NexiumArmor.toggle(player, ArmorBonus.NETHER_FORGED);
        h.assertTrue(infinite(player, MobEffects.FIRE_RESISTANCE), "on again");
        // A potion's Fire Resistance is never taken away
        NexiumArmor.toggle(player, ArmorBonus.NETHER_FORGED);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600));
        NexiumArmor.update(player);
        h.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "the potion's stays");
        h.succeed();
    }

    @GameTest
    public void minersSetMinesAtFullSpeedInTheAir(GameTestHelper h) {
        var stone = Blocks.STONE.defaultBlockState();
        ServerPlayer plain = wearing(h, false);
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        plain.setOnGround(true);
        float onGround = plain.getDestroySpeed(stone);
        plain.setOnGround(false);
        h.assertTrue(plain.getDestroySpeed(stone) < onGround, "off the ground is slower without the set");

        ServerPlayer miner = wearing(h, true);
        miner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        miner.setOnGround(false);
        h.assertTrue(Math.abs(miner.getDestroySpeed(stone) - onGround) < 1e-4, "with the full set, as fast as on the ground");
        NexiumArmor.toggle(miner, ArmorBonus.MINERS_SET);
        h.assertTrue(miner.getDestroySpeed(stone) < onGround, "switched off: slow again");
        h.succeed();
    }

    private static final BlockPos TERMINAL = new BlockPos(1, 1, 1), CONNECTOR = new BlockPos(2, 1, 1), CHEST = new BlockPos(3, 1, 1);

    private static int count(ChestBlockEntity chest, Item item) {
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(item)) n += chest.getItem(i).getCount();
        return n;
    }

    @GameTest
    public void nexusRestockTopsUpTheHotbarAndFeeds(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.TORCH, 64));
        chest.setItem(1, new ItemStack(Items.BREAD, 5));
        chest.setItem(2, new ItemStack(Items.GOLDEN_APPLE, 5));
        chest.setItem(3, new ItemStack(Items.ROTTEN_FLESH, 5));

        ServerPlayer player = wearing(h, true);
        player.getItemBySlot(EquipmentSlot.CHEST).set(ModComponents.TERMINAL_LINK,
                GlobalPos.of(h.getLevel().dimension(), h.absolutePos(TERMINAL)));
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 10));
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 40));
        NexusRestock.tick(player, false);

        // Used down to 9: not low enough yet
        player.getInventory().getItem(0).shrink(1);
        NexusRestock.tick(player, false);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 9, "9 torches: no top-up yet");
        // Used down to 8: topped up to a full stack from the network
        player.getInventory().getItem(0).shrink(1);
        NexusRestock.tick(player, false);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 64, "topped up to 64");
        h.assertValueEqual(count(chest, Items.TORCH), 64 - 56, "56 torches taken from the network");

        // A whole stack moved away is not refilled
        player.getInventory().setItem(1, ItemStack.EMPTY);
        NexusRestock.tick(player, false);
        h.assertTrue(player.getInventory().getItem(1).isEmpty(), "a stack moved away is left alone");

        // Hungry: eats bread, never the golden apple or the rotten flesh
        player.getFoodData().setFoodLevel(10);
        NexusRestock.tick(player, true);
        h.assertTrue(player.getFoodData().getFoodLevel() > 10, "ate something, food " + player.getFoodData().getFoodLevel());
        h.assertValueEqual(count(chest, Items.BREAD), 4, "one bread eaten");
        h.assertValueEqual(count(chest, Items.GOLDEN_APPLE), 5, "golden apples left alone");
        h.assertValueEqual(count(chest, Items.ROTTEN_FLESH), 5, "rotten flesh left alone");

        // Switched off: nothing
        NexiumArmor.toggle(player, ArmorBonus.NEXUS_RESTOCK);
        player.getInventory().setItem(0, new ItemStack(Items.TORCH, 10));
        NexusRestock.tick(player, false);
        player.getInventory().getItem(0).shrink(2);
        NexusRestock.tick(player, false);
        h.assertValueEqual(player.getInventory().getItem(0).getCount(), 8, "no top-up when switched off");
        h.succeed();
    }

    @GameTest
    public void restockLooksAgainLaterWhenThereIsNothingToEat(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        h.setBlock(CONNECTOR, ModBlocks.STORAGE_CONNECTOR);
        h.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity chest = h.getBlockEntity(CHEST, ChestBlockEntity.class);
        NexusRestock.reset(null);
        ServerPlayer player = wearing(h, true);
        player.getItemBySlot(EquipmentSlot.CHEST).set(ModComponents.TERMINAL_LINK,
                GlobalPos.of(h.getLevel().dimension(), h.absolutePos(TERMINAL)));
        player.getFoodData().setFoodLevel(6);
        NexusRestock.tick(player, true); // nothing to eat in the network
        chest.setItem(0, new ItemStack(Items.BREAD, 5));
        net.storagenexus.network.StorageNetwork.clearCache();
        NexusRestock.tick(player, true);
        h.assertValueEqual(player.getFoodData().getFoodLevel(), 6, "still waiting before it looks again");
        NexusRestock.reset(player.getUUID()); // the wait is forgotten, as when the armor comes off
        NexusRestock.tick(player, true);
        h.assertTrue(player.getFoodData().getFoodLevel() > 6, "eats once it looks again");
        h.succeed();
    }

    @GameTest
    public void chestplateLinksToATerminal(GameTestHelper h) {
        h.setBlock(TERMINAL, ModBlocks.CRAFTING_TERMINAL);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEXIUM_CHESTPLATE));
        player.setShiftKeyDown(true);
        BlockPos abs = h.absolutePos(TERMINAL);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        GlobalPos link = player.getMainHandItem().get(ModComponents.TERMINAL_LINK);
        h.assertTrue(link != null && link.pos().equals(abs), "sneak + right-click links the chestplate, got " + link);
        h.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "and doesn't put it on");
        h.succeed();
    }
}
