package net.storagenexus.tablet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.storagenexus.network.TerminalBlock;
import net.storagenexus.registry.ModComponents;

import java.util.function.Consumer;

/**
 * Sneak-right-click a terminal to link. Right-click anywhere to open that terminal's network:
 * no range limit and any dimension, as long as the terminal's chunk is loaded (a Dimensional Anchor
 * keeps it so).
 */
public class StorageTabletItem extends Item {
    public StorageTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof TerminalBlock)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            GlobalPos link = GlobalPos.of(level.dimension(), context.getClickedPos().immutable());
            context.getItemInHand().set(ModComponents.TERMINAL_LINK, link);
            player.sendOverlayMessage(Component.translatable("message.storagenexus.tablet_linked", describe(link)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        GlobalPos link = stack.get(ModComponents.TERMINAL_LINK);
        if (link == null) {
            player.sendOverlayMessage(Component.translatable("message.storagenexus.tablet_unlinked").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        MinecraftServer server = level.getServer();
        ServerLevel target = server == null ? null : server.getLevel(link.dimension());
        if (target == null) {
            offline(player);
            return InteractionResult.FAIL;
        }
        BlockPos pos = link.pos();
        if (!target.isLoaded(pos)) {
            offline(player);
            return InteractionResult.FAIL;
        }
        if (!(target.getBlockState(pos).getBlock() instanceof TerminalBlock)) {
            player.sendOverlayMessage(Component.translatable("message.storagenexus.tablet_terminal_missing").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        TerminalBlock.open((ServerPlayer) player, target, pos, true,
                p -> holdsLinkedTablet(p, link) && target.isLoaded(pos) && target.getBlockState(pos).getBlock() instanceof TerminalBlock,
                null);
        return InteractionResult.SUCCESS;
    }

    private static boolean holdsLinkedTablet(Player p, GlobalPos link) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.getItem() instanceof StorageTabletItem && link.equals(s.get(ModComponents.TERMINAL_LINK))) return true;
        }
        return false;
    }

    private static void offline(Player player) {
        player.sendOverlayMessage(Component.translatable("message.storagenexus.network_offline").withStyle(ChatFormatting.RED));
    }

    private static Component describe(GlobalPos link) {
        BlockPos p = link.pos();
        return Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + link.dimension().identifier().getPath() + ")");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = stack.get(ModComponents.TERMINAL_LINK);
        if (link == null) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.tablet_unlinked").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.storagenexus.tablet_linked", describe(link)).withStyle(ChatFormatting.AQUA));
        }
    }
}
