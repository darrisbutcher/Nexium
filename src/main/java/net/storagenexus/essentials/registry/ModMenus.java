package net.storagenexus.essentials.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.storagenexus.essentials.NexusEssentials;
import net.storagenexus.essentials.assembler.FarmAssemblerMenu;
import net.storagenexus.essentials.farm.FarmMenu;
import net.storagenexus.essentials.ironfarm.IronFarmMenu;

public final class ModMenus {
    public static final ExtendedMenuType<IronFarmMenu, BlockPos> IRON_FARM = Registry.register(BuiltInRegistries.MENU,
            NexusEssentials.id("iron_farm"), new ExtendedMenuType<>(IronFarmMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<net.storagenexus.essentials.xpfarm.XpFarmMenu, BlockPos> XP_FARM = Registry.register(BuiltInRegistries.MENU,
            NexusEssentials.id("xp_farm"), new ExtendedMenuType<>(net.storagenexus.essentials.xpfarm.XpFarmMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<FarmMenu, BlockPos> FARM = Registry.register(BuiltInRegistries.MENU,
            NexusEssentials.id("farm"), new ExtendedMenuType<>(FarmMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<FarmAssemblerMenu, BlockPos> FARM_ASSEMBLER = Registry.register(BuiltInRegistries.MENU,
            NexusEssentials.id("farm_assembler"), new ExtendedMenuType<>(FarmAssemblerMenu::new, BlockPos.STREAM_CODEC));

    /** Opening data: {@code Backpacks.EQUIPPED}, or the inventory slot holding the Backpack. */
    public static final ExtendedMenuType<net.storagenexus.essentials.backpack.BackpackMenu, Integer> BACKPACK = Registry.register(BuiltInRegistries.MENU,
            NexusEssentials.id("backpack"), new ExtendedMenuType<>(net.storagenexus.essentials.backpack.BackpackMenu::new, net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static void init() {}

    private ModMenus() {}
}
