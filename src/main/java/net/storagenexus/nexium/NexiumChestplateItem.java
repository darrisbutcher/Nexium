package net.storagenexus.nexium;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.registry.ModComponents;

import java.util.function.Consumer;

/**
 * The Nexium chestplate. Besides being armor, it carries the set bonuses' settings and the Nexus Restock link:
 * sneak + right-click a Crafting Terminal with it to link it, as with a tablet or the Nexus Link module.
 */
public class NexiumChestplateItem extends Item {
    public NexiumChestplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return ModuleItem.linkTo(context, link -> context.getItemInHand().set(ModComponents.TERMINAL_LINK, link),
                "message.storagenexus.nexus_restock.linked") ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = stack.get(ModComponents.TERMINAL_LINK);
        tooltip.accept(link == null
                ? Component.translatable("tooltip.storagenexus.nexus_restock.unlinked").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.storagenexus.nexus_restock.linked", ModuleItem.describe(link)).withStyle(ChatFormatting.AQUA));
    }
}
