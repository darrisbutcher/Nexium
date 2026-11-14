package net.storagenexus.essentials.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.storagenexus.essentials.slots.ExtraSlots;

import java.util.function.Consumer;

/**
 * A Backpack: 36 slots like the player's inventory, and module slots (Auto Torch, Torch Stack). Wear it in the backpack
 * slot above the trinket slot; the Open Backpack key (B) opens the worn one. Right-click: open the one in your hand.
 * Sneak + right-click: wear it.
 */
public class BackpackItem extends Item {
    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            // Swap with whatever is in the backpack slot
            ExtraSlots slots = ExtraSlots.of(player);
            ItemStack worn = slots.backpack();
            if (worn == stack) return InteractionResult.PASS;
            slots.setItem(ExtraSlots.BACKPACK, stack.copy());
            player.setItemInHand(hand, worn.copy());
            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendOverlayMessage(Component.translatable("message.nexusessentials.backpack_equipped"));
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Backpacks.open(serverPlayer, hand == InteractionHand.MAIN_HAND ? serverPlayer.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.nexusessentials.backpack_slots", BackpackContents.usedSlots(stack), BackpackContents.SIZE)
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack module : BackpackModules.slots(stack)) {
            if (module.isEmpty()) continue;
            boolean on = !module.has(net.storagenexus.registry.ModComponents.MODULE_OFF);
            tooltip.accept(Component.translatable(on ? "tooltip.nexusessentials.backpack_module_on" : "tooltip.nexusessentials.backpack_module_off",
                    module.getHoverName()).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.nexusessentials.backpack_use").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.backpack_key",
                Component.keybind("key.nexusessentials.open_backpack")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
