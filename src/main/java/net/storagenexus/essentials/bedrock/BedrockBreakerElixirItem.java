package net.storagenexus.essentials.bedrock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.storagenexus.essentials.config.NexusConfig;

import java.util.function.Consumer;

/** Drink to get the Bedrock Breaker effect: bedrock mines like obsidian with a diamond or netherite pickaxe. */
public class BedrockBreakerElixirItem extends Item {
    public BedrockBreakerElixirItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int seconds = NexusConfig.get().bedrockBreakerSeconds;
        String time = String.format("%d:%02d", seconds / 60, seconds % 60);
        tooltip.accept(Component.translatable("tooltip.nexusessentials.elixir_effect", time).withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.elixir_how").withStyle(ChatFormatting.GRAY));
    }
}
