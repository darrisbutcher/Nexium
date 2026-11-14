package net.storagenexus.essentials.xpfarm;

import net.minecraft.world.entity.player.Player;

/** Vanilla's experience arithmetic: how many points a level costs and how many it takes to reach one. */
public final class ExperienceMath {
    private ExperienceMath() {}

    /** Points from the start of this level to the start of the next: 2l+7 below 15, 5l-38 up to 29, then 9l-158. */
    public static int toNextLevel(int level) {
        if (level >= 30) return 9 * level - 158;
        if (level >= 15) return 5 * level - 38;
        return 2 * level + 7;
    }

    /** Points from level 0 to the start of this level (level 30 is 1,395; level 200 is 149,720). */
    public static long totalForLevel(int level) {
        long sum = 0;
        for (int l = 0; l < level; l++) sum += toNextLevel(l);
        return sum;
    }

    /** The level a total of points reaches. */
    public static int levelOf(long points) {
        int level = 0;
        long left = Math.max(0, points);
        while (left >= toNextLevel(level)) {
            left -= toNextLevel(level);
            level++;
        }
        return level;
    }

    /** All the points a player has: the whole levels and the way into the current one. */
    public static long totalOf(Player player) {
        return totalForLevel(player.experienceLevel) + Math.round(player.experienceProgress * toNextLevel(player.experienceLevel));
    }

    /** Points a player needs to reach the start of the level this many levels up. */
    public static long toGainLevels(Player player, int levels) {
        return Math.max(0, totalForLevel(player.experienceLevel + levels) - totalOf(player));
    }
}
