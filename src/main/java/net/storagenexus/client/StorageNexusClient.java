package net.storagenexus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.storagenexus.client.render.CompactingDrawerRenderer;
import net.storagenexus.client.render.DrawerRenderer;
import net.storagenexus.client.render.FacadeRenderer;
import net.storagenexus.client.render.FluidDrawerRenderer;
import net.storagenexus.client.render.LinkLabelRenderer;
import net.storagenexus.client.screen.ConduitConfigScreen;
import net.storagenexus.client.screen.ConnectorScreen;
import net.storagenexus.client.screen.CraftingTerminalScreen;
import net.storagenexus.client.screen.DimensionalScreen;
import net.storagenexus.client.screen.FilterScreen;
import net.storagenexus.client.screen.TerminalScreen;
import net.storagenexus.client.screen.UpgradeScreen;
import net.storagenexus.guide.GuideBookItem;
import net.storagenexus.registry.ModBlockEntities;
import net.storagenexus.registry.ModMenus;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.terminal.TerminalSyncPayload;

public class StorageNexusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.UPGRADES, UpgradeScreen::new);
        MenuScreens.register(ModMenus.CONNECTOR, ConnectorScreen::new);
        MenuScreens.<TerminalMenu, AbstractContainerScreen<TerminalMenu>>register(ModMenus.TERMINAL,
                (menu, inventory, title) -> menu.isCrafting()
                        ? new CraftingTerminalScreen(menu, inventory, title)
                        : new TerminalScreen(menu, inventory, title));
        // Terminals show as many rows as fit the window
        TerminalMenu.clientRows = crafting -> TerminalMenu.rowsFor(crafting,
                Minecraft.getInstance().getWindow().getGuiScaledHeight());
        MenuScreens.register(ModMenus.DIMENSIONAL, DimensionalScreen::new);
        MenuScreens.register(ModMenus.CONDUIT_CONFIG, ConduitConfigScreen::new);
        MenuScreens.register(ModMenus.FILTER, FilterScreen::new);

        // Front displays and labels. If any of these fail to compile on a newer 26.x build,
        // the mod still works without them: delete the line and the matching renderer class.
        BlockEntityRenderers.register(ModBlockEntities.DRAWER, DrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COMPACTING_DRAWER, CompactingDrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FLUID_DRAWER, FluidDrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_INPUT, LinkLabelRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_OUTPUT, LinkLabelRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CONDUIT_BUNDLE, FacadeRenderer::new);

        ItemDescriptions.init();
        ContentsTooltip.init();
        GuideBookItem.openScreen = GuideScreen::open;

        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, (payload, context) -> {
            if (context.client().player != null && context.client().player.containerMenu instanceof TerminalMenu menu) {
                menu.receiveSync(payload);
            }
        });
    }
}
