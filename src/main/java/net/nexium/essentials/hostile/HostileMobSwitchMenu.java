package net.nexium.essentials.hostile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.nexium.essentials.registry.ModBlocks;
import net.nexium.essentials.registry.ModMenus;

/**
 * The Hostile Mob Switch's menu: no slots, just the world's setting (sent to the screen as one data slot) and one button that flips
 * it. The setting is the world's, so every switch's screen shows the same state.
 */
public class HostileMobSwitchMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client side. */
    public HostileMobSwitchMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, ContainerLevelAccess.NULL, new SimpleContainerData(1));
    }

    /** Server side. */
    public HostileMobSwitchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        this(id, access, new ContainerData() {
            private final MinecraftServer server = ((ServerPlayer) inventory.player).level().getServer();

            @Override
            public int get(int index) {
                return HostileMobSwitch.get(server).isBlocking() ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        });
    }

    private HostileMobSwitchMenu(int id, ContainerLevelAccess access, ContainerData data) {
        super(ModMenus.HOSTILE_MOB_SWITCH, id);
        this.access = access;
        this.data = data;
        addDataSlots(data);
    }

    /** Whether hostile spawning is switched off (the world's setting, as of the last sync). */
    public boolean blocking() {
        return data.get(0) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE || !(player instanceof ServerPlayer sp)) return false;
        HostileMobSwitch setting = HostileMobSwitch.get(sp.level().getServer());
        setting.setBlocking(!setting.isBlocking());
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, setting.isBlocking() ? 0.6F : 0.5F));
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HOSTILE_MOB_SWITCH);
    }
}
