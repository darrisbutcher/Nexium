package net.storagenexus.essentials.farm;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.registry.ModBlockEntities;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.stream.IntStream;

/**
 * The block entity of every {@link FarmKind} farm (the kind comes from its block): 27 output slots hoppers can only take
 * from below, and an input slot for the kinds that have one (hoppers feed it from the top and sides). Every cycle it makes
 * what its kind makes. A full output, a missing input or a redstone signal pauses the cycle. The progress is kept in the
 * dropped item.
 */
public class FarmBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    public static final int OUTPUT_SLOTS = 27;
    public static final int INPUT_SLOT = OUTPUT_SLOTS;
    public static final int SIZE = OUTPUT_SLOTS + 1;

    private static final int[] OUTPUT = IntStream.range(0, OUTPUT_SLOTS).toArray();
    private static final int[] INPUT = {INPUT_SLOT};
    private static final int[] NO_SLOTS = new int[0];

    /** Skull, ingot and shell odds are vanilla's plus Looting V: 2.5% + 5 x 1%, and 50% + 5 x 6.25%. */
    private static final float SKULL_CHANCE = 0.075F, INGOT_CHANCE = 0.075F, SHELL_CHANCE = 0.8125F;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    /** Not saved; worked out again every tick. */
    private boolean full, starved;

    public FarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARM, pos, state);
    }

    public FarmKind kind() {
        return getBlockState().getBlock() instanceof FarmBlock block ? block.kind() : FarmKind.ZOMBIFIED_PIGLIN;
    }

    // ---------------------------------------------------------------- working

    public static void serverTick(Level level, BlockPos pos, BlockState state, FarmBlockEntity farm) {
        if (level instanceof ServerLevel serverLevel) farm.work(serverLevel);
    }

    private void work(ServerLevel level) {
        FarmKind kind = kind();
        full = !hasRoom(kind);
        starved = kind.hasInput() && !hasInput(kind);
        if (!full && !starved && !isPaused()) {
            progress++;
            if (progress >= kind.cycleTicks()) {
                progress = 0;
                cycle(level, kind, level.getRandom());
            }
            setChanged();
        }
    }

    /** Runs one cycle now, whatever the progress (tests use it). Takes the input and makes the output. */
    public void cycle(ServerLevel level, FarmKind kind, RandomSource random) {
        switch (kind) {
            case VILLAGER_BREEDER -> {
                items.get(INPUT_SLOT).shrink(FarmKind.foodCost(items.get(INPUT_SLOT)));
                insert(baby(level));
            }
            case PIGLIN_BARTER -> {
                items.get(INPUT_SLOT).shrink(1);
                LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
                for (ItemStack loot : level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING).getRandomItems(params)) {
                    insert(loot);
                }
            }
            case ZOMBIFIED_PIGLIN -> {
                // Gold nuggets 0-1 plus Looting V's 0-5, and the rare ingot
                int nuggets = random.nextInt(2) + Math.round(5 * random.nextFloat());
                if (nuggets > 0) insert(new ItemStack(Items.GOLD_NUGGET, nuggets));
                if (random.nextFloat() < INGOT_CHANCE) insert(new ItemStack(Items.GOLD_INGOT));
            }
            case WITHER_SKELETON -> {
                int coal = random.nextInt(2), bones = random.nextInt(3);
                if (coal > 0) insert(new ItemStack(Items.COAL, coal));
                if (bones > 0) insert(new ItemStack(Items.BONE, bones));
                if (random.nextFloat() < SKULL_CHANCE) insert(new ItemStack(Items.WITHER_SKELETON_SKULL));
            }
            case SHULKER -> {
                if (random.nextFloat() < SHELL_CHANCE) insert(new ItemStack(Items.SHULKER_SHELL));
            }
        }
    }

    /** A baby villager of the local kind (by biome), as a Captured Mob. */
    private ItemStack baby(ServerLevel level) {
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.BREEDING);
        if (villager == null) return ItemStack.EMPTY;
        villager.setBaby(true);
        villager.setVillagerData(villager.getVillagerData().withType(level.registryAccess(), VillagerType.byBiome(level.getBiome(worldPosition))));
        return MobCapture.capture(villager);
    }

    /** Whether the output has the empty slots another cycle needs. */
    private boolean hasRoom(FarmKind kind) {
        int empty = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) if (items.get(i).isEmpty()) empty++;
        return empty >= kind.slotsNeeded();
    }

    /** Whether the input slot holds what one cycle takes. */
    private boolean hasInput(FarmKind kind) {
        ItemStack stack = items.get(INPUT_SLOT);
        return !stack.isEmpty() && kind.acceptsInput(stack) && stack.getCount() >= kind.inputPerCycle(stack);
    }

    private void insert(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int i = 0; i < OUTPUT_SLOTS && !stack.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) items.set(i, stack.split(stack.getMaxStackSize()));
        }
    }

    // ---------------------------------------------------------------- what the screen and tests read

    /** Work towards the next cycle, 0 to 1. */
    public float progressFraction() {
        return Math.min(1.0F, (float) progress / kind().cycleTicks());
    }

    public int progress() { return progress; }
    public boolean isFull() { return full; }
    public boolean isStarved() { return starved; }

    public void setProgress(int ticks) {
        progress = Math.max(0, ticks);
        setChanged();
    }

    /** A redstone signal stops the farm. */
    public boolean isPaused() {
        BlockState state = getBlockState();
        return state.hasProperty(FarmBlock.POWERED) && state.getValue(FarmBlock.POWERED);
    }

    // ---------------------------------------------------------------- container

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && kind().acceptsInput(stack);
    }

    /** Hoppers and pipes take the output from below, and feed the input slot from the top and sides. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return OUTPUT;
        return kind().hasInput() ? INPUT : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot < OUTPUT_SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FarmMenu(containerId, inventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return worldPosition;
    }

    // ---------------------------------------------------------------- saving, and keeping the progress in the item

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = Math.max(0, input.getIntOr("progress", 0));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (progress > 0) components.set(ModComponents.FARM_PROGRESS, progress);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer saved = components.get(ModComponents.FARM_PROGRESS);
        if (saved != null) progress = Math.max(0, saved);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("progress");
    }
}
