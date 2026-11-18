package net.nexium.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nexium.drawer.DrawerInteraction;
import net.nexium.util.Menus;

/**
 * Right-click a conduit side: cycle that side's mode (first conduit type in the bundle).
 * Sneak + right-click: open the full side configuration screen.
 * Right-click a facade: remove it.
 */
public class ConduitWrenchItem extends Item {
    public ConduitWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level.getBlockEntity(pos) instanceof ConduitBundleBlockEntity be) || player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction side = targetSide(context, ConduitBundleBlock.mask(level.getBlockState(pos)));
        if (player.isSecondaryUseActive()) {
            Menus.open((ServerPlayer) player, Component.translatable("screen.nexium.conduit_config"),
                    new ConduitConfigMenu.OpenData(pos, side.get3DDataValue()),
                    (id, inv, p) -> new ConduitConfigMenu(id, inv, be, side.get3DDataValue()));
            return InteractionResult.SUCCESS;
        }
        if (be.facade() != null) {
            DrawerInteraction.give(player, new ItemStack(be.facade().getBlock()));
            be.setFacade(null);
            return InteractionResult.SUCCESS;
        }
        for (ConduitType t : ConduitType.VALUES) {
            if (!be.has(t)) continue;
            SideConfig cfg = be.config(t, side);
            cfg.mode = cfg.mode.next();
            be.changed();
            player.sendOverlayMessage(Component.translatable("message.nexium.conduit_mode",
                    Component.translatable("conduit.nexium." + t.key()),
                    Component.translatable("direction.nexium." + side.getSerializedName()),
                    Component.translatable("mode.nexium." + cfg.mode.name().toLowerCase(java.util.Locale.ROOT))));
            break;
        }
        return InteractionResult.SUCCESS;
    }

    /** The arm that was clicked, or the clicked face if the core was hit. */
    static Direction targetSide(UseOnContext context, int mask) {
        Vec3 local = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.get3DDataValue())) == 0) continue;
            if (ConduitBundleBlock.SHAPES.arm(d).bounds().inflate(0.01).contains(local)) return d;
        }
        return context.getClickedFace();
    }
}
