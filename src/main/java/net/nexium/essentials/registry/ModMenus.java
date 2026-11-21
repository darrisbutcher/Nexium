package net.nexium.essentials.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.nexium.essentials.NexiumEssentials;
import net.nexium.essentials.assembler.FarmAssemblerMenu;
import net.nexium.essentials.farm.FarmMenu;
import net.nexium.essentials.ironfarm.IronFarmMenu;

public final class ModMenus {
    public static final ExtendedMenuType<IronFarmMenu, BlockPos> IRON_FARM = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("iron_farm"), new ExtendedMenuType<>(IronFarmMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<net.nexium.essentials.xpfarm.XpFarmMenu, BlockPos> XP_FARM = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("xp_farm"), new ExtendedMenuType<>(net.nexium.essentials.xpfarm.XpFarmMenu::new, BlockPos.STREAM_CODEC));
    public static final ExtendedMenuType<FarmMenu, BlockPos> FARM = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("farm"), new ExtendedMenuType<>(FarmMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<FarmAssemblerMenu, BlockPos> FARM_ASSEMBLER = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("farm_assembler"), new ExtendedMenuType<>(FarmAssemblerMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<net.nexium.essentials.grindstone.EnchantedGrindstoneMenu, BlockPos> ENCHANTED_GRINDSTONE = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("enchanted_grindstone"), new ExtendedMenuType<>(net.nexium.essentials.grindstone.EnchantedGrindstoneMenu::new, BlockPos.STREAM_CODEC));

    public static final ExtendedMenuType<net.nexium.essentials.hostile.HostileMobSwitchMenu, BlockPos> HOSTILE_MOB_SWITCH = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("hostile_mob_switch"), new ExtendedMenuType<>(net.nexium.essentials.hostile.HostileMobSwitchMenu::new, BlockPos.STREAM_CODEC));

    /** Opening data: {@code Backpacks.EQUIPPED}, or the inventory slot holding the Backpack. */
    public static final ExtendedMenuType<net.nexium.essentials.backpack.BackpackMenu, Integer> BACKPACK = Registry.register(BuiltInRegistries.MENU,
            NexiumEssentials.id("backpack"), new ExtendedMenuType<>(net.nexium.essentials.backpack.BackpackMenu::new, net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    public static void init() {}

    private ModMenus() {}
}
