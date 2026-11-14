package net.storagenexus.essentials.xpfarm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The Experience Farm as an item: says what it makes and how much it holds. */
public class XpFarmItem extends BlockItem {
    public XpFarmItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nexusessentials.xp_farm_rate", XpFarmBlockEntity.XP_PER_TICK * 20,
                XpFarmBlockEntity.ENERGY_PER_TICK).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.xp_farm_cap", XpFarmBlockEntity.CAP_LEVELS).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.xp_farm_redstone").withStyle(ChatFormatting.DARK_GRAY));
    }
}
