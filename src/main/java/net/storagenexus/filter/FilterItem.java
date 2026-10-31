package net.storagenexus.filter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.util.Menus;

import java.util.function.Consumer;

/**
 * Basic Filter: whitelist/blacklist of up to 9 items, optional exact-component match.
 * Advanced Filter: matches anything sharing an item tag with an entry (e.g. one ore matches all ores).
 * Fluids are filtered by putting their bucket in the filter.
 */
public class FilterItem extends Item {
    private final boolean advanced;

    public FilterItem(Properties properties, boolean advanced) {
        super(properties);
        this.advanced = advanced;
    }

    public boolean isAdvanced() { return advanced; }

    public FilterData data(ItemStack stack) {
        FilterData d = stack.get(ModComponents.FILTER);
        return d != null ? d : FilterData.empty(advanced);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ItemStack stack = player.getItemInHand(hand);
            Menus.open(sp, stack.getHoverName(), hand.ordinal(), (id, inv, p) -> new FilterMenu(id, inv, hand.ordinal()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        FilterData d = data(stack);
        tooltip.accept(Component.translatable(d.whitelist() ? "tooltip.storagenexus.whitelist" : "tooltip.storagenexus.blacklist")
                .withStyle(d.whitelist() ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (advanced) tooltip.accept(Component.translatable("tooltip.storagenexus.tag_mode").withStyle(ChatFormatting.GOLD));
        else if (d.matchComponents()) tooltip.accept(Component.translatable("tooltip.storagenexus.match_components").withStyle(ChatFormatting.GRAY));
        for (ItemStack s : d.entries()) {
            if (!s.isEmpty()) tooltip.accept(Component.literal(" - ").append(s.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
