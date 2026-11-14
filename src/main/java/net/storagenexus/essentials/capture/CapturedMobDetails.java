package net.storagenexus.essentials.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/**
 * What a Captured Mob's tooltip says beyond its name and health, read from the mob's saved data. {@link #looks} is
 * what it looks like (a variant, a colour, a villager's job) and is always shown, because the farms ask for variants.
 * {@link #details} is the rest, shown while Shift is held: its age and breeding, its owner and gear, and its status.
 * A mob whose data lacks a field simply has no line for it.
 */
public final class CapturedMobDetails {
    private CapturedMobDetails() {}

    private static final String[] HORSE_COLOURS = {"White", "Creamy", "Chestnut", "Brown", "Black", "Gray", "Dark Brown"};
    private static final String[] HORSE_MARKINGS = {"", "White", "White Field", "White Dots", "Black Dots"};
    private static final String[] LLAMAS = {"Creamy", "White", "Brown", "Gray"};
    private static final String[] PARROTS = {"Red and Blue", "Blue", "Green", "Yellow and Blue", "Gray"};
    private static final String[] AXOLOTLS = {"Lucy", "Wild", "Gold", "Cyan", "Blue"};
    private static final String[] RABBITS = {"Brown", "White", "Black", "Black and White", "Gold", "Salt and Pepper"};
    /** The slots a mob's equipment can fill, in the order the tooltip lists them. */
    private static final String[] SLOTS = {"mainhand", "offhand", "head", "chest", "legs", "feet", "body", "saddle"};

    // ---------------------------------------------------------------- looks

    /** What the mob looks like: its variant, colour or job. Often one line, none for a mob that has nothing to tell apart. */
    public static List<Component> looks(CapturedMob mob) {
        List<Component> lines = new ArrayList<>();
        CompoundTag d = mob.data();
        EntityType<?> type = mob.type();
        String variant = d.getStringOr("variant", "");
        if (!variant.isEmpty()) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", pretty(variant)));
        } else if (type == EntityTypes.HORSE && d.contains("Variant")) {
            int v = d.getIntOr("Variant", 0);
            String colour = name(HORSE_COLOURS, v & 255);
            String markings = name(HORSE_MARKINGS, v >> 8 & 255);
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", markings.isEmpty() ? colour : colour + ", " + markings));
        } else if ((type == EntityTypes.LLAMA || type == EntityTypes.TRADER_LLAMA) && d.contains("Variant")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", name(LLAMAS, d.getIntOr("Variant", 0))));
        } else if (type == EntityTypes.PARROT && d.contains("Variant")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", name(PARROTS, d.getIntOr("Variant", 0))));
        } else if (type == EntityTypes.AXOLOTL && d.contains("Variant")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", name(AXOLOTLS, d.getIntOr("Variant", 0))));
        } else if (type == EntityTypes.RABBIT && d.contains("RabbitType")) {
            int r = d.getIntOr("RabbitType", 0);
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", r == 99 ? "Killer Bunny" : name(RABBITS, r)));
        } else if ((type == EntityTypes.FOX || type == EntityTypes.MOOSHROOM) && d.contains("Type")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", pretty(d.getStringOr("Type", ""))));
        } else if (type == EntityTypes.PANDA && d.contains("MainGene")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", pretty(d.getStringOr("MainGene", ""))));
        } else if (type == EntityTypes.TROPICAL_FISH && d.contains("Variant")) {
            int v = d.getIntOr("Variant", 0);
            String pattern = pretty(net.minecraft.world.entity.animal.fish.TropicalFish.getPattern(v).name());
            lines.add(line("tooltip.nexusessentials.captured_mob_variant", pattern + " (" + pretty(DyeColor.byId(v >> 16 & 255).getName())
                    + " and " + pretty(net.minecraft.world.entity.animal.fish.TropicalFish.getPatternColor(v).getName()) + ")"));
        }
        if (type == EntityTypes.SHEEP && d.contains("Color")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_colour", pretty(DyeColor.byId(d.getByteOr("Color", (byte) 0)).getName())));
        }
        if ((type == EntityTypes.VILLAGER || type == EntityTypes.ZOMBIE_VILLAGER) && d.contains("VillagerData")) {
            CompoundTag v = d.getCompoundOrEmpty("VillagerData");
            String profession = v.getStringOr("profession", "minecraft:none");
            Component job = profession.endsWith(":none") ? Component.translatable("tooltip.nexusessentials.captured_mob_unemployed") : Component.literal(pretty(profession));
            int level = Math.max(1, Math.min(5, v.getIntOr("level", 1)));
            lines.add(line("tooltip.nexusessentials.captured_mob_villager", job, Component.translatable("merchant.level." + level), pretty(v.getStringOr("type", "minecraft:plains"))));
        }
        return lines;
    }

    // ---------------------------------------------------------------- details

    /**
     * The rest: age and breeding, owner and gear, and status. {@code ownerName} turns the owner's id into a name when it
     * can (null if it can't), and the lines are empty for a mob with nothing more to say.
     */
    public static List<Component> details(CapturedMob mob, Function<UUID, @Nullable String> ownerName) {
        List<Component> lines = new ArrayList<>();
        CompoundTag d = mob.data();

        // Age and breeding
        int age = d.getIntOr("Age", 0);
        if (age < 0) {
            lines.add(d.getBooleanOr("AgeLocked", false) ? line("tooltip.nexusessentials.captured_mob_stays_baby")
                    : line("tooltip.nexusessentials.captured_mob_grows_up", time(-age)));
        } else if (age > 0) {
            lines.add(line("tooltip.nexusessentials.captured_mob_breed_in", time(age)));
        }
        if (d.getIntOr("InLove", 0) > 0) lines.add(line("tooltip.nexusessentials.captured_mob_in_love"));

        // Owner and gear
        var owner = d.getIntArray("Owner");
        if (owner.isPresent() && owner.get().length == 4) {
            String name = ownerName.apply(UUIDUtil.uuidFromIntArray(owner.get()));
            lines.add(name == null ? line("tooltip.nexusessentials.captured_mob_tamed") : line("tooltip.nexusessentials.captured_mob_tamed_by", name));
            if (d.contains("CollarColor")) {
                lines.add(line("tooltip.nexusessentials.captured_mob_collar", pretty(DyeColor.byId(d.getByteOr("CollarColor", (byte) 0)).getName())));
            }
        }
        if (d.getBooleanOr("Sitting", false)) lines.add(line("tooltip.nexusessentials.captured_mob_sitting"));
        List<Component> gear = gear(d.getCompoundOrEmpty("equipment"));
        if (!gear.isEmpty()) {
            MutableComponent joined = Component.empty();
            for (int i = 0; i < gear.size(); i++) {
                if (i > 0) joined.append(", ");
                joined.append(gear.get(i));
            }
            lines.add(line("tooltip.nexusessentials.captured_mob_gear", joined));
        }

        // Status and traits
        ListTag effects = d.getListOrEmpty("active_effects");
        for (int i = 0; i < effects.size(); i++) {
            CompoundTag e = effects.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(e.getStringOr("id", ""));
            var effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getValue(id);
            if (effect == null) continue;
            MutableComponent text = effect.getDisplayName().copy();
            int amplifier = e.getByteOr("amplifier", (byte) 0);
            if (amplifier >= 1 && amplifier < 10) text.append(" ").append(Component.translatable("enchantment.level." + (amplifier + 1)));
            lines.add(Component.empty().append(text).append(" (" + time(e.getIntOr("duration", 0)) + ")").withStyle(ChatFormatting.BLUE));
        }
        if ((mob.type() == EntityTypes.SLIME || mob.type() == EntityTypes.MAGMA_CUBE) && d.contains("Size")) {
            lines.add(line("tooltip.nexusessentials.captured_mob_size", d.getIntOr("Size", 0) + 1));
        }
        if (d.getBooleanOr("powered", false)) lines.add(line("tooltip.nexusessentials.captured_mob_charged"));
        if (d.getBooleanOr("Sheared", false)) lines.add(line("tooltip.nexusessentials.captured_mob_sheared"));
        if (d.getIntOr("ConversionTime", -1) > 0) lines.add(line("tooltip.nexusessentials.captured_mob_curing"));
        return lines;
    }

    /** The names of what the mob wears and holds, in slot order. */
    private static List<Component> gear(CompoundTag equipment) {
        List<Component> names = new ArrayList<>();
        for (String slot : SLOTS) {
            Identifier id = Identifier.tryParse(equipment.getCompoundOrEmpty(slot).getStringOr("id", ""));
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) names.add(stack.getHoverName());
        }
        return names;
    }

    // ---------------------------------------------------------------- helpers

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static String name(String[] names, int index) {
        return index >= 0 && index < names.length ? names[index] : "?";
    }

    /** "minecraft:british_shorthair" as "British Shorthair". */
    public static String pretty(String id) {
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_ ]")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /** Ticks as m:ss, or h:mm:ss from an hour up. */
    public static String time(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        int h = seconds / 3600, m = seconds / 60 % 60, s = seconds % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }
}
