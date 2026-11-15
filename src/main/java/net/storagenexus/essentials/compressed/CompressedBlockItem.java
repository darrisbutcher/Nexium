package net.storagenexus.essentials.compressed;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The item of a compressed block. Its tooltip says how much of the plain block it stands for. */
public class CompressedBlockItem extends BlockItem {
    private final CompressedBlocks.Base base;
    private final int level;

    public CompressedBlockItem(Block block, CompressedBlocks.Base base, int level, Properties properties) {
        super(block, properties);
        this.base = base;
        this.level = level;
    }

    public CompressedBlocks.Base base() {
        return base;
    }

    /** 1 for Compressed, 2 for Double Compressed, 3 for Triple Compressed. */
    public int level() {
        return level;
    }

    /** How many of the plain block this one stands for: 9, 81 or 729. */
    public int standsFor() {
        return CompressedBlocks.STANDS_FOR[level - 1];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.nexusessentials.compressed", String.format("%,d", standsFor()), base.block().getName())
                .withStyle(ChatFormatting.GRAY));
    }
}
