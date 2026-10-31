package net.storagenexus.util;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Small helper so every menu is opened the same way with its opening data. */
public final class Menus {
    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(int id, Inventory inventory, Player player);
    }

    public static <D> void open(ServerPlayer player, Component title, D data, Factory factory) {
        player.openMenu(new ExtendedScreenHandlerFactory<D>() {
            @Override
            public D getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return factory.create(id, inventory, p);
            }
        });
    }

    private Menus() {}
}
