package net.storagenexus.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.storagenexus.StorageNexus;
import net.storagenexus.conduit.ConduitConfigMenu;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.filter.FilterMenu;
import net.storagenexus.network.ConnectorMenu;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.upgrade.UpgradeMenu;

public final class ModMenus {
    public static final ExtendedScreenHandlerType<UpgradeMenu, BlockPos> UPGRADES = register("upgrades",
            new ExtendedScreenHandlerType<>(UpgradeMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedScreenHandlerType<ConnectorMenu, BlockPos> CONNECTOR = register("connector",
            new ExtendedScreenHandlerType<>(ConnectorMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedScreenHandlerType<TerminalMenu, Boolean> TERMINAL = register("terminal",
            new ExtendedScreenHandlerType<>(TerminalMenu::new, ByteBufCodecs.BOOL));
    public static final ExtendedScreenHandlerType<DimensionalMenu, DimensionalMenu.OpenData> DIMENSIONAL = register("dimensional",
            new ExtendedScreenHandlerType<>(DimensionalMenu::new, DimensionalMenu.OpenData.STREAM_CODEC));
    public static final ExtendedScreenHandlerType<ConduitConfigMenu, ConduitConfigMenu.OpenData> CONDUIT_CONFIG = register("conduit_config",
            new ExtendedScreenHandlerType<>(ConduitConfigMenu::new, ConduitConfigMenu.OpenData.STREAM_CODEC));
    public static final ExtendedScreenHandlerType<FilterMenu, Integer> FILTER = register("filter",
            new ExtendedScreenHandlerType<>(FilterMenu::new, ByteBufCodecs.VAR_INT));

    private static <T extends AbstractContainerMenu, M extends MenuType<T>> M register(String name, M type) {
        return Registry.register(BuiltInRegistries.MENU, StorageNexus.id(name), type);
    }

    public static void init() {}

    private ModMenus() {}
}
