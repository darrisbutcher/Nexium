package net.storagenexus.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Nexium guide: right-click to open the Nexium Codex. */
public class GuideBookItem extends Item {
    /** Set by the client entrypoint; opening a screen is client-only code. */
    public static Runnable openScreen = () -> {};

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) openScreen.run();
        return InteractionResult.SUCCESS;
    }
}
