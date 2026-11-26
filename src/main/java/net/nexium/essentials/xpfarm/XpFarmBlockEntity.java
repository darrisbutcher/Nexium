package net.nexium.essentials.xpfarm;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.nexium.util.XpMending;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.nexium.energy.EnergyBuffer;
import net.nexium.essentials.farm.FarmBlockEntity;
import net.nexium.essentials.farm.Overclock;
import net.nexium.essentials.registry.ModComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.nexium.essentials.registry.ModBlockEntities;
import net.nexium.util.ComparatorReadable;

/**
 * The Experience Farm: turns Nexus Energy into experience and keeps it until a player collects it. Every tick it has
 * power it uses {@value #ENERGY_PER_TICK} NE and banks {@value #XP_PER_TICK} points (40 a second), until the bank holds
 * {@value #CAP_LEVELS} levels' worth. A redstone signal pauses it. Break it and the bank comes out as experience orbs.
 */
public class XpFarmBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos>, ComparatorReadable {
    /** Nexus Energy used each tick it works. */
    public static final int ENERGY_PER_TICK = 10;
    /** Experience points banked each tick it works: 40 a second. */
    public static final int XP_PER_TICK = 2;
    /** The bank stops filling at this many levels' worth of points. */
    public static final int CAP_LEVELS = 200;
    /** The most it holds, in points (149,720: what it takes to get from level 0 to level 200). */
    public static final long CAPACITY = ExperienceMath.totalForLevel(CAP_LEVELS);

    /** The Nexus Energy it runs on: ten seconds' worth. */
    private final EnergyBuffer energy = EnergyBuffer.machine(ENERGY_PER_TICK * FarmBlockEntity.ENERGY_TICKS, this::setChanged);
    private long experience;
    /** What it did last tick (not saved; worked out again every tick). */
    private boolean full, unpowered, working;

    public XpFarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XP_FARM, pos, state);
    }

    public EnergyBuffer energy() { return energy; }

    /** The overclock level: 0 off, 1 is 2x, 2 is 4x, 3 is 8x. */
    private int overclock;

    public int overclock() { return overclock; }

    /** Sets the overclock level; the energy buffer is resized to hold ten seconds of work at the new speed. */
    public void setOverclock(int level) {
        overclock = Overclock.clamp(level);
        energy.setCapacity(Overclock.capacity(ENERGY_PER_TICK, overclock));
        setChanged();
    }

    /** Experience points banked and waiting to be collected. */
    public long experience() { return experience; }

    public boolean isFull() { return full; }
    public boolean isUnpowered() { return unpowered; }
    public boolean isWorking() { return working; }

    public boolean isPaused() {
        return getBlockState().hasProperty(XpFarmBlock.POWERED) && getBlockState().getValue(XpFarmBlock.POWERED);
    }

    /** Switched off from its screen: no work and no power use (kept in the dropped item). */
    private boolean off;

    public boolean isOff() { return off; }

    public void setOff(boolean off) {
        this.off = off;
        setChanged();
    }

    // ---------------------------------------------------------------- working

    public static void serverTick(Level level, BlockPos pos, BlockState state, XpFarmBlockEntity farm) {
        if (farm.off) return;
        farm.work();
    }

    private void work() {
        int before = comparatorSignal();
        boolean paused = isPaused();
        full = experience >= CAPACITY;
        int run = Overclock.ticksOfWork(overclock, energy.stored(), ENERGY_PER_TICK);
        unpowered = !full && !paused && run < 1;
        working = false;
        if (!full && !paused && !unpowered) {
            energy.use((long) ENERGY_PER_TICK * run);
            experience = Math.min(CAPACITY, experience + (long) XP_PER_TICK * run);
            full = experience >= CAPACITY;
            working = true;
            setChanged();
        }
        if (level != null && comparatorSignal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** What a comparator reads: how full the bank is, 15 only when it is full. */
    @Override
    public int comparatorSignal() {
        return ComparatorReadable.signal((double) experience / CAPACITY);
    }

    // ---------------------------------------------------------------- collecting

    /** Banks experience (the farm itself adds {@value #XP_PER_TICK} a tick; this is for tests and commands). Never past the cap. */
    public void addExperience(long points) {
        experience = Math.max(0, Math.min(CAPACITY, experience + points));
        setChanged();
    }

    /** Hands the player every point banked, as an orb would: Mending items are repaired first. Returns how many. */
    public long takeAll(Player player) {
        return give(player, experience);
    }

    /**
     * Hands the player just enough to rise this many levels (to the start of that level), or everything banked if that
     * is not enough. Returns how many points.
     */
    public long takeLevels(Player player, int levels) {
        return give(player, Math.min(experience, ExperienceMath.toGainLevels(player, levels)));
    }

    private long give(Player player, long points) {
        if (points <= 0) return 0;
        experience -= points;
        XpMending.give(player, (int) points);
        setChanged();
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        return points;
    }

    /** Whatever is banked comes out as experience orbs when the farm is broken. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && experience > 0) {
            ExperienceOrb.award(server, Vec3.atCenterOf(pos), (int) experience);
            experience = 0;
        }
    }

    // ---------------------------------------------------------------- menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new XpFarmMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("off", off);
        output.putLong("experience", experience);
        output.putInt("overclock", overclock);
        output.putLong("energy", energy.stored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        off = input.getBooleanOr("off", false);
        experience = Math.max(0, Math.min(CAPACITY, input.getLongOr("experience", 0L)));
        overclock = Overclock.clamp(input.getIntOr("overclock", 0));
        energy.setCapacity(Overclock.capacity(ENERGY_PER_TICK, overclock));
        energy.set(input.getLongOr("energy", 0L));
    }

    /** The overclock setting stays in the dropped item. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (off) components.set(net.nexium.essentials.registry.ModComponents.MACHINE_OFF, net.minecraft.util.Unit.INSTANCE);
        if (overclock > 0) components.set(ModComponents.OVERCLOCK, overclock);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        if (components.get(net.nexium.essentials.registry.ModComponents.MACHINE_OFF) != null) off = true;
        Integer saved = components.get(ModComponents.OVERCLOCK);
        if (saved != null) setOverclock(saved);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("off");
        output.discard("overclock");
    }
}
