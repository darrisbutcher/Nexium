package net.nexium.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.nexium.drawer.DrawerClickPayload;
import net.nexium.drawer.DrawerInteraction;
import net.nexium.essentials.client.NexiumEssentialsClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.nexium.client.guide.CodexClient;
import net.nexium.client.guide.GuideScreens;
import net.nexium.client.render.CompactingDrawerRenderer;
import net.nexium.client.render.DrawerRenderer;
import net.nexium.client.render.FacadeRenderer;
import net.nexium.client.render.FluidDrawerRenderer;
import net.nexium.client.render.LinkLabelRenderer;
import net.nexium.client.screen.ConduitConfigScreen;
import net.nexium.client.screen.BarrelScreen;
import net.nexium.client.screen.ConnectorScreen;
import net.nexium.client.screen.LevelEmitterScreen;
import net.nexium.client.screen.NetworkBusScreen;
import net.nexium.client.screen.CraftingTerminalScreen;
import net.nexium.client.screen.DimensionalScreen;
import net.nexium.client.screen.FilterScreen;
import net.nexium.client.screen.UpgradeScreen;
import net.nexium.guide.GuideBookItem;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModMenus;
import net.nexium.terminal.TerminalMenu;
import net.nexium.terminal.TerminalSyncPayload;

public class NexiumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.nexium.client.dog.DogModel.LAYER,
                () -> net.nexium.client.dog.DogModel.createLayer(net.minecraft.client.model.geom.builders.CubeDeformation.NONE));
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.nexium.client.dog.DogModel.ARMOR_LAYER,
                () -> net.nexium.client.dog.DogModel.createLayer(new net.minecraft.client.model.geom.builders.CubeDeformation(0.4F)));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(net.nexium.dog.ModDogs.DOG, net.nexium.client.dog.DogRenderer::new);
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
        MenuScreens.register(ModMenus.DISK_DRIVE, net.nexium.client.screen.DiskDriveScreen::new);
        MenuScreens.register(ModMenus.FREEZER, net.nexium.client.screen.FreezerScreen::new);
        MenuScreens.register(ModMenus.REDSTONE_REACTOR, net.nexium.client.screen.RedstoneReactorScreen::new);
        MenuScreens.register(ModMenus.NEXUS_FURNACE, net.nexium.client.screen.NexusFurnaceScreen::new);
        MenuScreens.register(ModMenus.THERMAL_GENERATOR, net.nexium.client.screen.ThermalGeneratorScreen::new);
        MenuScreens.register(ModMenus.BATTERY, net.nexium.client.screen.BatteryScreen::new);
        MenuScreens.register(ModMenus.COMBUSTION_GENERATOR, net.nexium.client.screen.CombustionGeneratorScreen::new);
        FreezerTint.init();

        // Front displays and labels. If any of these fail to compile on a newer 26.x build,
        // the mod still works without them: delete the line and the matching renderer class.
        BlockEntityRenderers.register(ModBlockEntities.DRAWER, DrawerRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.DISK_DRIVE, net.nexium.client.render.DiskDriveRenderer::new);
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
        Tooltips.init();
        GuideBookItem.openScreen = GuideScreens::open;
        CodexClient.init();
        SortClient.init();
        net.nexium.client.screen.ConfigKey.init();
        ShiftTransferClient.init();
        ScrollTransferClient.init();
        NexiumEssentialsClient.init();
        ToolMenuClient.init();
        AiotPreview.init();
        ScannerOutline.init();
        DiagonalPreview.init();
        net.nexium.essentials.client.FreeCamera.init();
        net.nexium.essentials.client.Zoom.init();
        AiotHud.init();

        ClientPlayNetworking.registerGlobalReceiver(net.nexium.elevator.ElevatorOpenPayload.TYPE, (payload, context) ->
                context.client().gui.setScreen(new net.nexium.client.screen.ElevatorScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(net.nexium.energy.PowerReportPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (payload.open()) client.gui.setScreen(new net.nexium.client.screen.PowerMeterScreen(payload));
            else if (client.gui.screen() instanceof net.nexium.client.screen.PowerMeterScreen screen) screen.update(payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(net.nexium.conduit.ScanReportPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (payload.open()) client.gui.setScreen(new net.nexium.client.screen.ConduitScannerScreen(payload));
            else if (client.gui.screen() instanceof net.nexium.client.screen.ConduitScannerScreen screen) screen.update(payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(net.nexium.quest.QuestStatePayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (payload.open()) client.gui.setScreen(new net.nexium.client.screen.QuestScreen(payload));
            else if (client.gui.screen() instanceof net.nexium.client.screen.QuestScreen screen) screen.update(payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, (payload, context) -> {
            if (context.client().player != null && context.client().player.containerMenu instanceof TerminalMenu menu) {
                menu.receiveSync(payload);
            }
        });
    }
}
