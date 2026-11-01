package net.storagenexus.essentials.magnet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.slots.ExtraSlots;

import java.util.function.Consumer;

/**
 * Pulls nearby items and experience to the player while it sits in the trinket slot and is switched on.
 * Right-click: wear it in the trinket slot. Sneak + right-click: change the range. Hotkey: switch on or off.
 */
public class MagnetItem extends Item {
    public static final int DEFAULT_RANGE = 8;
    public static final int RANGE_STEP = 4;

    public MagnetItem(Properties properties) {
        super(properties);
    }

    public static int range(ItemStack stack) {
        return Math.max(1, Math.min(stack.getOrDefault(ModComponents.MAGNET_RANGE, DEFAULT_RANGE), NexusConfig.get().magnetMaxRange));
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ModComponents.MAGNET_ACTIVE, true);
    }

    /** The range after this one: 4, 8, 12 ... up to the configured maximum, then back to 4. */
    public static int nextRange(int range) {
        int max = NexusConfig.get().magnetMaxRange;
        if (range >= max) return Math.min(RANGE_STEP, max);
        return Math.min(max, (range / RANGE_STEP + 1) * RANGE_STEP);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                int range = nextRange(range(stack));
                stack.set(ModComponents.MAGNET_RANGE, range);
                player.sendOverlayMessage(Component.translatable("message.nexusessentials.magnet_range", range));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.4F, 1.0F + range / 32.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // Swap with whatever is in the trinket slot
        ExtraSlots slots = ExtraSlots.of(player);
        ItemStack worn = slots.trinket();
        if (ItemStack.isSameItemSameComponents(stack, worn)) return InteractionResult.PASS;
        slots.setItem(ExtraSlots.TRINKET, stack.copy());
        player.setItemInHand(hand, worn.copy());
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.translatable("message.nexusessentials.magnet_equipped"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        boolean active = isActive(stack);
        tooltip.accept(Component.translatable(active ? "tooltip.nexusessentials.magnet_on" : "tooltip.nexusessentials.magnet_off")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.magnet_range", range(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.magnet_slot").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.magnet_cycle").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.magnet_toggle",
                Component.keybind("key.nexusessentials.toggle_magnet")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
