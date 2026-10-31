package net.storagenexus.dimension;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.storagenexus.registry.ModComponents;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Click a Dimensional Input to start (or pick up) a link, then click any number of
 * Dimensional Outputs to join them to it. Rename the card in an anvil first to choose the
 * link's name; otherwise a random unique name is picked.
 */
public class DimensionalLinkCardItem extends Item {
    public DimensionalLinkCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack card = context.getItemInHand();
        var be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof LinkedBlockEntity linked)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server) || player == null) return InteractionResult.SUCCESS;
        LinkRegistry reg = LinkRegistry.get(server.getServer());

        if (linked instanceof DimensionalInputBlockEntity input) {
            UUID id = input.linkId();
            if (id == null || reg.get(id) == null) {
                UUID carried = card.get(ModComponents.LINK_ID);
                LinkRegistry.Link orphan = reg.get(carried);
                if (orphan != null && orphan.input() == null) {
                    // The card's link lost its Input (broken or moved): this Input takes it over.
                    id = carried;
                } else {
                    Component custom = card.get(DataComponents.CUSTOM_NAME);
                    String requested = custom != null ? custom.getString() : null;
                    id = reg.create(requested, input.globalPos(), server.getRandom());
                    if (id == null) {
                        player.sendSystemMessage(Component.translatable("message.storagenexus.name_taken", requested).withStyle(ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                }
                input.setLink(id);
            }
            reg.setInput(id, input.globalPos());
            String name = reg.get(id).name();
            card.set(ModComponents.LINK_ID, id);
            card.set(ModComponents.LINK_NAME, name);
            player.sendOverlayMessage(Component.translatable("message.storagenexus.card_input", name).withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }

        if (linked instanceof DimensionalOutputBlockEntity output) {
            UUID id = card.get(ModComponents.LINK_ID);
            if (id == null || reg.get(id) == null) {
                player.sendOverlayMessage(Component.translatable("message.storagenexus.card_empty").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            reg.addOutput(id, output.globalPos());
            output.setLink(id);
            // Numbering of the other outputs may have changed ("Name" -> "Name 1").
            for (var gp : reg.get(id).outputs()) {
                ServerLevel l = server.getServer().getLevel(gp.dimension());
                if (l != null && l.isLoaded(gp.pos()) && l.getBlockEntity(gp.pos()) instanceof LinkedBlockEntity other) {
                    if (other.refreshName()) other.sync();
                }
            }
            player.sendOverlayMessage(Component.translatable("message.storagenexus.card_output", output.displayName()).withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String name = stack.get(ModComponents.LINK_NAME);
        if (name == null) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.card_blank").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.storagenexus.card_linked", name).withStyle(ChatFormatting.AQUA));
        }
    }
}
