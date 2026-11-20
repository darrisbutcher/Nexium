package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.Vec3;
import net.nexium.dog.DogEntity;
import net.nexium.dog.ModDogs;
import net.nexium.essentials.capture.CapturedMob;
import net.nexium.essentials.capture.MobCapture;
import net.nexium.essentials.registry.ModComponents;
import net.nexium.registry.ModItems;

import java.util.List;

/** The dog: nothing in the world kills it, creepers keep away and cannot blow up near it, its egg is craftable and lets out a tame green-collared Zoe, and it wears armor. */
public class DogGameTest {
    private static DogEntity dog(GameTestHelper h, BlockPos pos, ServerPlayer owner) {
        DogEntity dog = ModDogs.DOG.create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(pos));
        dog.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        if (owner != null) dog.tame(owner);
        dog.setPersistenceRequired();
        h.getLevel().addFreshEntity(dog);
        return dog;
    }

    @GameTest(maxTicks = 40)
    public void nothingInTheWorldKillsTheDog(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        DogEntity dog = dog(h, new BlockPos(2, 2, 2), player);
        ServerLevel level = h.getLevel();
        Zombie zombie = new Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, level);
        List<DamageSource> harmless = List.of(level.damageSources().mobAttack(zombie), level.damageSources().fall(), level.damageSources().onFire(),
                level.damageSources().inFire(), level.damageSources().lava(), level.damageSources().drown(), level.damageSources().generic());
        float health = dog.getHealth();
        for (DamageSource source : harmless) {
            dog.hurtServer(level, source, 1000.0F);
            h.assertTrue(dog.isAlive() && dog.getHealth() == health, "no harm from " + source.getMsgId() + ", health " + dog.getHealth());
        }
        h.assertTrue(!dog.causeFallDamage(30.0, 1.0F, level.damageSources().fall()), "a fall does nothing");
        // The owner's own blow can hurt, but never kill
        dog.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        h.assertTrue(dog.isAlive() && dog.getHealth() > 0, "a player's blow never kills: health " + dog.getHealth());
        // The kill command and the void still can
        dog.hurtServer(level, level.damageSources().genericKill(), 1000.0F);
        h.assertTrue(!dog.isAlive(), "the kill command still removes it");
        h.succeed();
    }

    /** Monsters are removed on Peaceful, which would look like an explosion: make sure the world is not. */
    private static net.minecraft.world.Difficulty notPeaceful(GameTestHelper h) {
        var server = h.getLevel().getServer();
        net.minecraft.world.Difficulty before = h.getLevel().getDifficulty();
        if (before == net.minecraft.world.Difficulty.PEACEFUL) server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        return before;
    }

    private static void restore(GameTestHelper h, net.minecraft.world.Difficulty before) {
        if (before == net.minecraft.world.Difficulty.PEACEFUL) h.getLevel().getServer().setDifficulty(before, true);
    }

    @GameTest(maxTicks = 120)
    public void aCreeperNearTheDogCannotBlowUp(GameTestHelper h) {
        net.minecraft.world.Difficulty before = notPeaceful(h);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        DogEntity dog = dog(h, new BlockPos(3, 2, 3), player);
        dog.setNoAi(true);
        Creeper creeper = net.minecraft.world.entity.EntityTypes.CREEPER.create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(5, 2, 3)));
        creeper.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        creeper.setNoAi(true);
        creeper.setPersistenceRequired(); // the mock player stands far away, so it would otherwise despawn at once
        h.getLevel().addFreshEntity(creeper);
        creeper.ignite();
        h.runAfterDelay(80, () -> {
            h.assertTrue(!creeper.isRemoved() && creeper.isAlive(), "the creeper did not explode beside the dog (removed: " + creeper.getRemovalReason() + ")");
            h.assertTrue(dog.isAlive(), "and the dog is fine");
            restore(h, before);
            h.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void aCreeperAwayFromTheDogStillExplodes(GameTestHelper h) {
        // The control: the same ignited creeper with no dog about does go off
        net.minecraft.world.Difficulty before = notPeaceful(h);
        Creeper creeper = net.minecraft.world.entity.EntityTypes.CREEPER.create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(5, 2, 3)));
        creeper.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        creeper.setNoAi(true);
        creeper.setPersistenceRequired(); // the mock player stands far away, so it would otherwise despawn at once
        h.getLevel().addFreshEntity(creeper);
        creeper.ignite();
        h.succeedWhen(() -> {
            h.assertTrue(creeper.isRemoved(), "an ignited creeper with no dog near explodes");
            restore(h, before);
        });
    }

    @GameTest(maxTicks = 20)
    public void theEggLetsOutATameGreenCollaredZoe(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack egg = ModDogs.zoeEgg();
        CapturedMob captured = egg.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(captured != null && captured.type() == ModDogs.DOG, "the egg holds a dog");
        h.assertTrue(egg.getHoverName().getString().equals("Zoe's Egg"), "it is called Zoe's Egg, not " + egg.getHoverName().getString());
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        h.assertTrue(MobCapture.release(h.getLevel(), captured, at, 0.0F, player), "the dog is let out");
        DogEntity dog = h.getLevel().getEntitiesOfClass(DogEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(3.0)).stream().findFirst().orElse(null);
        h.assertTrue(dog != null, "there is a dog");
        h.assertTrue(dog.isTame() && dog.isOwnedBy(player), "tame, and the player's");
        h.assertTrue(dog.getCollarColor() == DyeColor.GREEN, "with a green collar, not " + dog.getCollarColor());
        h.assertTrue(dog.hasCustomName() && dog.getCustomName().getString().equals("Zoe"), "called Zoe");
        h.assertTrue(dog.getHealth() == dog.getMaxHealth(), "in full health");
        // It can be picked up again as an egg
        ItemStack back = MobCapture.capture(dog);
        h.assertTrue(back.get(ModComponents.CAPTURED_MOB).type() == ModDogs.DOG && back.getHoverName().getString().equals("Zoe's Egg"), "and goes back into an egg");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void theEggIsCraftedFromBonesAndLeather(GameTestHelper h) {
        RecipeHolder<?> holder = h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("nexium:dog_egg"))).orElse(null);
        h.assertTrue(holder != null && holder.value() instanceof ShapedRecipe, "the Dog Egg recipe is loaded");
        ItemStack bone = new ItemStack(Items.BONE), leather = new ItemStack(Items.LEATHER);
        CraftingInput input = CraftingInput.of(3, 3, List.of(bone, leather, bone, leather, bone, leather, bone, leather, bone));
        ShapedRecipe recipe = (ShapedRecipe) holder.value();
        h.assertTrue(recipe.matches(input, h.getLevel()), "5 bones and 4 leather make it");
        ItemStack out = recipe.assemble(input);
        CapturedMob captured = out.get(ModComponents.CAPTURED_MOB);
        h.assertTrue(captured != null && captured.type() == ModDogs.DOG, "the result is a Captured Mob holding a dog");
        h.assertTrue(out.getHoverName().getString().equals("Zoe's Egg"), "called Zoe's Egg: " + out.getHoverName().getString());
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void zoeBitesOneAndAHalfTimesAsHardAsAWolf(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        net.minecraft.world.entity.animal.wolf.Wolf wolf = net.minecraft.world.entity.EntityTypes.WOLF.create(h.getLevel(), EntitySpawnReason.COMMAND);
        double wolfDamage = wolf.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        DogEntity dog = dog(h, new BlockPos(2, 2, 2), player);
        dog.setNoAi(true);
        double dogDamage = dog.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.assertTrue(Math.abs(dogDamage - wolfDamage * 1.5) < 1.0E-6, "Zoe's attack is 1.5 times a wolf's: " + dogDamage + " against " + wolfDamage);

        // A real bite: a pig has 10 health and no armor
        net.minecraft.world.entity.animal.pig.Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(3, 2, 2)));
        pig.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        pig.setNoAi(true);
        h.getLevel().addFreshEntity(pig);
        float before = pig.getHealth();
        dog.doHurtTarget(h.getLevel(), pig);
        h.assertTrue(Math.abs((before - pig.getHealth()) - dogDamage) < 1.0E-3, "a bite takes " + dogDamage + " health, took " + (before - pig.getHealth()));

        // It comes back if it is lost, and stays through the egg
        dog.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).removeModifiers();
        h.assertTrue(dog.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == wolfDamage, "without the bonus she bites like a wolf");
        dog.ensureDamageBoost();
        h.assertTrue(Math.abs(dog.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - dogDamage) < 1.0E-6, "the bonus is put back");
        ItemStack egg = MobCapture.capture(dog);
        h.assertTrue(MobCapture.release(h.getLevel(), egg.get(ModComponents.CAPTURED_MOB), h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))), 0.0F, player), "let out of the egg");
        DogEntity out = h.getLevel().getEntitiesOfClass(DogEntity.class, h.getBounds().inflate(2)).stream().findFirst().orElse(null);
        h.assertTrue(out != null && Math.abs(out.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - dogDamage) < 1.0E-6,
                "still 1.5 times after the egg");
        h.succeed();
    }

    @GameTest(maxTicks = 40)
    public void theDogWearsWolfArmorAndNexiumDogArmor(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        DogEntity dog = dog(h, new BlockPos(2, 2, 2), player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOLF_ARMOR));
        dog.interact(player, InteractionHand.MAIN_HAND, dog.position());
        h.assertTrue(dog.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR), "Wolf Armor goes on by right-click: " + dog.getItemBySlot(EquipmentSlot.BODY));
        dog.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NEXIUM_DOG_ARMOR));
        dog.interact(player, InteractionHand.MAIN_HAND, dog.position());
        h.assertTrue(dog.getItemBySlot(EquipmentSlot.BODY).is(ModItems.NEXIUM_DOG_ARMOR), "so does the Nexium Dog Armor: " + dog.getItemBySlot(EquipmentSlot.BODY));
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void theNexiumDogArmorIsMadeFromIngotsAndOnlyFitsTheDog(GameTestHelper h) {
        ItemStack armor = new ItemStack(ModItems.NEXIUM_DOG_ARMOR);
        var equippable = armor.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        h.assertTrue(equippable != null && equippable.slot() == EquipmentSlot.BODY, "it is body armor");
        h.assertTrue(equippable.allowedEntities().isPresent() && equippable.allowedEntities().get().contains(ModDogs.DOG.builtInRegistryHolder()), "for the dog");
        h.assertTrue(!equippable.allowedEntities().get().contains(net.minecraft.world.entity.EntityTypes.WOLF.builtInRegistryHolder()), "and not for a wolf");
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("nexium:nexium_dog_armor"))).isPresent(), "it has a recipe");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void thereIsNoBreedingAndNoSecondKindOfDog(GameTestHelper h) {
        DogEntity dog = dog(h, new BlockPos(2, 2, 2), null);
        h.assertTrue(!dog.canMate(dog), "dogs do not breed");
        h.succeed();
    }
}
