package net.storagenexus.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.storagenexus.registry.ModComponents;

import java.util.function.Consumer;

/**
 * A Backpack module. Put it in one of the module slots in the Backpack's screen; the screen also switches it on or off
 * (the switch is the same {@code storagenexus:module_off} flag the AIOT modules use, kept on the module itself).
 */
public class BackpackModuleItem extends Item {
    private final BackpackModuleType type;

    public BackpackModuleItem(Properties properties, BackpackModuleType type) {
        super(properties);
        this.type = type;
    }

    public BackpackModuleType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.nexusessentials." + type.id() + "_module.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.backpack_module_use").withStyle(ChatFormatting.DARK_GRAY));
        if (stack.has(ModComponents.MODULE_OFF)) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.module_off").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
