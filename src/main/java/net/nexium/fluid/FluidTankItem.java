package net.nexium.fluid;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.nexium.registry.ModComponents;
import net.nexium.util.Fmt;

import java.util.function.Consumer;

/**
 * The Fluid Tank as an item: it carries its fluid, and works like a big bucket. Right-click a fluid source to scoop a bucket of it in
 * (the same fluid as it holds, or any if it is empty), right-click anywhere else to pour a bucket out. Sneak + right-click places the
 * tank as a block, fluid and all; an empty one places on a plain right-click too. Used on another tank, or a tank block, it moves fluid.
 */
public class FluidTankItem extends BlockItem {
    private final int tier;

    public FluidTankItem(Block block, Properties properties, int tier) {
        super(block, properties);
        this.tier = tier;
    }

    public int tier() { return tier; }
    public long capacity() { return FluidTankBlockEntity.capacity(tier); }

    public static FluidTankBlockEntity.Contents contents(ItemStack stack) {
        FluidTankBlockEntity.Contents c = stack.get(ModComponents.TANK_FLUID);
        return c == null ? FluidTankBlockEntity.Contents.EMPTY : c;
    }

    public static void setContents(ItemStack stack, FluidTankBlockEntity.Contents contents) {
        if (contents.isEmpty()) stack.remove(ModComponents.TANK_FLUID);
        else stack.set(ModComponents.TANK_FLUID, contents);
    }

    // ---- The fill bar (like a durability bar): shown in the hotbar and inventory while the tank holds fluid, in the fluid's colour ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.min(13, Math.round(13.0F * contents(stack).amount() / capacity())));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FluidTankBlockEntity.Contents c = contents(stack);
        return c.isEmpty() ? 0xFF55A0FF : 0xFF000000 | FluidVariantAttributes.getAssociatedColor(c.variant());
    }

    // ---- Bucket behaviour ---------------------------------------------------------------------

    /** Scoops the fluid source the player is looking at into the tank. Returns whether it did. */
    public static boolean scoop(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FluidTankItem item) || hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return false;
        if (!level.mayInteract(player, pos)) return false;
        FluidVariant variant = FluidVariant.of(fluidState.getType());
        FluidTankBlockEntity.Contents have = contents(stack);
        if (!have.isEmpty() && !have.variant().equals(variant)) return false;
        if (item.capacity() - have.amount() < FluidConstants.BUCKET) return false;
        if (!level.isClientSide()) {
            ItemStack bucket = pickup.pickupBlock(player, level, pos, state);
            if (bucket.isEmpty()) return false;
            setContents(stack, new FluidTankBlockEntity.Contents(variant, have.amount() + FluidConstants.BUCKET));
            level.playSound(null, pos, FluidVariantAttributes.getFillSound(variant), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    /** Pours a bucket of the tank's fluid where the player is looking. Returns whether it did. */
    public static boolean pour(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        FluidTankBlockEntity.Contents have = contents(stack);
        if (have.isEmpty() || have.amount() < FluidConstants.BUCKET || hit.getType() != HitResult.Type.BLOCK) return false;
        Fluid fluid = have.variant().getFluid();
        if (!(fluid.getBucket() instanceof BucketItem bucket)) return false;
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) return false;
        // Like a bucket: into the block itself if it can hold the fluid, else against the face that was aimed at
        BlockState state = level.getBlockState(pos);
        BlockPos place = state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer || state.canBeReplaced(fluid) ? pos : pos.relative(hit.getDirection());
        if (level.isClientSide()) return true;
        if (!bucket.emptyContents(player, level, place, hit)) return false;
        if (!player.getAbilities().instabuild) setContents(stack, new FluidTankBlockEntity.Contents(have.variant(), have.amount() - FluidConstants.BUCKET));
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (player != null && !player.isSecondaryUseActive()) {
            // The fluid the player is looking at (the ordinary hit result looks through it to the block underneath)
            BlockHitResult source = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (scoop(level, player, stack, source)) return InteractionResult.SUCCESS;
            if (pour(level, player, stack, new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), false))) return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    /** Aimed at fluid with no block to click: scoops it; aimed at nothing it does nothing. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() && scoop(level, player, stack, getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY))) {
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        FluidTankBlockEntity.Contents c = contents(stack);
        if (c.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.nexium.tank_empty", Fmt.fluid(capacity())).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.nexium.tank_holds", FluidVariantAttributes.getName(c.variant()), Fmt.fluid(c.amount()), Fmt.fluid(capacity()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.nexium.tank_use").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---- The item as a fluid container for buckets, other tanks and machines ---------------------

    /** Lets everything that moves fluid between containers treat a tank item like a bucket. */
    public static class Storage extends SingleVariantItemStorage<FluidVariant> {
        private final long capacity;

        public Storage(ContainerItemContext context, long capacity) {
            super(context);
            this.capacity = capacity;
        }

        @Override protected FluidVariant getBlankResource() { return FluidVariant.blank(); }
        @Override protected FluidVariant getResource(ItemVariant currentVariant) { return contents(currentVariant.toStack()).variant(); }
        @Override protected long getAmount(ItemVariant currentVariant) { return contents(currentVariant.toStack()).amount(); }
        @Override protected long getCapacity(FluidVariant variant) { return capacity; }

        @Override
        protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
            ItemStack stack = currentVariant.toStack();
            setContents(stack, new FluidTankBlockEntity.Contents(newResource, newAmount));
            return ItemVariant.of(stack);
        }
    }
}
