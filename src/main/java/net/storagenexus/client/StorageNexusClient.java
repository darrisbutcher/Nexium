package net.storagenexus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.storagenexus.drawer.DrawerClickPayload;
import net.storagenexus.drawer.DrawerInteraction;
import net.storagenexus.essentials.client.NexusEssentialsClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.storagenexus.client.guide.CodexClient;
import net.storagenexus.client.guide.GuideScreens;
import net.storagenexus.client.render.CompactingDrawerRenderer;
import net.storagenexus.client.render.DrawerRenderer;
import net.storagenexus.client.render.FacadeRenderer;
import net.storagenexus.client.render.FluidDrawerRenderer;
import net.storagenexus.client.render.LinkLabelRenderer;
import net.storagenexus.client.screen.ConduitConfigScreen;
import net.storagenexus.client.screen.BarrelScreen;
import net.storagenexus.client.screen.ConnectorScreen;
import net.storagenexus.client.screen.LevelEmitterScreen;
import net.storagenexus.client.screen.NetworkBusScreen;
import net.storagenexus.client.screen.CraftingTerminalScreen;
import net.storagenexus.client.screen.DimensionalScreen;
import net.storagenexus.client.screen.FilterScreen;
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
        MenuScreens.register(ModMenus.NETWORK_BUS, NetworkBusScreen::new);
        MenuScreens.register(ModMenus.BARREL, BarrelScreen::new);
        MenuScreens.register(ModMenus.LEVEL_EMITTER, LevelEmitterScreen::new);
        MenuScreens.<TerminalMenu, AbstractContainerScreen<TerminalMenu>>register(ModMenus.TERMINAL, CraftingTerminalScreen::new);
        // Terminals show as many rows as fit the window
        TerminalMenu.clientRows = crafting -> TerminalMenu.rowsFor(crafting,
                Minecraft.getInstance().getWindow().getGuiScaledHeight());
        MenuScreens.register(ModMenus.DIMENSIONAL, DimensionalScreen::new);
        MenuScreens.register(ModMenus.CONDUIT_CONFIG, ConduitConfigScreen::new);
        MenuScreens.register(ModMenus.FILTER, FilterScreen::new);
        MenuScreens.register(ModMenus.DISK_DRIVE, net.storagenexus.client.screen.DiskDriveScreen::new);
        MenuScreens.register(ModMenus.FREEZER, net.storagenexus.client.screen.FreezerScreen::new);
        MenuScreens.register(ModMenus.REDSTONE_REACTOR, net.storagenexus.client.screen.RedstoneReactorScreen::new);
        MenuScreens.register(ModMenus.NEXUS_FURNACE, net.storagenexus.client.screen.NexusFurnaceScreen::new);
        MenuScreens.register(ModMenus.THERMAL_GENERATOR, net.storagenexus.client.screen.ThermalGeneratorScreen::new);
        MenuScreens.register(ModMenus.BATTERY, net.storagenexus.client.screen.BatteryScreen::new);
        MenuScreens.register(ModMenus.COMBUSTION_GENERATOR, net.storagenexus.client.screen.CombustionGeneratorScreen::new);
        FreezerTint.init();

        // Front displays and labels. If any of these fail to compile on a newer 26.x build,
        // the mod still works without them: delete the line and the matching renderer class.
        BlockEntityRenderers.register(ModBlockEntities.DRAWER, DrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DISK_DRIVE, net.storagenexus.client.render.DiskDriveRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.COMPACTING_DRAWER, CompactingDrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.FLUID_DRAWER, FluidDrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_INPUT, LinkLabelRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DIMENSIONAL_OUTPUT, LinkLabelRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.CONDUIT_BUNDLE, FacadeRenderer::new);

        // Drawer fronts: one extraction per real left click, however fast; holding the button doesn't repeat
        ClientPreAttackCallback.EVENT.register((client, player, clicks) -> {
            if (client.level == null || player.isSpectator()) return false;
            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
            BlockPos pos = hit.getBlockPos();
            int slot = DrawerInteraction.frontSlot(client.level.getBlockState(pos), client.level.getBlockEntity(pos), hit);
            if (slot < 0) return false;
            for (int i = 0; i < clicks; i++) ClientPlayNetworking.send(new DrawerClickPayload(pos, slot));
            if (clicks > 0) player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
            return true; // the front is never mined, and a held button does nothing more
        });

        ItemDescriptions.init();
        ContentsTooltip.init();
        GuideBookItem.openScreen = GuideScreens::open;
        CodexClient.init();
        SortClient.init();
        ShiftTransferClient.init();
        NexusEssentialsClient.init();
        ToolMenuClient.init();
        AiotPreview.init();
        AiotHud.init();

        ClientPlayNetworking.registerGlobalReceiver(net.storagenexus.elevator.ElevatorOpenPayload.TYPE, (payload, context) ->
                context.client().gui.setScreen(new net.storagenexus.client.screen.ElevatorScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, (payload, context) -> {
            if (context.client().player != null && context.client().player.containerMenu instanceof TerminalMenu menu) {
                menu.receiveSync(payload);
            }
        });
    }
}
