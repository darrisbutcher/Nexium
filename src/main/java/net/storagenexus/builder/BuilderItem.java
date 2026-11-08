package net.storagenexus.builder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.storagenexus.aiot.module.ModuleItem;
import net.storagenexus.registry.ModItems;
import net.storagenexus.network.TerminalBlock;

import java.util.function.Consumer;

/**
 * The Construction Builder: right-click a block to build out from it (see {@link BuilderPlan}), using blocks from the
 * inventory and then from a storage network through its Nexus Link. Its mode, plane, size, module and Undo are in the
 * tool menu (G). Sneak + right-click a Crafting Terminal links its Nexus Link there.
 */
public class BuilderItem extends Item {
    private final BuilderTier tier;

    public BuilderItem(Properties properties, BuilderTier tier) {
        super(properties);
        this.tier = tier;
    }

    public BuilderTier tier() {
        return tier;
    }

    /** One per stack; durability (none for Nexium), fire resistance from netherite up, repaired with its material. */
    public static Properties properties(BuilderTier tier) {
        Properties p = new Properties().stacksTo(1);
        if (tier.durability() > 0) p.durability(tier.durability());
        if (tier.fireResistant()) p.fireResistant();
        switch (tier) {
            case IRON -> p.repairable(Items.IRON_INGOT);
            case DIAMOND -> p.repairable(Items.DIAMOND);
            case NETHERITE -> p.repairable(Items.NETHERITE_INGOT);
            case NEXIUM -> {}
        }
        return p;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack builder = context.getItemInHand();
        // Sneak + right-click a Crafting Terminal: links the Nexus Link in its module slot
        if (context.isSecondaryUseActive() && context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof TerminalBlock) {
            ItemStack module = BuilderSettings.module(builder);
            if (!(module.getItem() instanceof ModuleItem item) || item.type() != net.storagenexus.aiot.module.ModuleType.NEXUS_LINK) {
                if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
                    context.getPlayer().sendOverlayMessage(Component.translatable("message.storagenexus.builder.needs_link").withStyle(ChatFormatting.RED));
                }
                return InteractionResult.SUCCESS;
            }
            ModuleItem.linkTo(context, link -> {
                ItemStack relinked = module.copy();
                relinked.set(net.storagenexus.registry.ModComponents.TERMINAL_LINK, link);
                BuilderSettings.setModule(builder, relinked);
            }, "message.storagenexus.nexus_link.linked");
            return InteractionResult.SUCCESS;
        }
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
            BuilderService.build(level, player, builder, context.getClickedPos(), context.getClickedFace());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.storagenexus.builder.blocks", BuilderSettings.size(stack), tier.maxBlocks())
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.storagenexus.builder.mode",
                Component.translatable("screen.storagenexus.builder.mode." + BuilderSettings.mode(stack).name().toLowerCase()),
                Component.translatable("screen.storagenexus.builder.plane." + BuilderSettings.plane(stack).name().toLowerCase()))
                .withStyle(ChatFormatting.GRAY));
        ItemStack module = BuilderSettings.module(stack);
        if (!module.isEmpty()) {
            GlobalPos link = BuilderSettings.activeLink(stack);
            tooltip.accept(link != null
                    ? Component.translatable("tooltip.storagenexus.builder.link", ModuleItem.describe(link)).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("tooltip.storagenexus.builder.link_off").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** The builder of this tier. */
    public static BuilderItem of(BuilderTier tier) {
        return ModItems.BUILDERS.get(tier);
    }
}
