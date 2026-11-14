package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.CapturedMobDetails;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What a Captured Mob's tooltip says beyond its name: what it looks like (always shown) and its age, owner, gear and status
 * (with Shift). Most cases capture a real mob, so a change in how a mob is saved shows up here. Lines are checked by their
 * lang keys and arguments, so they do not depend on a language being loaded.
 */
public class CapturedMobDetailsGameTest {
    private static CapturedMob capture(GameTestHelper h, EntityType<?> type, java.util.function.Consumer<Mob> setup) {
        Mob mob = (Mob) h.spawn(type, new BlockPos(2, 2, 2));
        setup.accept(mob);
        return MobCapture.toItem(new CapturedMob(type, MobCapture.capture(mob).get(ModComponents.CAPTURED_MOB).data(),
                Optional.empty(), 10.0F, 10.0F, mob.isBaby())).get(ModComponents.CAPTURED_MOB);
    }

    private static CapturedMob made(EntityType<?> type, CompoundTag data) {
        return new CapturedMob(type, data, Optional.empty(), 10.0F, 10.0F, false);
    }

    /** Every lang key in a line, its arguments and siblings too. */
    private static List<String> keys(Component c) {
        List<String> out = new ArrayList<>();
        collect(c, out);
        return out;
    }

    private static void collect(Component c, List<String> out) {
        if (c.getContents() instanceof TranslatableContents t) {
            out.add(t.getKey());
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component inner) collect(inner, out);
                else out.add(String.valueOf(arg));
            }
        } else {
            out.add(c.getString());
        }
        for (Component sibling : c.getSiblings()) collect(sibling, out);
    }

    private static boolean has(List<Component> lines, String key, String arg) {
        for (Component line : lines) {
            List<String> k = keys(line);
            if (k.contains(key) && (arg == null || k.contains(arg))) return true;
        }
        return false;
    }

    @GameTest
    public void aFrogShowsItsVariant(GameTestHelper h) {
        CapturedMob frog = capture(h, EntityTypes.FROG, m -> {});
        h.assertTrue(has(CapturedMobDetails.looks(frog), "tooltip.nexusessentials.captured_mob_variant", "Temperate"), "a frog shows its variant");
        CompoundTag warm = new CompoundTag();
        warm.putString("variant", "minecraft:warm");
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.FROG, warm)), "tooltip.nexusessentials.captured_mob_variant", "Warm"), "a warm one says warm");
        h.succeed();
    }

    @GameTest
    public void otherMobsShowWhatTheyLookLike(GameTestHelper h) {
        CompoundTag horse = new CompoundTag();
        horse.putInt("Variant", 2 | 3 << 8); // chestnut with white dots
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.HORSE, horse)), "tooltip.nexusessentials.captured_mob_variant", "Chestnut, White Dots"), "a horse's coat");
        CompoundTag plain = new CompoundTag();
        plain.putInt("Variant", 5);
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.HORSE, plain)), "tooltip.nexusessentials.captured_mob_variant", "Gray"), "no markings, just the colour");

        CapturedMob sheep = capture(h, EntityTypes.SHEEP, m -> ((Sheep) m).setColor(DyeColor.LIGHT_BLUE));
        h.assertTrue(has(CapturedMobDetails.looks(sheep), "tooltip.nexusessentials.captured_mob_colour", "Light Blue"), "a sheep's wool");

        CompoundTag rabbit = new CompoundTag();
        rabbit.putInt("RabbitType", 5);
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.RABBIT, rabbit)), "tooltip.nexusessentials.captured_mob_variant", "Salt and Pepper"), "a rabbit");
        CompoundTag fox = new CompoundTag();
        fox.putString("Type", "snow");
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.FOX, fox)), "tooltip.nexusessentials.captured_mob_variant", "Snow"), "a fox");

        CompoundTag villager = new CompoundTag();
        CompoundTag data = new CompoundTag();
        data.putString("profession", "minecraft:farmer");
        data.putInt("level", 3);
        data.putString("type", "minecraft:desert");
        villager.put("VillagerData", data);
        List<Component> lines = CapturedMobDetails.looks(made(EntityTypes.VILLAGER, villager));
        h.assertTrue(has(lines, "tooltip.nexusessentials.captured_mob_villager", "Farmer") && has(lines, "tooltip.nexusessentials.captured_mob_villager", "Desert")
                && has(lines, "tooltip.nexusessentials.captured_mob_villager", "merchant.level.3"), "a villager's job, level and village type");
        CompoundTag none = new CompoundTag();
        CompoundTag jobless = new CompoundTag();
        jobless.putString("profession", "minecraft:none");
        none.put("VillagerData", jobless);
        h.assertTrue(has(CapturedMobDetails.looks(made(EntityTypes.VILLAGER, none)), "tooltip.nexusessentials.captured_mob_unemployed", null), "without a job");
        h.succeed();
    }

    @GameTest
    public void aMobWithNothingToSayHasNoLines(GameTestHelper h) {
        CapturedMob blank = made(EntityTypes.COW, new CompoundTag());
        h.assertTrue(CapturedMobDetails.looks(blank).isEmpty(), "no looks without data");
        h.assertTrue(CapturedMobDetails.details(blank, id -> null).isEmpty(), "no details without data");
        CapturedMob creeper = capture(h, EntityTypes.CREEPER, m -> {});
        h.assertTrue(CapturedMobDetails.looks(creeper).isEmpty(), "a creeper has nothing to tell apart");
        h.assertTrue(CapturedMobDetails.details(creeper, id -> null).isEmpty(), "and nothing more to say");
        h.succeed();
    }

    @GameTest
    public void ageAndBreeding(GameTestHelper h) {
        CapturedMob baby = capture(h, EntityTypes.COW, m -> { ((Animal) m).setBaby(true); ((Animal) m).setInLove(null); });
        List<Component> lines = CapturedMobDetails.details(baby, id -> null);
        h.assertTrue(has(lines, "tooltip.nexusessentials.captured_mob_grows_up", "20:00"), "a new baby grows up in 20 minutes");
        h.assertTrue(has(lines, "tooltip.nexusessentials.captured_mob_in_love", null), "in love");
        CompoundTag adult = new CompoundTag();
        adult.putInt("Age", 6000);
        h.assertTrue(has(CapturedMobDetails.details(made(EntityTypes.COW, adult), id -> null), "tooltip.nexusessentials.captured_mob_breed_in", "5:00"), "breeding cooldown");
        CompoundTag locked = new CompoundTag();
        locked.putInt("Age", -100);
        locked.putBoolean("AgeLocked", true);
        h.assertTrue(has(CapturedMobDetails.details(made(EntityTypes.COW, locked), id -> null), "tooltip.nexusessentials.captured_mob_stays_baby", null), "a locked baby stays one");
        h.succeed();
    }

    @SuppressWarnings("deprecation")
    @GameTest
    public void ownerAndGear(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        CapturedMob wolf = capture(h, EntityTypes.WOLF, m -> ((Wolf) m).tame(player));
        UUID owner = player.getUUID();
        h.assertTrue(has(CapturedMobDetails.details(wolf, id -> id.equals(owner) ? "Steve" : null), "tooltip.nexusessentials.captured_mob_tamed_by", "Steve"), "tamed by name");
        h.assertTrue(has(CapturedMobDetails.details(wolf, id -> null), "tooltip.nexusessentials.captured_mob_tamed", null), "tamed, owner not known");
        h.assertTrue(has(CapturedMobDetails.details(wolf, id -> null), "tooltip.nexusessentials.captured_mob_collar", "Red"), "its collar");
        CapturedMob zombie = capture(h, EntityTypes.ZOMBIE, m -> {
            m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        });
        List<Component> gear = CapturedMobDetails.details(zombie, id -> null);
        h.assertTrue(has(gear, "tooltip.nexusessentials.captured_mob_gear", "item.minecraft.diamond_helmet") && has(gear, "tooltip.nexusessentials.captured_mob_gear", "item.minecraft.iron_sword"),
                "what it wears and holds");
        CapturedMob horse = capture(h, EntityTypes.HORSE, m -> m.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.IRON_HORSE_ARMOR)));
        h.assertTrue(has(CapturedMobDetails.details(horse, id -> null), "tooltip.nexusessentials.captured_mob_gear", "item.minecraft.iron_horse_armor"), "horse armor");
        h.succeed();
    }

    @GameTest
    public void statusAndTraits(GameTestHelper h) {
        CapturedMob cow = capture(h, EntityTypes.COW, m -> m.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1)));
        List<Component> lines = CapturedMobDetails.details(cow, id -> null);
        h.assertTrue(has(lines, "effect.minecraft.speed", null) && has(lines, "enchantment.level.2", null) && has(lines, " (0:30)", null), "Speed II with its time left");
        CapturedMob sheared = capture(h, EntityTypes.SHEEP, m -> ((Sheep) m).setSheared(true));
        h.assertTrue(has(CapturedMobDetails.details(sheared, id -> null), "tooltip.nexusessentials.captured_mob_sheared", null), "sheared");
        CompoundTag slime = new CompoundTag();
        slime.putInt("Size", 2);
        h.assertTrue(has(CapturedMobDetails.details(made(EntityTypes.SLIME, slime), id -> null), "tooltip.nexusessentials.captured_mob_size", "3"), "a slime's size");
        CompoundTag creeper = new CompoundTag();
        creeper.putBoolean("powered", true);
        h.assertTrue(has(CapturedMobDetails.details(made(EntityTypes.CREEPER, creeper), id -> null), "tooltip.nexusessentials.captured_mob_charged", null), "charged");
        CompoundTag zombieVillager = new CompoundTag();
        zombieVillager.putInt("ConversionTime", 1200);
        h.assertTrue(has(CapturedMobDetails.details(made(EntityTypes.ZOMBIE_VILLAGER, zombieVillager), id -> null), "tooltip.nexusessentials.captured_mob_curing", null), "being cured");
        h.succeed();
    }

    @GameTest
    public void theHelpersFormatLikeTheGame(GameTestHelper h) {
        h.assertValueEqual(CapturedMobDetails.pretty("minecraft:british_shorthair"), "British Shorthair", "a name from an id");
        h.assertValueEqual(CapturedMobDetails.pretty("snow"), "Snow", "without a namespace");
        h.assertValueEqual(CapturedMobDetails.time(600), "0:30", "ticks as m:ss");
        h.assertValueEqual(CapturedMobDetails.time(24000), "20:00", "a baby's whole growing time");
        h.assertValueEqual(CapturedMobDetails.time(72000), "1:00:00", "an hour");
        h.succeed();
    }
}
