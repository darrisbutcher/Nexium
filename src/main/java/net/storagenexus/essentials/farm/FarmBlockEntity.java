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
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
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
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.registry.ModBlockEntities;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.function.Consumer;
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
    /** Which of the farm's {@link FarmKind#modes()} it is on (the Generator). */
    private int mode;
    /** Cycles left on the bone meal in use (the Crop Farm runs twice as fast on it, the Moss Farm needs it). */
    private int boostLeft;
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
        boolean boneMeal = kind.input() == FarmKind.Input.BONE_MEAL || kind.input() == FarmKind.Input.BONE_MEAL_BOOST;
        // A bone meal is used up when the one before it has run out of cycles
        if (boneMeal && boostLeft <= 0 && hasInput(kind) && !full && !isPaused()) {
            items.get(INPUT_SLOT).shrink(1);
            boostLeft = FarmKind.CYCLES_PER_BONE_MEAL;
        }
        starved = kind.inputRequired() && (boneMeal ? boostLeft <= 0 : !hasInput(kind));
        if (!full && !starved && !isPaused()) {
            progress += kind == FarmKind.CROP && boostLeft > 0 ? 2 : 1;
            if (progress >= cycleTicks()) {
                progress = 0;
                cycle(level, kind, level.getRandom());
                if (boostLeft > 0) boostLeft--;
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
            case WITCH -> roll(level, EntityTypes.WITCH, null);
            case SUGAR_CANE -> insert(new ItemStack(Items.SUGAR_CANE, 3));
            case BAMBOO -> insert(new ItemStack(Items.BAMBOO, 4));
            case NETHER_WART -> insert(new ItemStack(Items.NETHER_WART, 4));
            case MONSTER -> {
                // The monsters' loot, and the raiders'
                roll(level, EntityTypes.ZOMBIE, null);
                roll(level, EntityTypes.SKELETON, null);
                roll(level, EntityTypes.SPIDER, null);
                roll(level, EntityTypes.CREEPER, null);
                roll(level, EntityTypes.PILLAGER, null);
                roll(level, EntityTypes.VINDICATOR, null);
                roll(level, EntityTypes.EVOKER, null);
            }
            case NETHER_MOB -> {
                roll(level, EntityTypes.BLAZE, null);
                roll(level, EntityTypes.GHAST, null);
                roll(level, EntityTypes.MAGMA_CUBE, e -> ((net.minecraft.world.entity.monster.cubemob.AbstractCubeMob) e).setSize(2, true));
            }
            case OCEAN -> {
                roll(level, EntityTypes.GUARDIAN, null);
                roll(level, EntityTypes.DROWNED, null);
                roll(level, EntityTypes.COD, null);
            }
            case SLIME -> roll(level, EntityTypes.SLIME, e -> ((net.minecraft.world.entity.monster.cubemob.AbstractCubeMob) e).setSize(1, true));
            case LIVESTOCK -> {
                ItemStack feed = items.get(INPUT_SLOT);
                FarmKind.Feed kindOfFeed = FarmKind.feed(feed);
                if (kindOfFeed == null) return;
                feed.shrink(1);
                switch (kindOfFeed) {
                    case WHEAT -> {
                        roll(level, EntityTypes.COW, null);
                        roll(level, EntityTypes.SHEEP, e -> ((net.minecraft.world.entity.animal.sheep.Sheep) e).setColor(sheepColour(random)));
                    }
                    case SEEDS -> {
                        roll(level, EntityTypes.CHICKEN, null);
                        insert(new ItemStack(Items.EGG));
                    }
                    case CARROT -> {
                        roll(level, EntityTypes.PIG, null);
                        roll(level, EntityTypes.RABBIT, null);
                    }
                    case ROOT -> roll(level, EntityTypes.PIG, null);
                }
            }
            case CROP -> {
                insert(new ItemStack(Items.WHEAT, 3));
                insert(new ItemStack(Items.WHEAT_SEEDS, 1));
                insert(new ItemStack(Items.CARROT, 3));
                insert(new ItemStack(Items.POTATO, 3));
                insert(new ItemStack(Items.BEETROOT, 3));
                insert(new ItemStack(Items.BEETROOT_SEEDS, 1));
            }
            case GOURD -> {
                insert(new ItemStack(Items.MELON_SLICE, 12));
                insert(new ItemStack(Items.PUMPKIN, 3));
                insert(new ItemStack(Items.CACTUS, 4));
                insert(new ItemStack(Items.KELP, 6));
            }
            case GENERATOR -> {
                switch (kind.modes().get(mode())) {
                    case "cobblestone" -> insert(new ItemStack(Items.COBBLESTONE, 8));
                    case "stone" -> insert(new ItemStack(Items.STONE, 8));
                    case "gravel" -> insert(new ItemStack(Items.GRAVEL, 8));
                    case "sand" -> insert(new ItemStack(Items.SAND, 8));
                    case "clay" -> insert(new ItemStack(Items.CLAY, 4));
                    case "basalt" -> insert(new ItemStack(Items.BASALT, 2));
                    default -> insert(new ItemStack(Items.OBSIDIAN, 1)); // twice as long a cycle
                }
            }
            case BEE -> {
                insert(new ItemStack(Items.HONEYCOMB, 1 + random.nextInt(3)));
                insert(new ItemStack(Items.HONEY_BOTTLE, 2));
            }
            case FROGLIGHT -> {
                insert(new ItemStack(Items.OCHRE_FROGLIGHT, 1));
                insert(new ItemStack(Items.VERDANT_FROGLIGHT, 1));
                insert(new ItemStack(Items.PEARLESCENT_FROGLIGHT, 1));
            }
            case MOSS -> {
                insert(new ItemStack(Items.MOSS_BLOCK, 3));
                insert(new ItemStack(Items.MOSS_CARPET, 2));
                insert(new ItemStack(Items.PALE_MOSS_BLOCK, 3));
                insert(new ItemStack(Items.PALE_MOSS_CARPET, 2));
                insert(new ItemStack(Items.PALE_HANGING_MOSS, 2));
                insert(new ItemStack(Items.RESIN_CLUMP, 6));
                if (random.nextFloat() < 0.1F) insert(new ItemStack(random.nextBoolean() ? Items.AZALEA : Items.FLOWERING_AZALEA));
            }
        }
    }

    /**
     * Rolls the loot table of one mob as if a player killed it (no Looting, and the drops that need a player kill included),
     * into the output. {@code setup} gives the mob its size or colour first.
     */
    private void roll(ServerLevel level, EntityType<?> type, java.util.function.@org.jspecify.annotations.Nullable Consumer<Entity> setup) {
        Entity mob = type.create(level, EntitySpawnReason.SPAWNER);
        if (!(mob instanceof LivingEntity)) return;
        if (setup != null) setup.accept(mob);
        mob.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        var table = mob.getLootTable();
        if (table.isEmpty()) return;
        var killer = FakePlayer.get(level);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        for (ItemStack loot : level.getServer().reloadableRegistries().getLootTable(table.get()).getRandomItems(params)) insert(loot);
    }

    /** A sheep colour at the natural spawn odds. */
    private static DyeColor sheepColour(RandomSource random) {
        int n = random.nextInt(10000);
        if (n < 500) return DyeColor.BLACK;
        if (n < 1000) return DyeColor.GRAY;
        if (n < 1500) return DyeColor.LIGHT_GRAY;
        if (n < 1800) return DyeColor.BROWN;
        if (n < 1816) return DyeColor.PINK;
        return DyeColor.WHITE;
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
        return Math.min(1.0F, (float) progress / cycleTicks());
    }

    /** Ticks per cycle on the current mode. */
    public int cycleTicks() { return kind().cycleTicks(mode()); }

    /** The mode, as an index into {@link FarmKind#modes()} (0 for farms without modes). */
    public int mode() {
        int n = kind().modes().size();
        return n == 0 ? 0 : Math.floorMod(mode, n);
    }

    /** Steps to the next mode. */
    public void cycleMode() {
        int n = kind().modes().size();
        if (n == 0) return;
        mode = (mode() + 1) % n;
        progress = Math.min(progress, Math.max(0, cycleTicks() - 1));
        setChanged();
    }

    public void setMode(int value) {
        mode = value;
        setChanged();
    }

    /** Cycles left on the bone meal in use. */
    public int boostLeft() { return boostLeft; }

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
        output.putInt("mode", mode);
        output.putInt("boost", boostLeft);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = Math.max(0, input.getIntOr("progress", 0));
        mode = Math.max(0, input.getIntOr("mode", 0));
        boostLeft = Math.max(0, input.getIntOr("boost", 0));
        // 1.19 Bee Farms kept honey in a tank: hand it back as bottles (a quarter bucket each)
        long legacy = input.getLongOr("honey", 0L) / (net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 4);
        while (legacy > 0) {
            int n = (int) Math.min(legacy, 16);
            insert(new ItemStack(Items.HONEY_BOTTLE, n));
            legacy -= n;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (progress > 0) components.set(ModComponents.FARM_PROGRESS, progress);
        if (mode() > 0) components.set(ModComponents.FARM_MODE, mode());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer saved = components.get(ModComponents.FARM_PROGRESS);
        if (saved != null) progress = Math.max(0, saved);
        Integer savedMode = components.get(ModComponents.FARM_MODE);
        if (savedMode != null) mode = Math.max(0, savedMode);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("progress");
        output.discard("mode");
    }
}
