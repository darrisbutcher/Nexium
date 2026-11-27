package net.nexium.autocraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What an encoded pattern teaches an Autocrafter. A <b>crafting</b> pattern keeps the nine slots of a crafting grid ({@code inputs},
 * empty where the grid is) and the recipe's result; the Autocrafter crafts it itself. A <b>processing</b> pattern keeps what goes
 * into a machine next to the Autocrafter and what comes out of it, and the Autocrafter pushes the inputs in and waits for the outputs
 * to arrive on the network.
 */
public record Pattern(boolean processing, List<ItemStack> inputs, List<ItemStack> outputs) {
    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("processing").forGetter(Pattern::processing),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inputs").forGetter(Pattern::inputs),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("outputs").forGetter(Pattern::outputs)
    ).apply(i, Pattern::new));

    /** What one craft uses, each kind added up (an empty slot uses nothing). */
    public Map<ItemVariant, Long> needs() {
        return merge(inputs);
    }

    /** What one craft makes, each kind added up. */
    public Map<ItemVariant, Long> yields() {
        return merge(outputs);
    }

    public long yieldOf(ItemVariant variant) {
        return yields().getOrDefault(variant, 0L);
    }

    /** The first output: what the pattern is for (a crafting pattern has just one). */
    public ItemStack primary() {
        for (ItemStack stack : outputs) if (!stack.isEmpty()) return stack;
        return ItemStack.EMPTY;
    }

    public boolean valid() {
        return !primary().isEmpty() && inputs.stream().anyMatch(s -> !s.isEmpty()) && (processing || inputs.size() == 9);
    }

    private static Map<ItemVariant, Long> merge(List<ItemStack> stacks) {
        Map<ItemVariant, Long> out = new LinkedHashMap<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) out.merge(ItemVariant.of(stack), (long) stack.getCount(), Long::sum);
        return out;
    }

    /** A copy of each stack, so a pattern never shares stacks with the slots it was encoded from. */
    public static List<ItemStack> copies(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) out.add(stack.copy());
        return out;
    }
}
