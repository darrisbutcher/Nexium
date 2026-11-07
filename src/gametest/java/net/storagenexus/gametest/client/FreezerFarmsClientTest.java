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
import net.storagenexus.essentials.assembler.AssemblyTarget;
import net.storagenexus.essentials.assembler.FarmAssemblerBlockEntity;
import net.storagenexus.essentials.assembler.FarmAssemblerMenu;
import net.storagenexus.essentials.client.FarmAssemblerScreen;
import net.storagenexus.essentials.client.FarmScreen;
import net.storagenexus.essentials.farm.FarmBlockEntity;
import net.storagenexus.essentials.farm.FarmKind;
import net.storagenexus.freezer.FreezerBlock;
import net.storagenexus.freezer.FreezerBlockEntity;
import net.storagenexus.freezer.FreezerMenu;
import net.storagenexus.freezer.FreezerMode;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.storagenexus.registry.ModBlocks;
import net.storagenexus.util.Menus;

/**
 * The Freezer and the farms on the real client: dyed and undyed Freezers are drawn (screenshots), its screen opens and its
 * mode button switches what it makes, a farm's screen opens, and the Farm Assembler fills from the inventory and builds a
 * farm. Screenshots show them.
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
            ctx.computeOnClient(mc -> {
                mc.player.closeContainer();
                return null;
            });
            ctx.waitTicks(5);

            // The Farm Assembler, part-filled for a Shulker Farm, with the rest in the player's inventory
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base.offset(3, 0, 2);
                player.level().setBlockAndUpdate(pos, net.storagenexus.essentials.registry.ModBlocks.FARM_ASSEMBLER.defaultBlockState());
                FarmAssemblerBlockEntity assembler = (FarmAssemblerBlockEntity) player.level().getBlockEntity(pos);
                assembler.select(AssemblyTarget.SHULKER, null);
                assembler.accept(new ItemStack(Items.SHULKER_SPAWN_EGG, 4));
                assembler.accept(new ItemStack(Items.PURPUR_BLOCK, 32));
                assembler.accept(new ItemStack(Items.END_ROD, 5));
                assembler.accept(new ItemStack(Items.ENDER_PEARL, 8));
                for (int i = 0; i < 30; i++) assembler.accept(new ItemStack(Items.END_STONE_BRICKS, 64));
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.END_ROD, 3));
                for (int i = 0; i < 17; i++) player.getInventory().setItem(9 + i, new ItemStack(Items.END_STONE_BRICKS, 64));
                player.getInventory().setItem(27, new ItemStack(Items.DIRT, 12));
                Menus.open(player, net.minecraft.network.chat.Component.translatable("block.nexusessentials.farm_assembler"), pos,
                        (id, inventory, p) -> new FarmAssemblerMenu(id, inventory, assembler));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof FarmAssemblerScreen), "the Farm Assembler's screen opens");
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().target() == AssemblyTarget.SHULKER), "on the Shulker Farm");
            check(!ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().complete()), "not complete yet");
            System.out.println("[nexus] farm assembler: " + ctx.takeScreenshot("nexus_farm_assembler"));

            // Add from inventory takes the 3 end rods and 1,088 end stone bricks it still needs; then Build
            clickAt(ctx, FarmAssemblerMenu.LIST_X + FarmAssemblerMenu.LIST_W / 2, FarmAssemblerMenu.ADD_Y + 7);
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().complete()), "Add from inventory completed it");
            System.out.println("[nexus] farm assembler ready: " + ctx.takeScreenshot("nexus_farm_assembler_ready"));
            clickAt(ctx, FarmAssemblerMenu.BUILD_X + FarmAssemblerMenu.BUILD_W / 2, FarmAssemblerMenu.HEADER_Y + 8);
            check(ctx.computeOnClient(mc -> ((FarmAssemblerScreen) mc.gui.screen()).getMenu().slots.get(0).getItem()
                    .is(net.storagenexus.essentials.registry.ModItems.FARMS.get(FarmKind.SHULKER))), "Build made a Shulker Farm");
            check(ctx.computeOnClient(mc -> mc.player.getInventory().countItem(Items.DIRT) == 12 && mc.player.getInventory().countItem(Items.END_STONE_BRICKS) == 0),
                    "the dirt stayed and the bricks went in");
            System.out.println("[nexus] farm assembler built: " + ctx.takeScreenshot("nexus_farm_assembler_built"));
            ctx.computeOnClient(mc -> {
                mc.player.closeContainer();
                return null;
            });
            ctx.waitTicks(5);

            // An anvil job over 40 levels: the screen shows its full cost, not "Too Expensive!", and the result is there
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = base.offset(-3, 0, 4);
                var level = player.level();
                level.setBlockAndUpdate(pos, Blocks.ANVIL.defaultBlockState());
                player.setExperienceLevels(100);
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.set(DataComponents.REPAIR_COST, 63);
                var sharpness = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
                ItemStack book = net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(sharpness, 5));
                player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> {
                    var anvil = new net.minecraft.world.inventory.AnvilMenu(id, inv, net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos));
                    anvil.getSlot(0).set(sword);
                    anvil.getSlot(1).set(book);
                    return anvil;
                }, net.minecraft.network.chat.Component.translatable("container.repair")));
            });
            ctx.waitTicks(10);
            check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen), "the anvil screen opens");
            int anvilCost = ctx.computeOnClient(mc -> ((net.minecraft.world.inventory.AnvilMenu) mc.player.containerMenu).getCost());
            check(anvilCost >= 40, "the anvil asks the full cost, got " + anvilCost);
            check(ctx.computeOnClient(mc -> !mc.player.containerMenu.getSlot(2).getItem().isEmpty()), "the anvil's result is there");
            System.out.println("[nexus] anvil over 40 levels (" + anvilCost + "): " + ctx.takeScreenshot("nexus_anvil_full_cost"));
        }
    }

    /** Clicks at a point given in the Farm Assembler screen's own coordinates. */
    private static void clickAt(ClientGameTestContext ctx, int guiX, int guiY) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            int left = (screen.width - FarmAssemblerMenu.WIDTH) / 2, top = (screen.height - FarmAssemblerMenu.HEIGHT) / 2;
            return new double[]{(left + guiX) * scale, (top + guiY) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        ctx.waitTicks(6);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
