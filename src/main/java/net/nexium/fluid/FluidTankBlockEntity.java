package net.nexium.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nexium.registry.ModBlockEntities;
import net.nexium.registry.ModComponents;
import net.nexium.util.ComparatorReadable;
import net.nexium.util.SyncedBlockEntity;

/**
 * A Fluid Tank: one fluid, up to 16, 64 or 256 buckets by tier, open to conduits and machines on every side. It keeps its fluid when
 * it is broken (in the item), and the item can scoop and pour like a bucket ({@link FluidTankItem}).
 */
public class FluidTankBlockEntity extends SyncedBlockEntity implements ComparatorReadable {
    private static final int[] TIER_BUCKETS = {16, 64, 256};

    /** What a tank item carries: the fluid and how much of it (in droplets). */
    public record Contents(FluidVariant variant, long amount) {
        public static final Contents EMPTY = new Contents(FluidVariant.blank(), 0);
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(i -> i.group(
                // A fluid that no longer exists loads as empty, not as an error
                FluidVariant.CODEC.orElse(FluidVariant.blank()).fieldOf("variant").forGetter(Contents::variant),
                Codec.LONG.fieldOf("amount").forGetter(Contents::amount)
        ).apply(i, Contents::new));

        public boolean isEmpty() { return variant.isBlank() || amount <= 0; }
    }

    private final int tier;
    private boolean dirty;
    private final SingleVariantStorage<FluidVariant> tank = new SingleVariantStorage<>() {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return capacity(); }
        @Override
        protected void onFinalCommit() {
            dirty = true;
            setChanged();
        }
    };

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK, pos, state);
        this.tier = state.getBlock() instanceof FluidTankBlock block ? block.tier() : 1;
    }

    public static long capacity(int tier) { return TIER_BUCKETS[tier - 1] * FluidConstants.BUCKET; }

    public int tier() { return tier; }
    public long capacity() { return capacity(tier); }
    public SingleVariantStorage<FluidVariant> tank() { return tank; }
    public FluidVariant fluid() { return tank.variant; }
    public long amount() { return tank.amount; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity be) {
        if (be.dirty && level.getGameTime() % 4 == 0) {
            be.dirty = false;
            be.sync();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Override
    public int comparatorSignal() {
        return ComparatorReadable.signal((double) tank.amount / capacity());
    }

    public Contents contents() {
        return tank.amount <= 0 || tank.variant.isBlank() ? Contents.EMPTY : new Contents(tank.variant, tank.amount);
    }

    public void setContents(Contents contents) {
        tank.variant = contents.isEmpty() ? FluidVariant.blank() : contents.variant();
        tank.amount = contents.isEmpty() ? 0 : Math.min(capacity(), contents.amount());
        dirty = true;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("contents", Contents.CODEC, contents());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Contents c = input.read("contents", Contents.CODEC).orElse(Contents.EMPTY);
        tank.variant = c.isEmpty() ? FluidVariant.blank() : c.variant();
        tank.amount = c.isEmpty() ? 0 : Math.min(capacity(), c.amount());
    }

    // ---- Keep contents when broken -----------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents().isEmpty()) components.set(ModComponents.TANK_FLUID, contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Contents c = components.get(ModComponents.TANK_FLUID);
        if (c != null) setContents(c);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("contents");
    }
}
