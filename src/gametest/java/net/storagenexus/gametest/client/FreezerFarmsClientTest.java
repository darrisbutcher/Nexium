package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.storagenexus.client.screen.FreezerScreen;
import net.storagenexus.essentials.client.FarmScreen;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.freezer.FreezerBlock;
import net.storagenexus.freezer.FreezerBlockEntity;
import net.storagenexus.freezer.FreezerMenu;
import net.storagenexus.freezer.FreezerMode;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.storagenexus.registry.ModBlocks;

/**
 * The Freezer and the farms on the real client: dyed and undyed Freezers are drawn (screenshots), its screen opens and its
 * mode button switches what it makes, and a farm's screen opens. Screenshots show them.
 */
public class FreezerFarmsClientTest implements FabricClientGameTest {
    private static BlockPos base;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                base = player.blockPosition();
                var level = player.level();
                // A row of blocks in front of the player, all facing it (north)
                for (int x = -3; x <= 3; x++) {
                    level.setBlockAndUpdate(base.offset(x, 0, 2), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(base.offset(x, -1, 2), Blocks.STONE.defaultBlockState());
                }
                int[] dyes = {-1, 0xB02E26, 0x3C44AA, 0x80C71F};
                for (int i = 0; i < dyes.length; i++) {
                    BlockPos pos = base.offset(i - 3, 0, 2);
                    level.setBlockAndUpdate(pos, ModBlocks.FREEZER.defaultBlockState().setValue(FreezerBlock.FACING, net.minecraft.core.Direction.NORTH));
                    FreezerBlockEntity freezer = (FreezerBlockEntity) level.getBlockEntity(pos);
                    ItemStack item = new ItemStack(ModBlocks.FREEZER);
                    if (dyes[i] >= 0) item.set(DataComponents.DYED_COLOR, new DyedItemColor(dyes[i]));
                    freezer.applyComponentsFromItemStack(item);
                    freezer.setWater(5 * FluidConstants.BUCKET);
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
                level.setBlockAndUpdate(base.offset(1, 0, 2), net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.SHULKER).defaultBlockState());
                level.setBlockAndUpdate(base.offset(2, 0, 2), net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.PIGLIN_BARTER).defaultBlockState());
                level.setBlockAndUpdate(base.offset(3, 0, 2), net.storagenexus.essentials.registry.ModBlocks.FARMS.get(FarmKind.WITHER_SKELETON).defaultBlockState());
                player.snapTo(base.getX() + 0.5, base.getY(), base.getZ() - 1.5, 0.0F, 10.0F);
            });
            ctx.waitTicks(15);
            System.out.println("[nexus] freezers and farms: " + ctx.takeScreenshot("nexus_freezers_farms"));

            // The Freezer's screen, and its mode button
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.openMenu((FreezerBlockEntity) player.level().getBlockEntity(base.offset(-3, 0, 2)));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FreezerScreen), "right-clicking a Freezer opens its screen");
            check(ctx.computeOnClient(mc -> ((FreezerScreen) mc.gui.screen()).getMenu().mode() == FreezerMode.ICE), "it starts on ice");
            int milli = ctx.computeOnClient(mc -> ((FreezerScreen) mc.gui.screen()).getMenu().waterMilli());
            check(milli > 4000 && milli <= 5000, "the tank shows about 5 buckets (it has started freezing), got " + milli);
            System.out.println("[nexus] freezer screen: " + ctx.takeScreenshot("nexus_freezer_screen"));
            ctx.computeOnClient(mc -> {
                mc.gameMode.handleInventoryButtonClick(((FreezerScreen) mc.gui.screen()).getMenu().containerId, FreezerMenu.BTN_MODE);
                return null;
            });
            ctx.waitTicks(10);
            String label = ctx.computeOnClient(mc -> FreezerScreen.modeLabel(((FreezerScreen) mc.gui.screen()).getMenu().mode()).getString());
            check(label.equals("Packed Ice"), "the mode button switches to packed ice, got: " + label);
            System.out.println("[nexus] freezer packed ice: " + ctx.takeScreenshot("nexus_freezer_packed"));
            ctx.computeOnClient(mc -> {
                mc.player.closeContainer();
                return null;
            });
            ctx.waitTicks(5);

            // A farm's screen
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                FarmBlockEntity farm = (FarmBlockEntity) player.level().getBlockEntity(base.offset(2, 0, 2));
                farm.setItem(FarmBlockEntity.INPUT_SLOT, new ItemStack(Items.GOLD_INGOT, 12));
                farm.setItem(0, new ItemStack(Items.ENDER_PEARL, 6));
                farm.setItem(1, new ItemStack(Items.OBSIDIAN, 3));
                farm.setProgress(40);
                player.openMenu(farm);
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmScreen), "right-clicking a farm opens its screen");
            check(ctx.computeOnClient(mc -> ((FarmScreen) mc.gui.screen()).getMenu().kind() == FarmKind.PIGLIN_BARTER), "it is the barter farm");
            System.out.println("[nexus] barter farm screen: " + ctx.takeScreenshot("nexus_barter_screen"));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
