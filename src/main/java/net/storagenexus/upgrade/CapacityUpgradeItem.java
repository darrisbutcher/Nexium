package net.storagenexus.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class CapacityUpgradeItem extends UpgradeItem {
    private final int multiplier;

    public CapacityUpgradeItem(Properties properties, int multiplier) {
        super(properties, Kind.CAPACITY);
        this.multiplier = multiplier;
    }

    public int multiplier() {
        return multiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.storagenexus.capacity_upgrade", multiplier).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.storagenexus.capacity_upgrade.stack").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.storagenexus.capacity_upgrade.swap").withStyle(ChatFormatting.DARK_GRAY));
    }
}
