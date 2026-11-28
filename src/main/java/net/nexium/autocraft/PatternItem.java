package net.nexium.autocraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.nexium.registry.ModItems;
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

    /** Shift + right-click in the air: an encoded pattern is wiped back to a Blank Pattern. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return clear(level, player, hand) ? InteractionResult.SUCCESS : super.use(level, player, hand);
    }

    /** The same on a block (sneaking skips the block's own use, so this is the item's turn). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return clear(context.getLevel(), context.getPlayer(), context.getHand()) ? InteractionResult.SUCCESS : super.useOn(context);
    }

    private static boolean clear(Level level, Player player, InteractionHand hand) {
        if (player == null || !player.isShiftKeyDown() || !isEncoded(player.getItemInHand(hand))) return false;
        if (!level.isClientSide()) {
            player.setItemInHand(hand, new ItemStack(ModItems.BLANK_PATTERN));
            player.sendOverlayMessage(Component.translatable("message.nexium.pattern.cleared"));
        }
        return true;
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
        tooltip.accept(Component.translatable("tooltip.nexium.pattern.clear").withStyle(ChatFormatting.DARK_GRAY));
    }
}
