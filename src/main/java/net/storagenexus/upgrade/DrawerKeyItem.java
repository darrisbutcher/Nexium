package net.storagenexus.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Drawer Key: right-click a drawer, compacting drawer or fluid drawer with it to lock it, and again to unlock it. It is
 * never used up, and any key works on any drawer. (Its registry id is still {@code lock_upgrade}: the Lock Upgrade it
 * replaces, in old worlds and chests, is this item.)
 */
public class DrawerKeyItem extends Item {
    public DrawerKeyItem(Properties properties) {
        super(properties);
    }

    /** Flips the drawer's lock, with a message and a click; false if this is not a lockable block. Only acts on the server. */
    public static boolean toggle(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Lockable lockable)) return false;
        if (!level.isClientSide()) {
            boolean now = !lockable.isLocked();
            lockable.setLocked(now);
            player.sendOverlayMessage(Component.translatable(now ? "message.storagenexus.drawer_locked" : "message.storagenexus.drawer_unlocked"));
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, now ? 1.3F : 0.9F);
        }
        return true;
    }

    /** Sneaking skips the block's own right-click, so the key works then too. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player != null && toggle(context.getLevel(), context.getClickedPos(), player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
