package net.storagenexus.energy;

/**
 * A plain store of Nexus Energy with a size. What may be put in and taken out from outside is set when it is made:
 * a machine's buffer only takes energy in, a generator's only gives it out, a Battery's does both.
 */
public class EnergyBuffer implements NexusEnergy {
    private final long capacity;
    private final boolean accepts, gives;
    private final Runnable onChange;
    private long stored;

    public EnergyBuffer(long capacity, boolean accepts, boolean gives, Runnable onChange) {
        this.capacity = capacity;
        this.accepts = accepts;
        this.gives = gives;
        this.onChange = onChange;
    }

    /** A machine's buffer: energy comes in from outside and the machine uses it. */
    public static EnergyBuffer machine(long capacity, Runnable onChange) {
        return new EnergyBuffer(capacity, true, false, onChange);
    }

    /** A generator's buffer: the generator fills it and energy goes out. */
    public static EnergyBuffer generator(long capacity, Runnable onChange) {
        return new EnergyBuffer(capacity, false, true, onChange);
    }

    @Override
    public long insert(long max, boolean simulate) {
        return accepts ? add(max, simulate) : 0;
    }

    @Override
    public long extract(long max, boolean simulate) {
        return gives ? remove(max, simulate) : 0;
    }

    /** Adds energy from the block itself (a generator making it), whatever the outside may do. */
    public long add(long max, boolean simulate) {
        long n = Math.max(0, Math.min(max, capacity - stored));
        if (n > 0 && !simulate) {
            stored += n;
            onChange.run();
        }
        return n;
    }

    /** Takes energy for the block itself (a machine using it), whatever the outside may do. */
    public long remove(long max, boolean simulate) {
        long n = Math.max(0, Math.min(max, stored));
        if (n > 0 && !simulate) {
            stored -= n;
            onChange.run();
        }
        return n;
    }

    /** Uses exactly {@code amount}, or nothing if there isn't that much. */
    public boolean use(long amount) {
        if (stored < amount) return false;
        if (amount > 0) {
            stored -= amount;
            onChange.run();
        }
        return true;
    }

    @Override
    public long stored() { return stored; }

    @Override
    public long capacity() { return capacity; }

    /** For loading and tests: sets what is stored, within the size. */
    public void set(long amount) {
        stored = Math.max(0, Math.min(capacity, amount));
    }
}
