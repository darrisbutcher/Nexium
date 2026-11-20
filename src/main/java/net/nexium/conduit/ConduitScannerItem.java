package net.nexium.conduit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Conduit Scanner: right-click any conduit to open a window with everything on its network (what each side connects to, whether it
 * extracts or inserts, its channels, and warnings), one conduit type at a time. It changes nothing; the Conduit Wrench does that.
 */
public class ConduitScannerItem extends Item {
    public ConduitScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ConduitBundleBlockEntity)) return InteractionResult.PASS;
        if (context.getLevel().isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        ConduitScanner.open(player, context.getClickedPos());
        return InteractionResult.SUCCESS;
    }
}
