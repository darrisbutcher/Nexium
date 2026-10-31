package net.storagenexus.conduit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.storagenexus.drawer.DrawerInteraction;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.registry.ModItems;

import java.util.function.Consumer;

/**
 * Places a conduit. Clicking an existing conduit bundle adds this conduit type to it (several
 * types share one block), or upgrades the tier and gives the old conduit back.
 */
public class ConduitItem extends Item {
    private final ConduitType type;
    private final int tier;

    public ConduitItem(Properties properties, ConduitType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public ConduitType type() { return type; }
    public int tier() { return tier; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockEntity(context.getClickedPos()) instanceof ConduitBundleBlockEntity be && be.tier(type) < tier) {
            if (!level.isClientSide()) {
                int old = be.addConduit(type, tier);
                if (old > 0 && player != null) DrawerInteraction.give(player, new ItemStack(ModItems.conduitFor(type, old)));
                if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, context.getClickedPos(), SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }

        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos pos = place.getClickedPos();
        if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity existing) {
            if (existing.tier(type) >= tier) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                int old = existing.addConduit(type, tier);
                if (old > 0 && player != null) DrawerInteraction.give(player, new ItemStack(ModItems.conduitFor(type, old)));
                if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (!place.canPlace()) return InteractionResult.FAIL;
        BlockState state = ModBlocks.CONDUIT_BUNDLE.defaultBlockState();
        if (!level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlock(pos, state, 3);
            if (level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be) be.addConduit(type, tier);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
