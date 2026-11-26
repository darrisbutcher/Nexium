package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.drawer.DrawerInteraction;
import net.nexium.registry.ModBlocks;
import net.nexium.registry.ModItems;


/**
 * The six conduit items from before the Conduit became one item (Item, Fluid and Energy, Tier 1 and 2). They are no longer made or shown,
 * but they still place what they always did, and a player carrying one is handed a {@link ConduitItem} instead (an Advanced Conduit for a
 * Tier 2 one), so nothing is lost.
 */
public class LegacyConduitItem extends Item {
    private final ConduitType type;
    private final int tier;

    public LegacyConduitItem(Properties properties, ConduitType type, int tier) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public ConduitType type() { return type; }
    public int tier() { return tier; }

    /** Swaps a carried stack of this item for the same number of Conduits (Advanced Conduits when it was Tier 2). */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity owner,
                              net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (stack.isEmpty() || !(owner instanceof Player player)) return;
        int count = stack.getCount();
        stack.shrink(count);
        DrawerInteraction.give(player, new ItemStack(tier >= 2 ? ModItems.ADVANCED_CONDUIT : ModItems.CONDUIT, count));
    }

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

}
