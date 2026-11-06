package net.storagenexus.essentials.farm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.storagenexus.essentials.ironfarm.IronFarmItem;

import java.util.function.Consumer;

/** A farm as an item: says what it makes and how often. */
public class FarmItem extends BlockItem {
    private final FarmKind kind;

    public FarmItem(FarmKind kind, Block block, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nexusessentials." + kind.lang("makes")).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.farm_every", IronFarmItem.time(kind.cycleTicks())).withStyle(ChatFormatting.GRAY));
        if (kind.hasInput()) tooltip.accept(Component.translatable("tooltip.nexusessentials." + kind.lang("input")).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.farm_hopper").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_redstone").withStyle(ChatFormatting.DARK_GRAY));
    }
}
