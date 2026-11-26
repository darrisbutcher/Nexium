package net.nexium.conduit;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The Advanced Conduit Upgrade: right-click a conduit to make it Tier 2 (items, fluids and energy all move faster). */
public class ConduitUpgradeItem extends Item {
    public ConduitUpgradeItem(Properties properties) {
        super(properties);
    }

    /** Raises every type the conduit has to Tier 2. Returns whether anything changed. */
    public static boolean upgrade(ConduitBundleBlockEntity be) {
        boolean changed = false;
        for (ConduitType t : ConduitType.VALUES) {
            if (!be.has(t) || be.tier(t) >= 2) continue;
            be.addConduit(t, 2);
            changed = true;
        }
        return changed;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof ConduitBundleBlockEntity be)) return InteractionResult.PASS;
        if (be.typeMask() != 0 && !hasTier1(be)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            upgrade(be);
            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, context.getClickedPos(), SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1.4f);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean hasTier1(ConduitBundleBlockEntity be) {
        for (ConduitType t : ConduitType.VALUES) if (be.has(t) && be.tier(t) < 2) return true;
        return false;
    }
}
