package net.storagenexus.aiot.module;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
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
 * An AIOT module. Put it in a module slot in the tool menu (G while holding an AIOT); the menu also switches it on or
 * off. The Nexus Link module is linked to a Crafting Terminal by sneak + right-clicking the terminal with it (or with
 * the AIOT it is in).
 */
public class ModuleItem extends Item {
    private final ModuleType type;

    public ModuleItem(Properties properties, ModuleType type) {
        super(properties);
        this.type = type;
    }

    public ModuleType type() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (type != ModuleType.NEXUS_LINK) return InteractionResult.PASS;
        return linkTo(context, link -> context.getItemInHand().set(ModComponents.TERMINAL_LINK, link), "message.storagenexus.nexus_link.linked")
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /**
     * Sneak + right-click on a Crafting Terminal: links to it, with {@code messageKey} above the hotbar. Shared with the
     * AIOT, which relinks its installed Nexus Link the same way, and the Nexium chestplate (Nexus Restock). Returns
     * whether the click was a link.
     */
    public static boolean linkTo(UseOnContext context, Consumer<GlobalPos> setLink, String messageKey) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isSecondaryUseActive()) return false;
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof TerminalBlock)) return false;
        if (!level.isClientSide()) {
            GlobalPos link = GlobalPos.of(level.dimension(), context.getClickedPos().immutable());
            setLink.accept(link);
            player.sendOverlayMessage(Component.translatable(messageKey, describe(link)));
        }
        return true;
    }

    public static Component describe(GlobalPos link) {
        BlockPos p = link.pos();
        return Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ() + " (" + link.dimension().identifier().getPath() + ")");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (type == ModuleType.NEXUS_LINK) {
            GlobalPos link = stack.get(ModComponents.TERMINAL_LINK);
            tooltip.accept(link == null
                    ? Component.translatable("tooltip.storagenexus.nexus_link.unlinked").withStyle(ChatFormatting.RED)
                    : Component.translatable("tooltip.storagenexus.nexus_link.linked", describe(link)).withStyle(ChatFormatting.AQUA));
        }
        if (stack.has(ModComponents.MODULE_OFF)) {
            tooltip.accept(Component.translatable("tooltip.storagenexus.module_off").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
