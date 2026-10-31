package net.storagenexus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.storagenexus.drawer.DrawerControllerBlockEntity;
import net.storagenexus.drawer.DrawerWood;
import net.storagenexus.registry.ModBlocks;

/** Drawer Controller reach through slaves, and its reach outline. */
public class ControllerGameTest {
    @GameTest(environment = "storagenexus:isolated", padding = 40, maxTicks = 20)
    public void slavesExtendReachAlongADrawerLine(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 1), ModBlocks.DRAWER_CONTROLLER);
        for (int x = 1; x <= 30; x++) h.setBlock(new BlockPos(x, 1, 1), ModBlocks.DRAWERS.get(DrawerWood.OAK)[0]);
        DrawerControllerBlockEntity ctrl = h.getBlockEntity(new BlockPos(0, 1, 1), DrawerControllerBlockEntity.class);
        h.assertValueEqual(ctrl.drawerCount(), 12, "drawers within 12 of the controller alone");
        h.setBlock(new BlockPos(10, 2, 1), ModBlocks.CONTROLLER_SLAVE); // on top of the line, like the screenshot
        h.assertValueEqual(ctrl.drawerCount(), 22, "with a slave at x=10");
        h.setBlock(new BlockPos(21, 2, 1), ModBlocks.CONTROLLER_SLAVE); // in reach of the first slave
        h.assertValueEqual(ctrl.drawerCount(), 30, "with a second, chained slave");
        h.succeed();
    }
    @GameTest
    public void controllerOutlineToggles(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.DRAWER_CONTROLLER);
        DrawerControllerBlockEntity be = h.getBlockEntity(pos, DrawerControllerBlockEntity.class);
        h.assertTrue(be.toggleRangeDisplay(h.getLevel()), "first click shows the outline");
        h.assertTrue(!be.toggleRangeDisplay(h.getLevel()), "second click hides it");
        h.succeed();
    }
}
