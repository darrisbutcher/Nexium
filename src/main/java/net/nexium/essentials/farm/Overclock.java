package net.nexium.essentials.farm;

/**
 * The overclock setting of a farm: off, 2x, 4x or 8x. A farm on a setting does that many ticks of work each tick and
 * uses that many times the energy, so a cycle costs the same energy at any speed. Short of energy it still works,
 * as fast as what it holds can pay for. Its energy buffer grows with the setting to keep holding ten seconds' worth.
 */
public final class Overclock {
    /** Off, 2x, 4x and 8x. */
    public static final int LEVELS = 4;
    public static final int MAX_LEVEL = LEVELS - 1;
    public static final int MAX_MULTIPLIER = 1 << MAX_LEVEL;

    /** Keeps a stored or received level in range. */
    public static int clamp(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    /** Ticks of work done each tick on this level: 1, 2, 4 or 8. */
    public static int multiplier(int level) {
        return 1 << clamp(level);
    }

    public static int next(int level) {
        return (clamp(level) + 1) % LEVELS;
    }

    public static int previous(int level) {
        return (clamp(level) + LEVELS - 1) % LEVELS;
    }

    /** How many ticks of work this tick can be paid for: up to the level's multiplier, as far as the energy goes. */
    public static int ticksOfWork(int level, long stored, long energyPerTick) {
        if (energyPerTick <= 0) return multiplier(level);
        return (int) Math.min(multiplier(level), stored / energyPerTick);
    }

    /** The size of a machine's energy buffer: ten seconds of work at this level. */
    public static long capacity(long energyPerTick, int level) {
        return energyPerTick * FarmBlockEntity.ENERGY_TICKS * multiplier(level);
    }

    private Overclock() {}
}
