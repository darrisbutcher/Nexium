package net.nexium.autocraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.nexium.registry.ModComponents;

import java.util.function.Consumer;

/** An encoded pattern: it carries a {@link Pattern} and lists it in its tooltip. */
public class PatternItem extends Item {
    public PatternItem(Properties properties) {
        super(properties);
    }

    public static boolean isEncoded(ItemStack stack) {
        return stack.getItem() instanceof PatternItem && stack.get(ModComponents.PATTERN) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Pattern pattern = stack.get(ModComponents.PATTERN);
        if (pattern == null) {
            tooltip.accept(Component.translatable("tooltip.nexium.pattern.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable(pattern.processing() ? "tooltip.nexium.pattern.processing" : "tooltip.nexium.pattern.crafting")
                .withStyle(ChatFormatting.AQUA));
        for (var entry : pattern.needs().entrySet()) {
            tooltip.accept(Component.literal(entry.getValue() + " x ").append(entry.getKey().toStack().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.nexium.pattern.makes").withStyle(ChatFormatting.DARK_GRAY));
        for (var entry : pattern.yields().entrySet()) {
            tooltip.accept(Component.literal(entry.getValue() + " x ").append(entry.getKey().toStack().getHoverName()).withStyle(ChatFormatting.GREEN));
        }
    }
}
