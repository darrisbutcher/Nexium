package net.nexium.gametest;

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
import net.nexium.gear.ArmorBonus;
import net.nexium.gear.NexiumArmor;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;

/** Nexium armor's bonuses: what each gives, the full-set rule and the armor menu's switches. */
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
        h.assertTrue(infinite(player, MobEffects.WATER_BREATHING), "the full set: Water Breathing");
        h.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "no knockback, got "
                + player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));

        // Without the full set: only the helmet's Night Vision
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        NexiumArmor.update(player);
        h.assertFalse(player.hasEffect(MobEffects.FIRE_RESISTANCE), "no Fire Resistance without the boots");
        h.assertFalse(player.hasEffect(MobEffects.WATER_BREATHING), "and no Water Breathing without the boots");
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
        NexiumArmor.toggle(player, ArmorBonus.WATER_BREATHING);
        h.assertFalse(player.hasEffect(MobEffects.WATER_BREATHING), "Water Breathing off");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.BONUSES_OFF).contains(ArmorBonus.WATER_BREATHING), "saved on the chestplate");
        NexiumArmor.toggle(player, ArmorBonus.WATER_BREATHING);
        h.assertTrue(infinite(player, MobEffects.WATER_BREATHING), "and on again");
        // A potion's Water Breathing is never taken away
        NexiumArmor.toggle(player, ArmorBonus.WATER_BREATHING);
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600));
        NexiumArmor.update(player);
        h.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING), "the potion's stays");
        NexiumArmor.toggle(player, ArmorBonus.WATER_BREATHING);
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

    @GameTest
    public void anOldChestplateWithTheRemovedNexusRestockBonusStillLoads(GameTestHelper h) {
        var json = new com.google.gson.JsonArray();
        json.add("nexus_restock");
        json.add("water_breathing");
        var parsed = ArmorBonus.SAVED_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json);
        h.assertTrue(parsed.result().isPresent(), "a list holding an id this version no longer has still decodes");
        h.assertValueEqual(parsed.result().get(), java.util.List.of(ArmorBonus.WATER_BREATHING), "and keeps the bonuses it still has");
        h.assertTrue(ArmorBonus.byId("nexus_restock") == null, "Nexus restock is gone");
        h.succeed();
    }
}
