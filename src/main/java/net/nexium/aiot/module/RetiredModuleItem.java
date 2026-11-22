package net.nexium.aiot.module;

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
 * What is left of a module that AIOTs no longer need: the Vein Mining Module (hold the Vein Mine key with an AIOT) and the 3x3
 * Mining Module (3x3 mining is built into every AIOT now, switched on and off in the tool menu or with its key). It stays
 * registered under its old id so that an AIOT, chest or shulker box that still holds one loads as before: an item id that no
 * longer exists makes the whole stack, and whatever it sits in, fail to load. It does nothing, can't be crafted and isn't in
 * the creative tab; right-clicking it hands back the materials it was made from, and so does opening the tool menu of an AIOT
 * that still has one installed ({@link AiotModules#takeRetired}).
 */
public class RetiredModuleItem extends Item {
    private final Item middle;

    /** {@code middle} is the module's second material (4 of it, besides 4 iron ingots and a diamond): lapis lazuli or redstone. */
    public RetiredModuleItem(Properties properties, Item middle) {
        super(properties);
        this.middle = middle;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        // The middle material and the diamond go to the inventory while the module still holds its slot, and the module itself
        // turns into its iron ingots (a refund put into the slot the game is about to rewrite would be lost)
        give(serverPlayer, new ItemStack(middle, 4));
        give(serverPlayer, new ItemStack(Items.DIAMOND));
        return InteractionResult.SUCCESS.heldItemTransformedTo(new ItemStack(Items.IRON_INGOT, 4));
    }

    /** Gives back what this retired module was crafted from: 4 iron ingots, 4 of its middle material and a diamond. */
    public static void refund(ServerPlayer player, ItemStack module) {
        if (!(module.getItem() instanceof RetiredModuleItem retired)) return;
        give(player, new ItemStack(Items.IRON_INGOT, 4));
        give(player, new ItemStack(retired.middle, 4));
        give(player, new ItemStack(Items.DIAMOND));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(stack.getMaxStackSize());
            if (!player.getInventory().add(part)) player.drop(part, false, Prediction.SERVER_ONLY);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nexium.retired_module").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexium.retired_module.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
