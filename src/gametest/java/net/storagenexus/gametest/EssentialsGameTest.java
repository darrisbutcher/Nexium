package net.storagenexus.gametest;

import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.magnet.MagnetItem;
import net.storagenexus.essentials.magnet.MagnetLogic;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModEffects;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.slots.ExtraSlots;
import net.storagenexus.registry.ModTab;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EssentialsGameTest {
    private static final BlockPos CENTER = new BlockPos(4, 2, 4);

    // ---------------------------------------------------------------- helpers

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        ExtraSlots.of(player).clearContent();
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(CENTER)));
        return player;
    }

    /**
     * Passes once the world can see every given entity. The magnet tests put items up to 12 blocks from the player,
     * outside the 8x8x8 test area, and only the area's own chunks are loaded when a test starts: an entity added to a
     * chunk that isn't loaded yet exists, but no lookup finds it until the chunk loads, so a magnet couldn't pull it.
     */
    private static void waitUntilLoaded(GameTestHelper h, Entity... entities) {
        for (Entity e : entities) h.assertTrue(h.getLevel().getEntity(e.getId()) == e, "waiting for the chunk at " + e.blockPosition() + " to load");
    }

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchanted(GameTestHelper h, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(enchantment(h, key), level);
        return stack;
    }

    private static void fillBox(GameTestHelper h, BlockPos from, BlockPos to, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) h.setBlock(pos, block);
    }

    private static int countBlocks(GameTestHelper h, BlockPos from, BlockPos to, Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) if (h.getBlockState(pos).is(block)) n++;
        return n;
    }

    // ---------------------------------------------------------------- creepers, ghasts, endermen

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void creeperExplosionKeepsBlocksAndDecorations(GameTestHelper h) {
        fillBox(h, new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), Blocks.STONE);
        fillBox(h, new BlockPos(2, 2, 6), new BlockPos(6, 4, 6), Blocks.STONE);
        Creeper creeper = h.spawn(EntityTypes.CREEPER, CENTER);
        ItemEntity item = h.spawn(EntityTypes.ITEM, CENTER.east());
        item.setItem(new ItemStack(Items.DIAMOND));
        ArmorStand stand = h.spawn(EntityTypes.ARMOR_STAND, CENTER.west());
        Pig pig = h.spawn(EntityTypes.PIG, CENTER.north());
        Vec3 at = creeper.position();
        h.getLevel().explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.MOB);
        h.assertValueEqual(countBlocks(h, new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), Blocks.STONE), 25, "floor blocks left");
        h.assertValueEqual(countBlocks(h, new BlockPos(2, 2, 6), new BlockPos(6, 4, 6), Blocks.STONE), 15, "wall blocks left");
        h.assertTrue(item.isAlive(), "the dropped diamond survived");
        h.assertTrue(stand.isAlive(), "the armor stand survived");
        h.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig still got hurt");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void tntStillBreaksBlocks(GameTestHelper h) {
        fillBox(h, new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), Blocks.STONE);
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(CENTER));
        h.getLevel().explode(null, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.TNT);
        h.assertTrue(countBlocks(h, new BlockPos(2, 1, 2), new BlockPos(6, 1, 6), Blocks.STONE) < 25, "TNT still breaks blocks");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 40)
    public void ghastFireballKeepsBlocksAndStartsNoFire(GameTestHelper h) {
        fillBox(h, new BlockPos(1, 1, 1), new BlockPos(7, 1, 7), Blocks.OAK_PLANKS);
        LargeFireball fireball = new LargeFireball(EntityTypes.FIREBALL, h.getLevel());
        Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(CENTER));
        fireball.setPos(at);
        h.getLevel().explode(fireball, at.x, at.y, at.z, 2.0F, true, Level.ExplosionInteraction.MOB);
        h.assertValueEqual(countBlocks(h, new BlockPos(1, 1, 1), new BlockPos(7, 1, 7), Blocks.OAK_PLANKS), 49, "planks left");
        h.assertValueEqual(countBlocks(h, new BlockPos(1, 2, 1), new BlockPos(7, 2, 7), Blocks.FIRE), 0, "fires started");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void endermenNeverTakeBlocks(GameTestHelper h) throws ReflectiveOperationException {
        Enderman enderman = h.spawn(EntityTypes.ENDERMAN, CENTER);
        Class<?> goalClass = Class.forName("net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal");
        var constructor = goalClass.getDeclaredConstructor(Enderman.class);
        constructor.setAccessible(true);
        var goal = (net.minecraft.world.entity.ai.goal.Goal) constructor.newInstance(enderman);
        for (int i = 0; i < 500; i++) h.assertTrue(!goal.canUse(), "the take-block goal never starts");
        h.succeed();
    }

    // ---------------------------------------------------------------- piglins

    @GameTest(environment = EssentialsEnvironment.ID)
    public void netheriteArmorCalmsPiglins(GameTestHelper h) {
        ServerPlayer player = player(h);
        h.assertTrue(!PiglinAi.isWearingSafeArmor(player), "no armor: piglins attack");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        h.assertTrue(PiglinAi.isWearingSafeArmor(player), "netherite boots count as gold");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        h.assertTrue(!PiglinAi.isWearingSafeArmor(player), "diamond boots don't");
        h.succeed();
    }

    // ---------------------------------------------------------------- magnet

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void magnetPullsItemsAndXpInRange(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        Vec3 base = player.position();
        ItemEntity near = new ItemEntity(h.getLevel(), base.x + 6, base.y, base.z, new ItemStack(Items.EMERALD, 3));
        ItemEntity far = new ItemEntity(h.getLevel(), base.x + 12, base.y, base.z, new ItemStack(Items.GOLD_INGOT));
        ItemEntity thrown = new ItemEntity(h.getLevel(), base.x - 4, base.y, base.z, new ItemStack(Items.APPLE));
        thrown.setThrower(player);
        for (ItemEntity e : List.of(near, far, thrown)) {
            e.setNoPickUpDelay();
            h.getLevel().addFreshEntity(e);
        }
        ExperienceOrb orb = new ExperienceOrb(h.getLevel(), base.x, base.y, base.z + 5, 7);
        h.getLevel().addFreshEntity(orb);

        // The magnet goes on only once the world can see everything: see waitUntilLoaded
        h.startSequence().thenWaitUntil(() -> waitUntilLoaded(h, near, far, thrown, orb)).thenExecute(() -> {
            ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, magnet);
            int xpBefore = player.totalExperience;
            MagnetLogic.pull(player);
            h.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 3, "emeralds 6 blocks away pulled in");
            h.assertValueEqual(player.getInventory().countItem(Items.GOLD_INGOT), 0, "gold 12 blocks away left alone (range 8)");
            h.assertValueEqual(player.getInventory().countItem(Items.APPLE), 0, "the player's own thrown apple left alone");
            h.assertTrue(player.totalExperience > xpBefore, "XP orb collected");

            magnet.set(ModComponents.MAGNET_RANGE, 16);
            MagnetLogic.pull(player);
            h.assertValueEqual(player.getInventory().countItem(Items.GOLD_INGOT), 1, "gold pulled in at range 16");
        }).thenSucceed();
    }

    @GameTest(environment = EssentialsEnvironment.ID, maxTicks = 100)
    public void magnetOnlyWorksInTrinketSlotAndWhenOn(GameTestHelper h) {
        ServerPlayer player = player(h);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        player.getInventory().setItem(5, magnet);
        Vec3 base = player.position();
        ItemEntity item = new ItemEntity(h.getLevel(), base.x + 5, base.y, base.z, new ItemStack(Items.EMERALD));
        item.setNoPickUpDelay();
        h.getLevel().addFreshEntity(item);
        h.startSequence().thenWaitUntil(() -> waitUntilLoaded(h, item)).thenExecute(() -> magnetOnlyWorksInTrinketSlotAndWhenOn(h, player, magnet)).thenSucceed();
    }

    private static void magnetOnlyWorksInTrinketSlotAndWhenOn(GameTestHelper h, ServerPlayer player, ItemStack magnet) {
        MagnetLogic.pull(player);
        h.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 0, "a magnet in the backpack does nothing");

        player.getInventory().setItem(5, ItemStack.EMPTY);
        ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, magnet);
        MagnetLogic.toggle(player);
        h.assertTrue(!MagnetItem.isActive(magnet), "the hotkey switched it off");
        MagnetLogic.pull(player);
        h.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 0, "a switched-off magnet does nothing");
        MagnetLogic.toggle(player);
        MagnetLogic.pull(player);
        h.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 1, "switched back on, it pulls");
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void magnetRangeCycles(GameTestHelper h) {
        h.assertValueEqual(MagnetItem.nextRange(4), 8, "4 -> 8");
        h.assertValueEqual(MagnetItem.nextRange(8), 12, "8 -> 12");
        h.assertValueEqual(MagnetItem.nextRange(12), 16, "12 -> 16");
        h.assertValueEqual(MagnetItem.nextRange(16), 4, "16 wraps to 4");

        ServerPlayer player = player(h);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNET));
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertValueEqual(MagnetItem.range(player.getMainHandItem()), 12, "sneak + right-click raised the range");

        player.setShiftKeyDown(false);
        player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(ExtraSlots.of(player).trinket().is(ModItems.MAGNET), "right-click put the magnet in the trinket slot");
        h.assertTrue(player.getMainHandItem().isEmpty(), "and emptied the hand");
        h.succeed();
    }

    // ---------------------------------------------------------------- trinket and wings slots

    @GameTest(environment = EssentialsEnvironment.ID)
    public void inventoryHasTheTwoSlots(GameTestHelper h) {
        ServerPlayer player = player(h);
        h.assertValueEqual(player.inventoryMenu.slots.size(), 48, "inventory menu slots");
        h.assertTrue(player.inventoryMenu.getSlot(ExtraSlots.MENU_WINGS).mayPlace(new ItemStack(Items.ELYTRA)), "elytra fits the wings slot");
        h.assertTrue(!player.inventoryMenu.getSlot(ExtraSlots.MENU_WINGS).mayPlace(new ItemStack(Items.DIAMOND_CHESTPLATE)), "a chestplate doesn't");
        h.assertTrue(player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET).mayPlace(new ItemStack(ModItems.MAGNET)), "the magnet fits the trinket slot");
        h.assertTrue(!player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET).mayPlace(new ItemStack(Items.DIAMOND)), "a diamond doesn't");

        // Shift-click from the inventory goes to the new slots first
        player.getInventory().setItem(9, new ItemStack(Items.ELYTRA));
        player.getInventory().setItem(10, new ItemStack(ModItems.MAGNET));
        player.inventoryMenu.quickMoveStack(player, 9);
        player.inventoryMenu.quickMoveStack(player, 10);
        h.assertTrue(ExtraSlots.of(player).wings().is(Items.ELYTRA), "shift-click moved the elytra to the wings slot");
        h.assertTrue(ExtraSlots.of(player).trinket().is(ModItems.MAGNET), "shift-click moved the magnet to the trinket slot");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void rightClickingElytraWearsItInWingsSlot(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ELYTRA));
        InteractionResult result = UseItemCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND);
        h.assertTrue(result.consumesAction(), "right-click was handled (" + result + ")");
        h.assertTrue(ExtraSlots.of(player).wings().is(Items.ELYTRA), "elytra went to the wings slot");
        h.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE), "chestplate stayed on");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void creativeCanSetTheNewSlots(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(ExtraSlots.MENU_WINGS, new ItemStack(Items.ELYTRA)));
        player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(ExtraSlots.MENU_TRINKET, new ItemStack(Items.STONE)));
        h.assertTrue(ExtraSlots.of(player).wings().is(Items.ELYTRA), "creative put an elytra in the wings slot");
        h.assertTrue(ExtraSlots.of(player).trinket().isEmpty(), "but not stone in the trinket slot");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void wingsSlotGlidesUnderAChestplate(GameTestHelper h) {
        ServerPlayer player = player(h);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        player.setOnGround(false);
        h.assertTrue(!player.tryToStartFallFlying(), "a chestplate alone can't glide");

        ItemStack elytra = new ItemStack(Items.ELYTRA);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, elytra);
        h.assertTrue(player.tryToStartFallFlying(), "chestplate + wings slot elytra glides");
        h.assertTrue(player.isFallFlying(), "is gliding");

        int damage = elytra.getDamageValue();
        h.assertTrue(EntityElytraEvents.CUSTOM.invoker().useCustomElytra(player, true), "a second of flight is allowed");
        h.assertTrue(elytra.getDamageValue() == damage + 1 || elytra.getDamageValue() == damage, "the wings take normal elytra wear");

        elytra.setDamageValue(elytra.getMaxDamage() - 1);
        player.stopFallFlying();
        h.assertTrue(!player.tryToStartFallFlying(), "a broken elytra stops gliding, like vanilla");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void wingsSlotProtectionAndMendingCount(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        float without = EnchantmentHelper.getDamageProtection(level, player, level.damageSources().generic());
        ItemStack elytra = enchanted(h, new ItemStack(Items.ELYTRA), Enchantments.PROTECTION, 4);
        enchanted(h, elytra, Enchantments.MENDING, 1);
        elytra.setDamageValue(100);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, elytra);
        float with = EnchantmentHelper.getDamageProtection(level, player, level.damageSources().generic());
        h.assertTrue(with > without, "Protection IV on the wings slot elytra counts (" + without + " -> " + with + ")");

        Optional<EnchantedItemInUse> mended = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
        h.assertTrue(mended.isPresent() && mended.get().itemStack() == elytra, "Mending picks the wings slot elytra");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void extraSlotsDropOnDeathAndSave(GameTestHelper h) throws ReflectiveOperationException {
        ServerPlayer player = player(h);
        ExtraSlots.of(player).setItem(ExtraSlots.WINGS, new ItemStack(Items.ELYTRA));
        ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, new ItemStack(ModItems.MAGNET));

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        player.saveWithoutId(output);
        ExtraSlots loaded = new ExtraSlots();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(loaded.wings().is(Items.ELYTRA) && loaded.trinket().is(ModItems.MAGNET), "both slots saved with the player");

        var dropEquipment = Player.class.getDeclaredMethod("dropEquipment", ServerLevel.class);
        dropEquipment.setAccessible(true);
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, true, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(!ExtraSlots.of(player).wings().isEmpty(), "keepInventory keeps them");
        h.getLevel().getGameRules().set(GameRules.KEEP_INVENTORY, false, h.getLevel().getServer());
        dropEquipment.invoke(player, h.getLevel());
        h.assertTrue(ExtraSlots.of(player).wings().isEmpty() && ExtraSlots.of(player).trinket().isEmpty(), "dying empties them");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.ELYTRA)), "the elytra dropped");
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.MAGNET)), "the magnet dropped");
        h.succeed();
    }

    // ---------------------------------------------------------------- elytra enchanting

    @GameTest(environment = EssentialsEnvironment.ID)
    public void anvilPutsEveryProtectionOnElytra(GameTestHelper h) {
        ServerPlayer player = player(h);
        AnvilMenu anvil = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(CENTER)));
        ItemStack elytra = enchanted(h, new ItemStack(Items.ELYTRA), Enchantments.PROTECTION, 4);
        anvil.getSlot(0).set(elytra);
        anvil.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment(h, Enchantments.FIRE_PROTECTION), 4)));
        anvil.createResult();
        ItemStack result = anvil.getSlot(2).getItem();
        h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment(h, Enchantments.FIRE_PROTECTION), result) == 4
                && EnchantmentHelper.getItemEnchantmentLevel(enchantment(h, Enchantments.PROTECTION), result) == 4, "elytra with Protection IV + Fire Protection IV: " + result.getEnchantments());

        // A plain elytra takes Blast Protection from a book
        anvil.getSlot(0).set(new ItemStack(Items.ELYTRA));
        anvil.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment(h, Enchantments.BLAST_PROTECTION), 3)));
        anvil.createResult();
        h.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(h, Enchantments.BLAST_PROTECTION), anvil.getSlot(2).getItem()), 3, "Blast Protection on a plain elytra");

        // Chestplates keep the vanilla one-protection rule
        anvil.getSlot(0).set(enchanted(h, new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 4));
        anvil.getSlot(1).set(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment(h, Enchantments.FIRE_PROTECTION), 4)));
        anvil.createResult();
        h.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(h, Enchantments.FIRE_PROTECTION), anvil.getSlot(2).getItem()), 0, "no Fire Protection added to a Protection chestplate");
        h.succeed();
    }

    // ---------------------------------------------------------------- bedrock

    @GameTest(environment = EssentialsEnvironment.ID)
    public void elixirLetsADiamondPickaxeMineBedrock(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos rel = CENTER.east(2);
        h.setBlock(rel, Blocks.BEDROCK);
        BlockPos pos = h.absolutePos(rel);
        var bedrock = h.getLevel().getBlockState(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertValueEqual(bedrock.getDestroyProgress(player, h.getLevel(), pos), 0.0F, "no effect: unbreakable");

        ItemStack elixir = new ItemStack(ModItems.BEDROCK_BREAKER_ELIXIR);
        elixir.finishUsingItem(h.getLevel(), player);
        MobEffectInstance effect = player.getEffect(ModEffects.BEDROCK_BREAKER);
        h.assertTrue(effect != null && effect.getDuration() == 300 * 20, "drinking gives 5 minutes of Bedrock Breaker (" + effect + ")");

        float obsidian = Blocks.OBSIDIAN.defaultBlockState().getDestroyProgress(player, h.getLevel(), pos);
        h.assertValueEqual(bedrock.getDestroyProgress(player, h.getLevel(), pos), obsidian, "bedrock mines like obsidian");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        h.assertValueEqual(bedrock.getDestroyProgress(player, h.getLevel(), pos), 0.0F, "an iron pickaxe can't");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        player.gameMode.destroyBlock(pos);
        h.assertBlockPresent(Blocks.AIR, rel);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "bedrock drops nothing");
        h.succeed();
    }

    // ---------------------------------------------------------------- picking up mobs

    private static InteractionResult sneakClick(GameTestHelper h, ServerPlayer player, net.minecraft.world.entity.Entity target) {
        player.setShiftKeyDown(true);
        return UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, target,
                new EntityHitResult(target, target.position()));
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void pickUpAndReleaseAMob(GameTestHelper h) {
        ServerPlayer player = player(h);
        Cow cow = h.spawn(EntityTypes.COW, CENTER.east());
        cow.setCustomName(Component.literal("Bessie"));
        cow.setHealth(5.0F);
        cow.setAge(-12000);
        UUID uuid = cow.getUUID();

        h.assertTrue(sneakClick(h, player, cow).consumesAction(), "sneak + empty hand picks the cow up");
        h.assertTrue(cow.isRemoved(), "the cow left the world");
        ItemStack item = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.CAPTURED_MOB)) item = player.getInventory().getItem(i);
        }
        CapturedMob captured = item.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(captured != null && captured.type() == EntityTypes.COW, "the item holds a cow");
        h.assertTrue(item.getHoverName().getString().contains("Bessie"), "named after it: " + item.getHoverName().getString());
        h.assertTrue(Items.COW_SPAWN_EGG.components().get(DataComponents.ITEM_MODEL).equals(item.get(DataComponents.ITEM_MODEL)), "looks like a cow spawn egg");

        BlockPos floor = CENTER.west(2).below();
        h.setBlock(floor, Blocks.STONE);
        BlockPos abs = h.absolutePos(floor);
        player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        InteractionResult released = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        h.assertTrue(released.consumesAction(), "right-click on a block lets it out (" + released + ")");
        List<Cow> cows = h.getLevel().getEntitiesOfClass(Cow.class, new AABB(abs.above()).inflate(1), c -> uuid.equals(c.getUUID()));
        h.assertValueEqual(cows.size(), 1, "the same cow is back");
        Cow back = cows.get(0);
        h.assertValueEqual(back.getHealth(), 5.0F, "same health");
        h.assertTrue(back.isBaby(), "still a calf");
        h.assertTrue("Bessie".equals(back.getCustomName().getString()), "same name");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void cannotPickUpBossesOrOthersPets(GameTestHelper h) {
        ServerPlayer player = player(h);
        WitherBoss wither = h.spawn(EntityTypes.WITHER, CENTER.east(3));
        h.assertTrue(sneakClick(h, player, wither) == InteractionResult.PASS && wither.isAlive(), "the Wither can't be picked up");
        wither.discard();

        Wolf wolf = h.spawn(EntityTypes.WOLF, CENTER.west());
        wolf.tame(h.makeMockServerPlayerInLevel());
        h.assertTrue(sneakClick(h, player, wolf) == InteractionResult.PASS && wolf.isAlive(), "someone else's wolf can't be picked up");
        wolf.tame(player);
        h.assertTrue(sneakClick(h, player, wolf).consumesAction() && wolf.isRemoved(), "your own wolf can");

        Pig pig = h.spawn(EntityTypes.PIG, CENTER.north());
        player.setShiftKeyDown(false);
        h.assertTrue(UseEntityCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, pig,
                new EntityHitResult(pig)) == InteractionResult.PASS, "not sneaking: no pick-up");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        h.assertTrue(sneakClick(h, player, pig) == InteractionResult.PASS, "holding something: no pick-up");
        h.succeed();
    }

    // ---------------------------------------------------------------- ancient debris

    @GameTest(environment = EssentialsEnvironment.ID)
    public void ancientDebrisDropsScrapWithFortune(GameTestHelper h) {
        ServerPlayer player = player(h);
        BlockPos pos = h.absolutePos(CENTER);
        var debris = Blocks.ANCIENT_DEBRIS.defaultBlockState();

        List<ItemStack> plain = Block.getDrops(debris, h.getLevel(), pos, null, player, new ItemStack(Items.NETHERITE_PICKAXE));
        h.assertTrue(plain.size() == 1 && plain.get(0).is(Items.NETHERITE_SCRAP) && plain.get(0).getCount() == 2, "no Fortune: 2 scrap, got " + plain);

        ItemStack fortune = enchanted(h, new ItemStack(Items.NETHERITE_PICKAXE), Enchantments.FORTUNE, 3);
        int total = 0;
        int most = 0;
        for (int i = 0; i < 200; i++) {
            int n = Block.getDrops(debris, h.getLevel(), pos, null, player, fortune).stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(n >= 2 && n <= 8 && n % 2 == 0, "Fortune III gives 2, 4, 6 or 8 scrap, got " + n);
            total += n;
            most = Math.max(most, n);
        }
        h.assertTrue(total / 200.0 > 3.4 && most == 8, "Fortune III averages about 4.4 (" + total / 200.0 + "), max " + most);

        List<ItemStack> silk = Block.getDrops(debris, h.getLevel(), pos, null, player, enchanted(h, new ItemStack(Items.NETHERITE_PICKAXE), Enchantments.SILK_TOUCH, 1));
        h.assertTrue(silk.size() == 1 && silk.get(0).is(Items.ANCIENT_DEBRIS), "Silk Touch keeps the block, got " + silk);
        h.succeed();
    }

    // ---------------------------------------------------------------- creative tab

    /** Since the merge, the Essentials items are in the Storage Nexus creative tab, after its own items, and not in vanilla's. */
    @GameTest(environment = EssentialsEnvironment.ID)
    public void essentialsItemsAreInTheStorageNexusTab(GameTestHelper h) {
        CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        List<ResourceKey<CreativeModeTab>> vanillaTabs = List.of(CreativeModeTabs.TOOLS_AND_UTILITIES,
                CreativeModeTabs.FOOD_AND_DRINKS, CreativeModeTabs.FUNCTIONAL_BLOCKS);
        for (Item item : List.of(ModItems.MAGNET, ModItems.BEDROCK_BREAKER_ELIXIR, ModItems.IRON_FARM)) {
            ItemStack stack = new ItemStack(item);
            h.assertTrue(ModTab.TAB.contains(stack), item + " is in the Storage Nexus tab");
            for (ResourceKey<CreativeModeTab> tab : vanillaTabs) {
                h.assertFalse(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tab).contains(stack), item + " is not in " + tab.identifier());
            }
        }
        h.assertFalse(ModTab.TAB.contains(new ItemStack(ModItems.CAPTURED_MOB)), "an empty Captured Mob isn't offered");
        h.assertTrue(ModTab.TAB.getDisplayItems().iterator().next().is(net.storagenexus.registry.ModItems.GUIDE), "the guide still comes first");
        h.succeed();
    }
}
