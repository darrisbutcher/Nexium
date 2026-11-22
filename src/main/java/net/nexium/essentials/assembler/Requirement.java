package net.nexium.essentials.assembler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * One line of what a {@link AssemblyTarget} needs: its name, how many, what counts, the item the screen shows, and
 * whether it is a mob (a spawn egg or a Captured Mob).
 */
public record Requirement(Component name, int needed, Predicate<ItemStack> matches, Item icon, boolean mob) {
    /**
     * The fluid that can stand in for this line's items when a storage network holds it as fluid: a line of water buckets is met by
     * water, a line of lava buckets by lava, one bucket's worth for each bucket. Null for every other line.
     */
    public @Nullable Fluid fluid() {
        if (icon == Items.WATER_BUCKET) return Fluids.WATER;
        if (icon == Items.LAVA_BUCKET) return Fluids.LAVA;
        return null;
    }
}
