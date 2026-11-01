package net.storagenexus.essentials.ironfarm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.Locale;
import java.util.function.Consumer;

/** The Iron Farm as an item: says what it makes, and shows a boost or cooldown it carries. */
public class IronFarmItem extends BlockItem {
    public IronFarmItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        NexusConfig config = NexusConfig.get();
        tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_rate", ironPerHour(1)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_bread",
                config.ironFarmBreadSpeedUp, config.ironFarmBreadSeconds).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_hopper").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_redstone").withStyle(ChatFormatting.DARK_GRAY));
        IronFarmTimers timers = stack.get(ModComponents.IRON_FARM_TIMERS);
        if (timers != null) {
            if (timers.boostLeft() > 0) {
                tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_boosting", time(timers.boostLeft())).withStyle(ChatFormatting.YELLOW));
            }
            if (timers.cooldown() > 0) {
                tooltip.accept(Component.translatable("tooltip.nexusessentials.iron_farm_cooling", time(timers.cooldown())).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /** Average iron an hour (golems drop 3-5, so 4 on average) at the given speed-up, e.g. "1,920". */
    public static String ironPerHour(int speedUp) {
        long golemsPerHour = 72000L * speedUp / NexusConfig.get().ironFarmGolemTicks;
        return String.format(Locale.ROOT, "%,d", golemsPerHour * 4);
    }

    /** Ticks as m:ss, or h:mm:ss from an hour up. */
    public static String time(long ticks) {
        long seconds = (ticks + 19) / 20;
        long h = seconds / 3600, m = seconds / 60 % 60, s = seconds % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }
}
