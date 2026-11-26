package net.nexium.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nexium.conduit.ConduitBundleBlockEntity;
import net.nexium.conduit.ConduitType;
import net.nexium.registry.ModItems;

/** Names what is next to a block on a given side, read from the client's copy of the world. */
final class NeighbourNames {
    private static final String[] SIDE_KEYS = {"down", "up", "north", "south", "west", "east"};

    static Component sideName(Direction side) {
        return Component.translatable("direction.nexium." + SIDE_KEYS[side.get3DDataValue()]);
    }

    /** "Chest", "Item Conduit (Tier 2), Fluid Conduit (Tier 1)", or "Nothing". */
    static Component describe(BlockPos pos, Direction side) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return Component.empty();
        BlockPos n = pos.relative(side);
        BlockState state = level.getBlockState(n);
        if (state.isAir()) return Component.translatable("screen.nexium.nothing");
        if (level.getBlockEntity(n) instanceof ConduitBundleBlockEntity bundle) {
            if (bundle.typeMask() == 7) {
                return Component.translatable("item.nexium.conduit").append(Component.translatable(bundle.tier(ConduitType.ITEM) >= 2 ? "conduit.nexium.tier2" : "conduit.nexium.tier1"));
            }
            MutableComponent out = Component.empty();
            for (ConduitType t : ConduitType.VALUES) {
                if (!bundle.has(t)) continue;
                if (!out.getSiblings().isEmpty()) out.append(", ");
                out.append(new ItemStack(ModItems.conduitFor(t, bundle.tier(t))).getHoverName());
            }
            if (!out.getSiblings().isEmpty()) return out;
        }
        return state.getBlock().getName();
    }

    private NeighbourNames() {}
}
