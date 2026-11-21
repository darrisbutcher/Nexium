package net.nexium.essentials.grindstone;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.nexium.util.Menus;
import org.jspecify.annotations.Nullable;

/**
 * A grindstone (same shape, same ways of being placed) whose menu pulls enchantments off an item onto books of their own,
 * instead of wiping them. Made from a Grindstone, a Book and Lapis Lazuli.
 */
public class EnchantedGrindstoneBlock extends GrindstoneBlock {
    public EnchantedGrindstoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Menus.open(sp, Component.translatable("block.nexium.enchanted_grindstone"), pos,
                    (id, inventory, p) -> new EnchantedGrindstoneMenu(id, inventory, ContainerLevelAccess.create(level, pos)));
            player.awardStat(Stats.INTERACT_WITH_GRINDSTONE);
        }
        return InteractionResult.SUCCESS;
    }

    /** The vanilla grindstone menu is not this block's: it has none to hand out (a spectator would otherwise get it). */
    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }
}
