package net.storagenexus.essentials.magnet;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.slots.ExtraSlots;

public final class MagnetLogic {
    /** Items are pulled every 4 ticks (0.2 s): too quick to notice, and a quarter of the work of scanning every tick. */
    public static final int ITEM_INTERVAL = 4;
    /** XP orbs every 2 ticks: vanilla lets a player take one orb every 2 ticks, so XP still arrives as fast as it can. */
    public static final int XP_INTERVAL = 2;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // Offset by the entity id, so players' scans fall on different ticks instead of all at once
                int phase = tick + player.getId();
                boolean items = Math.floorMod(phase, ITEM_INTERVAL) == 0;
                boolean xp = Math.floorMod(phase, XP_INTERVAL) == 0;
                if (items || xp) pull(player, items, xp);
            }
        });
    }

    /** The magnet hotkey: switch the magnet in the trinket slot on or off. */
    public static void toggle(ServerPlayer player) {
        ItemStack magnet = ExtraSlots.of(player).trinket();
        if (!(magnet.getItem() instanceof MagnetItem)) {
            player.sendOverlayMessage(Component.translatable("message.nexusessentials.magnet_missing"));
            return;
        }
        boolean active = !MagnetItem.isActive(magnet);
        magnet.set(ModComponents.MAGNET_ACTIVE, active);
        player.sendOverlayMessage(Component.translatable(active ? "message.nexusessentials.magnet_on" : "message.nexusessentials.magnet_off",
                MagnetItem.range(magnet)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS,
                0.4F, active ? 1.2F : 0.8F);
    }

    /**
     * Picks up every item and experience orb within range of a player wearing an active magnet, through walls.
     * Uses vanilla's own pickup, so items fly into the player as usual and pickup rules (delays, owners, full inventory) still apply.
     * Items the player dropped themselves come back too, once vanilla's pickup delay (2 seconds for a dropped item) is over:
     * to throw something away, switch the magnet off first.
     */
    public static void pull(ServerPlayer player) {
        pull(player, true, true);
    }

    private static void pull(ServerPlayer player, boolean items, boolean xp) {
        if (player.isSpectator() || !player.isAlive()) return;
        ItemStack magnet = ExtraSlots.of(player).trinket();
        if (!(magnet.getItem() instanceof MagnetItem) || !MagnetItem.isActive(magnet)) return;

        int range = MagnetItem.range(magnet);
        double rangeSqr = (double) range * range;
        ServerLevel level = player.level();
        AABB area = player.getBoundingBox().inflate(range);

        if (items) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay())) {
                if (item.distanceToSqr(player) > rangeSqr) continue;
                item.playerTouch(player);
            }
        }

        if (xp && NexusConfig.get().magnetPullsXp) {
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
                if (orb.distanceToSqr(player) > rangeSqr) continue;
                orb.playerTouch(player); // vanilla takes at most one orb every 2 ticks
            }
        }
    }

    private MagnetLogic() {}
}
