package net.storagenexus.essentials.sponge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The item of a big sponge: its tooltip says how far it reaches and how much water it takes, or that a wet one needs drying. */
public class BigSpongeItem extends BlockItem {
    public BigSpongeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof BigSpongeBlock sponge) {
            tooltip.accept(Component.translatable("tooltip.nexusessentials.big_sponge", sponge.reach(), String.format("%,d", sponge.capacity())).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.nexusessentials.big_sponge_wet").withStyle(ChatFormatting.GRAY));
        }
    }
}
