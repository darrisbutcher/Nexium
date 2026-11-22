package net.nexium.energy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/** A small battery: an item that holds Nexus Energy (in {@code nexium:battery_charge}) for the Jetpack and the Crafting Storage Tablet. */
public class BatteryCellItem extends Item {
    private final long capacity;

    public BatteryCellItem(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
    }

    public long capacity() {
        return capacity;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(MAX_BAR_WIDTH * (float) ItemEnergy.stored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFE02828;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        lines.accept(Component.translatable("tooltip.nexium.cell", f.format(ItemEnergy.stored(stack)), f.format(capacity)));
    }
}
