package net.storagenexus.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.storagenexus.StorageNexus;
import net.storagenexus.barrel.BarrelMenu;
import net.storagenexus.conduit.ConduitConfigMenu;
import net.storagenexus.dimension.DimensionalMenu;
import net.storagenexus.filter.FilterMenu;
import net.storagenexus.network.ConnectorMenu;
import net.storagenexus.network.LevelEmitterMenu;
import net.storagenexus.network.NetworkBusMenu;
import net.storagenexus.terminal.TerminalMenu;
import net.storagenexus.upgrade.UpgradeMenu;

public final class ModMenus {
    public static final ExtendedMenuType<UpgradeMenu, BlockPos> UPGRADES = register("upgrades",
            new ExtendedMenuType<>(UpgradeMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<ConnectorMenu, BlockPos> CONNECTOR = register("connector",
            new ExtendedMenuType<>(ConnectorMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<NetworkBusMenu, BlockPos> NETWORK_BUS = register("network_bus",
            new ExtendedMenuType<>(NetworkBusMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<LevelEmitterMenu, BlockPos> LEVEL_EMITTER = register("level_emitter",
            new ExtendedMenuType<>(LevelEmitterMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<BarrelMenu, Integer> BARREL = register("barrel",
            new ExtendedMenuType<>(BarrelMenu::new, ByteBufCodecs.VAR_INT));
    public static final ExtendedMenuType<TerminalMenu, Boolean> TERMINAL = register("terminal",
            new ExtendedMenuType<>(TerminalMenu::new, ByteBufCodecs.BOOL));
    public static final ExtendedMenuType<DimensionalMenu, DimensionalMenu.OpenData> DIMENSIONAL = register("dimensional",
            new ExtendedMenuType<>(DimensionalMenu::new, DimensionalMenu.OpenData.STREAM_CODEC));
    public static final ExtendedMenuType<ConduitConfigMenu, ConduitConfigMenu.OpenData> CONDUIT_CONFIG = register("conduit_config",
            new ExtendedMenuType<>(ConduitConfigMenu::new, ConduitConfigMenu.OpenData.STREAM_CODEC));
    public static final ExtendedMenuType<FilterMenu, Integer> FILTER = register("filter",
            new ExtendedMenuType<>(FilterMenu::new, ByteBufCodecs.VAR_INT));

    private static <T extends AbstractContainerMenu, M extends MenuType<T>> M register(String name, M type) {
        return Registry.register(BuiltInRegistries.MENU, StorageNexus.id(name), type);
    }

    public static void init() {}

    private ModMenus() {}
}
