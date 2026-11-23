package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.nexium.energy.CellMenu;
import net.nexium.energy.CellSource;
import net.nexium.energy.ItemEnergy;
import net.nexium.essentials.slots.ExtraSlots;
import net.nexium.jetpack.JetpackFlight;
import net.nexium.registry.ModComponents;
import net.nexium.registry.ModItems;

/**
 * The Jetpack: it is worn in the wings slot without gliding, flies on the charge of the cell inside it (8 NE a tick thrusting, 4 hovering) and
 * stops when that runs out, keeps a cell in step with its own screen, and takes armor enchantments.
 */
public class JetpackGameTest {
    private static final Input JUMP = new Input(false, false, false, false, true, false, false);
    private static final Input NONE = new Input(false, false, false, false, false, false, false);

    private static ServerPlayer player(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2, 6, 2))));
        player.setOnGround(false);
        return player;
    }

    private static ItemStack jetpackWith(long charge) {
        ItemStack jetpack = new ItemStack(ModItems.JETPACK);
        ItemStack cell = new ItemStack(ModItems.IRON_CELL);
        if (charge > 0) cell.set(ModComponents.BATTERY_CHARGE, charge);
        ItemEnergy.setCell(jetpack, cell);
        return jetpack;
    }

    @GameTest
    public void theWingsSlotTakesAJetpackButItNeverGlides(GameTestHelper h) {
        ItemStack jetpack = new ItemStack(ModItems.JETPACK);
        h.assertTrue(ExtraSlots.isWings(jetpack), "the wings slot takes a Jetpack");
        h.assertTrue(!ExtraSlots.canGlideWith(jetpack), "but it does not glide like an elytra");
        h.assertTrue(new ExtraSlots().canPlaceItem(ExtraSlots.WINGS, jetpack), "and the slot accepts it");
        h.succeed();
    }

    @GameTest
    public void aJumpInTheAirStartsFlightAndEachTickCostsByWhatYouDo(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpackWith(100));
        player.setLastClientInput(NONE);
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().mayfly, "a charged Jetpack allows flight");
        h.assertTrue(!player.getAbilities().flying, "but it does not start by itself");
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().flying, "a jump in the air takes off");
        h.assertValueEqual(ItemEnergy.stored(ExtraSlots.of(player).wings()), 96L, "thrusting costs 4 NE a tick");
        player.setLastClientInput(NONE);
        JetpackFlight.tick(player);
        h.assertValueEqual(ItemEnergy.stored(ExtraSlots.of(player).wings()), 94L, "hovering costs 2");
        h.assertTrue(player.getAbilities().flying, "and it keeps flying");
        h.succeed();
    }

    @GameTest
    public void whenTheCellRunsOutFlightEndsAndYouFall(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpackWith(5));
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().flying, "it takes off with 5 NE");
        h.assertValueEqual(ItemEnergy.stored(ExtraSlots.of(player).wings()), 1L, "4 used");
        JetpackFlight.tick(player);
        h.assertTrue(!player.getAbilities().flying && !player.getAbilities().mayfly, "with 1 NE left, flight ends");
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(!player.getAbilities().mayfly, "and cannot start again without charge");
        h.succeed();
    }

    @GameTest
    public void withHoverOffFlightLastsOnlyWhileJumpIsHeld(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack jetpack = jetpackWith(1_000);
        jetpack.set(ModComponents.JETPACK_HOVER_OFF, net.minecraft.util.Unit.INSTANCE);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpack);
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().flying, "holding jump lifts off");
        h.assertValueEqual(ItemEnergy.stored(jetpack), 996L, "thrusting costs 4");
        player.setLastClientInput(NONE);
        JetpackFlight.tick(player);
        h.assertTrue(!player.getAbilities().flying, "letting go ends the flight: you drop");
        h.assertValueEqual(ItemEnergy.stored(jetpack), 996L, "and nothing is used while dropping");
        h.assertTrue(player.getAbilities().mayfly, "but the Jetpack is still ready");
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().flying, "a jump in the air takes off again");
        h.succeed();
    }

    @GameTest
    public void withTheEngineOffThereIsNoFlightAndNoDrain(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpackWith(1_000));
        ItemStack jetpack = ExtraSlots.of(player).wings();
        JetpackFlight.toggle(player, JetpackFlight.ENGINE);
        h.assertTrue(jetpack.has(ModComponents.JETPACK_ENGINE_OFF), "the engine key switches it off");
        player.setLastClientInput(JUMP);
        JetpackFlight.tick(player);
        h.assertTrue(!player.getAbilities().mayfly && !player.getAbilities().flying, "no flight with the engine off");
        h.assertValueEqual(ItemEnergy.stored(jetpack), 1_000L, "and no power used");
        JetpackFlight.toggle(player, JetpackFlight.ENGINE);
        h.assertTrue(!jetpack.has(ModComponents.JETPACK_ENGINE_OFF), "the key switches it back on");
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().flying, "and a jump in the air flies again");
        JetpackFlight.toggle(player, JetpackFlight.HOVER_SWITCH);
        h.assertTrue(jetpack.has(ModComponents.JETPACK_HOVER_OFF), "the hover key switches hover off");
        JetpackFlight.toggle(player, JetpackFlight.HOVER_SWITCH);
        h.assertTrue(!jetpack.has(ModComponents.JETPACK_HOVER_OFF), "and on again");
        h.succeed();
    }

    @GameTest
    public void takingTheJetpackOffTakesFlightAwayAndCreativeIsLeftAlone(GameTestHelper h) {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, jetpackWith(1000));
        JetpackFlight.tick(player);
        h.assertTrue(player.getAbilities().mayfly, "flight is allowed while it is worn");
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, ItemStack.EMPTY);
        JetpackFlight.tick(player);
        h.assertTrue(!player.getAbilities().mayfly, "and gone once it is taken off");
        ServerPlayer creative = player(h);
        creative.setGameMode(GameType.CREATIVE);
        ExtraSlots.of(creative).setItem(ExtraSlots.WINGS, jetpackWith(0));
        creative.getAbilities().mayfly = true;
        JetpackFlight.tick(creative);
        h.assertTrue(creative.getAbilities().mayfly, "creative flight is not touched");
        h.succeed();
    }

    @GameTest
    public void theCellScreenKeepsTheCellInTheItem(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack jetpack = new ItemStack(ModItems.JETPACK);
        player.setItemInHand(InteractionHand.MAIN_HAND, jetpack);
        CellMenu menu = new CellMenu(1, player.getInventory(), CellSource.MAIN_HAND);
        ItemStack cell = new ItemStack(ModItems.GOLD_CELL);
        cell.set(ModComponents.BATTERY_CHARGE, 5_000L);
        h.assertTrue(menu.getSlot(0).mayPlace(cell) && !menu.getSlot(0).mayPlace(new ItemStack(ModItems.JETPACK)), "the slot takes cells only");
        menu.getSlot(0).set(cell);
        h.assertValueEqual(ItemEnergy.stored(jetpack), 5_000L, "a cell put in the screen is in the Jetpack");
        ItemEnergy.extract(jetpack, 1_000, false);
        menu.broadcastChanges();
        h.assertValueEqual(ItemEnergy.stored(menu.getSlot(0).getItem()), 4_000L, "and the screen follows what the Jetpack uses");
        menu.getSlot(0).set(ItemStack.EMPTY);
        h.assertTrue(!ItemEnergy.chargeable(jetpack), "taking the cell out leaves the Jetpack with none");
        h.succeed();
    }

    /** Puts a book with this enchantment on a Jetpack at an anvil and says what came out (empty if it would not go on). */
    private static ItemStack anvil(GameTestHelper h, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, int level) {
        ServerPlayer player = player(h);
        var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        net.minecraft.world.inventory.AnvilMenu anvil = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(), h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1))));
        anvil.getSlot(0).set(new ItemStack(ModItems.JETPACK));
        anvil.getSlot(1).set(net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(
                new net.minecraft.world.item.enchantment.EnchantmentInstance(holder, level)));
        anvil.createResult();
        return anvil.getSlot(2).getItem();
    }

    private static boolean has(GameTestHelper h, ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var holder = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0;
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theJetpackTakesArmorEnchantmentsButNotDurabilityOnes(GameTestHelper h) {
        h.assertTrue(has(h, anvil(h, Enchantments.PROTECTION, 4), Enchantments.PROTECTION), "Protection goes on at an anvil");
        h.assertTrue(has(h, anvil(h, Enchantments.BLAST_PROTECTION, 4), Enchantments.BLAST_PROTECTION), "Blast Protection");
        h.assertTrue(has(h, anvil(h, Enchantments.THORNS, 3), Enchantments.THORNS), "Thorns");
        h.assertTrue(has(h, anvil(h, Enchantments.BINDING_CURSE, 1), Enchantments.BINDING_CURSE), "Curse of Binding");
        h.assertTrue(anvil(h, Enchantments.UNBREAKING, 3).isEmpty(), "not Unbreaking: it has no durability");
        h.assertTrue(anvil(h, Enchantments.SHARPNESS, 5).isEmpty(), "not a weapon enchantment");
        h.succeed();
    }
}
