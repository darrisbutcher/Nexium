package net.nexium.tablet;

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
import net.nexium.network.TerminalBlock;
import net.nexium.registry.ModComponents;

import java.util.function.Consumer;

/**
 * Sneak-right-click a terminal to link. Right-click anywhere to open that terminal's network:
 * no range limit and any dimension, as long as the terminal's chunk is loaded (a Dimensional Anchor
 * keeps it so).
 */
public class StorageTabletItem extends Item implements net.nexium.energy.CellHolder {
    /** NE to open the linked terminal from the same dimension, and from another one; then NE a tick while its screen stays open. */
    public static final long OPEN_COST = 100, ACROSS_COST = 1_000, UPKEEP = 2;

    @Override
    public String useKey() {
        return "screen.nexium.cell.tablet";
    }

    /** What opening this link costs from where the player is. */
    public static long openCost(Level level, GlobalPos link) {
        return level.dimension().equals(link.dimension()) ? OPEN_COST : ACROSS_COST;
    }

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
            player.sendOverlayMessage(Component.translatable("message.nexium.tablet_linked", describe(link)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Sneak and right-click: the screen for the battery cell it runs on
        if (player.isShiftKeyDown()) {
            net.nexium.util.Menus.open((ServerPlayer) player, stack.getHoverName(), net.nexium.energy.CellSource.of(hand),
                    (id, inv, p) -> new net.nexium.energy.CellMenu(id, inv, net.nexium.energy.CellSource.of(hand)));
            return InteractionResult.SUCCESS;
        }
        GlobalPos link = stack.get(ModComponents.TERMINAL_LINK);
        if (link == null) {
            player.sendOverlayMessage(Component.translatable("message.nexium.tablet_unlinked").withStyle(ChatFormatting.RED));
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
            player.sendOverlayMessage(Component.translatable("message.nexium.tablet_terminal_missing").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        // It runs on a battery cell: opening costs NE (more from another dimension), and so does keeping the screen open
        long cost = openCost(level, link);
        if (net.nexium.energy.ItemEnergy.stored(stack) < cost) {
            player.sendOverlayMessage(Component.translatable(net.nexium.energy.ItemEnergy.chargeable(stack) ? "message.nexium.tablet_no_charge" : "message.nexium.tablet_no_cell", cost)
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        net.nexium.energy.ItemEnergy.extract(stack, cost, false);
        long[] lastPaid = {-1};
        TerminalBlock.open((ServerPlayer) player, target, pos, true,
                p -> holdsLinkedTablet(p, link) && target.isLoaded(pos) && target.getBlockState(pos).getBlock() instanceof TerminalBlock
                        && payUpkeep(p, link, lastPaid),
                null);
        return InteractionResult.SUCCESS;
    }

    /** Pays this tick's upkeep once, from the linked tablet in hand; false when it cannot (the screen closes). */
    private static boolean payUpkeep(Player p, GlobalPos link, long[] lastPaid) {
        if (lastPaid[0] == p.tickCount) return true;
        lastPaid[0] = p.tickCount;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.getItem() instanceof StorageTabletItem && link.equals(s.get(ModComponents.TERMINAL_LINK))) {
                return net.nexium.energy.ItemEnergy.extract(s, UPKEEP, false) >= UPKEEP;
            }
        }
        return false;
    }

    private static boolean holdsLinkedTablet(Player p, GlobalPos link) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.getItem() instanceof StorageTabletItem && link.equals(s.get(ModComponents.TERMINAL_LINK))) return true;
        }
        return false;
    }

    private static void offline(Player player) {
        player.sendOverlayMessage(Component.translatable("message.nexium.network_offline").withStyle(ChatFormatting.RED));
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
            tooltip.accept(Component.translatable("tooltip.nexium.tablet_unlinked").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.nexium.tablet_linked", describe(link)).withStyle(ChatFormatting.AQUA));
        }
        long capacity = net.nexium.energy.ItemEnergy.capacity(stack);
        java.text.NumberFormat f = java.text.NumberFormat.getIntegerInstance(java.util.Locale.ROOT);
        tooltip.accept(capacity <= 0 ? Component.translatable("tooltip.nexium.tablet_no_cell").withStyle(ChatFormatting.RED)
                : Component.translatable("tooltip.nexium.cell", f.format(net.nexium.energy.ItemEnergy.stored(stack)), f.format(capacity)).withStyle(ChatFormatting.GRAY));
    }
}
