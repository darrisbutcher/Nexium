package net.storagenexus.essentials.assembler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * One line of what a {@link AssemblyTarget} needs: its name, how many, what counts, the item the screen shows, and
 * whether it is a mob (a spawn egg or a Captured Mob).
 */
public record Requirement(Component name, int needed, Predicate<ItemStack> matches, Item icon, boolean mob) {
}
