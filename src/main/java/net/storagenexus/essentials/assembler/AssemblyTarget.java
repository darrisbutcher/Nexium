package net.storagenexus.essentials.assembler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.essentials.farm.FarmRecipe;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import net.storagenexus.essentials.librarian.Librarian;
import net.storagenexus.essentials.registry.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What the Farm Assembler can make: the one-block farms and the Master Librarian, in the order its screen lists them. */
public enum AssemblyTarget {
    IRON_FARM(null),
    VILLAGER_BREEDER(FarmKind.VILLAGER_BREEDER),
    PIGLIN_BARTER(FarmKind.PIGLIN_BARTER),
    ZOMBIFIED_PIGLIN(FarmKind.ZOMBIFIED_PIGLIN),
    WITHER_SKELETON(FarmKind.WITHER_SKELETON),
    SHULKER(FarmKind.SHULKER),
    WITCH(FarmKind.WITCH),
    SUGAR_CANE(FarmKind.SUGAR_CANE),
    BAMBOO(FarmKind.BAMBOO),
    NETHER_WART(FarmKind.NETHER_WART),
    LIBRARIAN(null);

    /** The most lines any target has (the Iron Farm: seven parts and its smooth stone). */
    public static final int MAX_REQUIREMENTS = 8;

    private final @Nullable FarmKind kind;

    AssemblyTarget(@Nullable FarmKind kind) {
        this.kind = kind;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AssemblyTarget byId(String id) {
        for (AssemblyTarget t : values()) if (t.id().equals(id)) return t;
        return IRON_FARM;
    }

    public static AssemblyTarget byIndex(int index) {
        AssemblyTarget[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** What it needs, in the order the screen lists them; the filler block, if any, comes last. */
    public List<Requirement> requirements() {
        List<Requirement> list = new ArrayList<>();
        if (this == IRON_FARM) {
            for (IronFarmRecipe.Part part : IronFarmRecipe.Part.values()) {
                list.add(new Requirement(part.displayName(), part.needed, s -> IronFarmRecipe.partOf(s) == part, part.icon, part.isMob()));
            }
            list.add(new Requirement(Component.translatable("message.nexusessentials.iron_farm_part.smooth_stone"), IronFarmRecipe.FILLER_AMOUNT,
                    IronFarmRecipe::isFiller, Items.SMOOTH_STONE, false));
        } else if (this == LIBRARIAN) {
            list.add(new Requirement(IronFarmRecipe.Part.VILLAGERS.displayName(), NexusConfig.get().librarianVillagers,
                    s -> IronFarmRecipe.partOf(s) == IronFarmRecipe.Part.VILLAGERS, Items.VILLAGER_SPAWN_EGG, true));
        } else {
            FarmRecipe recipe = FarmRecipe.of(kind);
            for (FarmRecipe.Part part : recipe.parts()) list.add(of(part));
            list.add(of(recipe.filler()));
        }
        return list;
    }

    private static Requirement of(FarmRecipe.Part part) {
        return new Requirement(part.displayName(), part.needed(), part.matches(), part.icon(), part.mob());
    }

    /** What it makes, as shown in the list and the header. */
    public ItemStack icon() {
        if (this == IRON_FARM) return new ItemStack(ModItems.IRON_FARM);
        if (this == LIBRARIAN) return new ItemStack(Items.VILLAGER_SPAWN_EGG);
        return new ItemStack(ModItems.FARMS.get(kind));
    }

    public Component displayName() {
        if (this == LIBRARIAN) return Component.translatable("screen.nexusessentials.assembler.librarian");
        return icon().getHoverName();
    }

    /** The shorter name the screen's list shows. */
    public Component shortName() {
        return Component.translatable("screen.nexusessentials.assembler.short." + id());
    }

    /** One line on what it does. */
    public Component makes() {
        if (kind != null) return Component.translatable("tooltip.nexusessentials." + kind.lang("makes"));
        return Component.translatable("screen.nexusessentials.assembler.makes." + id());
    }

    /** Makes the finished thing; empty if it can't be made right now (a Master Librarian needs a villager to spawn). */
    public ItemStack build(ServerLevel level) {
        if (this == LIBRARIAN) return Librarian.create(level);
        return icon();
    }
}
