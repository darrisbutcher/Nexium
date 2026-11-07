package net.storagenexus.aiot.module;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * What is left of the Vein Mining Module, which vein mining no longer needs (hold the Vein Mine key with an AIOT).
 * It stays registered under its old id so that an AIOT, chest or shulker box that still holds one loads as before: an
 * item id that no longer exists makes the whole stack, and whatever it sits in, fail to load. It does nothing, can't be
 * crafted and isn't in the creative tab; right-clicking it hands back the materials it was made from, and so does opening
 * the tool menu of an AIOT that still has one installed ({@link AiotModules#takeRetired}).
 */
public class RetiredModuleItem extends Item {
    public RetiredModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        // The lapis and the diamond go to the inventory while the module still holds its slot, and the module itself turns
        // into its iron ingots (a refund put into the slot the game is about to rewrite would be lost)
        give(serverPlayer, new ItemStack(Items.LAPIS_LAZULI, 4));
        give(serverPlayer, new ItemStack(Items.DIAMOND));
        return InteractionResult.SUCCESS.heldItemTransformedTo(new ItemStack(Items.IRON_INGOT, 4));
    }

    /** Gives back what {@code count} modules were crafted from: 4 iron ingots, 4 lapis lazuli and a diamond each. */
    public static void refund(ServerPlayer player, int count) {
        give(player, new ItemStack(Items.IRON_INGOT, 4 * count));
        give(player, new ItemStack(Items.LAPIS_LAZULI, 4 * count));
        give(player, new ItemStack(Items.DIAMOND, count));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(stack.getMaxStackSize());
            if (!player.getInventory().add(part)) player.drop(part, false, Prediction.SERVER_ONLY);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.storagenexus.retired_module").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.storagenexus.retired_module.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
