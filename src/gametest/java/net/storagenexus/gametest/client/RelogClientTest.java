package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.barrel.BarrelTier;
import net.storagenexus.barrel.TieredBarrelBlockEntity;
import net.storagenexus.registry.ModBlocks;

/** Barrels keep their items through a real save and quit and reopening the world (they once came back empty). */
public class RelogClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        BlockPos[] at = new BlockPos[2];
        TestWorldSave save;
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                BlockPos base = server.getPlayerList().getPlayers().get(0).blockPosition();
                at[0] = base.offset(2, 0, 0);
                at[1] = base.offset(-2, 0, 0);
                level.setBlockAndUpdate(at[0], ModBlocks.BARRELS.get(BarrelTier.IRON).defaultBlockState());
                level.setBlockAndUpdate(at[1], ModBlocks.BARRELS.get(BarrelTier.NETHERITE).defaultBlockState());
                ((TieredBarrelBlockEntity) level.getBlockEntity(at[0])).setItem(0, new ItemStack(Items.DIAMOND, 5));
                TieredBarrelBlockEntity netherite = (TieredBarrelBlockEntity) level.getBlockEntity(at[1]);
                netherite.setItem(netherite.getContainerSize() - 1, new ItemStack(Items.OAK_PLANKS, 64));
            });
            ctx.waitTicks(5);
            save = world.getWorldSave();
        } // save and quit

        try (TestSingleplayerContext world = save.open()) {
            ctx.waitTicks(20);
            String problem = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                if (!(level.getBlockEntity(at[0]) instanceof TieredBarrelBlockEntity iron)) return "the Iron Barrel is gone";
                if (!(level.getBlockEntity(at[1]) instanceof TieredBarrelBlockEntity netherite)) return "the Netherite Barrel is gone";
                ItemStack first = iron.getItem(0), last = netherite.getItem(netherite.getContainerSize() - 1);
                if (!first.is(Items.DIAMOND) || first.getCount() != 5) return "Iron Barrel slot 1 holds " + first + " instead of 5 diamonds";
                if (!last.is(Items.OAK_PLANKS) || last.getCount() != 64) return "Netherite Barrel's last slot holds " + last + " instead of 64 planks";
                return null;
            });
            if (problem != null) throw new AssertionError("After reopening the world: " + problem);
            System.out.println("[nexus] barrels kept their items through save and quit");
        }
    }
}
