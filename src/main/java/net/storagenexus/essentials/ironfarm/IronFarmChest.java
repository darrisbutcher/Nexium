package net.storagenexus.essentials.ironfarm;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.librarian.LibrarianChest;
import net.storagenexus.essentials.registry.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a double chest named "Nexus Iron Farm" (the {@code ironFarmChestName} setting) into an Iron Farm item as
 * soon as it holds exactly the recipe ({@link IronFarmRecipe}). A chest named "Nexus Librarian" is handled the same way
 * by {@link LibrarianChest}. Chests are looked at the end of the tick they
 * change in (players, hoppers, anything), and when a player closes one that isn't ready they're told what's wrong.
 */
public final class IronFarmChest {
    private record Key(ServerLevel level, BlockPos pos) {}

    private record Pending(Key key, @Nullable ServerPlayer player) {}

    /** Chests to look at this tick, and who closed them (null if nobody did). */
    private static final Map<Key, @Nullable ServerPlayer> PENDING = new LinkedHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> processPending());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    /** Called from {@code BlockEntity.setChanged} for every chest. */
    public static void changed(ChestBlockEntity chest) {
        queue(chest, null);
    }

    /** Called when someone closes a chest. */
    public static void closed(ChestBlockEntity chest, ContainerUser user) {
        if (user.getLivingEntity() instanceof ServerPlayer player) queue(chest, player);
    }

    private static void queue(ChestBlockEntity chest, @Nullable ServerPlayer player) {
        if (!(chest.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        BlockState state = chest.getBlockState();
        if (!(state.getBlock() instanceof ChestBlock)) return;
        BlockPos pos = chest.getBlockPos();
        BlockPos other = state.getValue(ChestBlock.TYPE) == ChestType.SINGLE ? null : ChestBlock.getConnectedBlockPos(pos, state);
        if (!isSpecial(chest) && !(other != null && level.isLoaded(other)
                && level.getBlockEntity(other) instanceof ChestBlockEntity half && isSpecial(half))) return;
        // Both halves of a double chest report changes and closing: count them once, under the lower position
        Key key = new Key(level, other != null && other.compareTo(pos) < 0 ? other : pos);
        if (player != null || !PENDING.containsKey(key)) PENDING.put(key, player);
    }

    /** A chest with one of the names that turn it into something. */
    private static boolean isSpecial(ChestBlockEntity chest) {
        return isNamed(chest) || LibrarianChest.isNamed(chest) || net.storagenexus.essentials.farm.FarmChest.isNamed(chest);
    }

    public static boolean isNamed(ChestBlockEntity chest) {
        Component name = chest.getCustomName();
        return name != null && name.getString().trim().equalsIgnoreCase(NexusConfig.get().ironFarmChestName.trim());
    }

    private static void processPending() {
        if (PENDING.isEmpty()) return;
        List<Pending> batch = new ArrayList<>(PENDING.size());
        PENDING.forEach((key, player) -> batch.add(new Pending(key, player)));
        PENDING.clear();
        for (Pending p : batch) process(p.key().level(), p.key().pos(), p.player());
    }

    /** Looks at the chest at {@code pos} (either half): builds the farm if it is ready, otherwise tells {@code player} what's wrong. */
    public static void process(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
        if (!level.isLoaded(pos)) return;
        List<ChestBlockEntity> halves = halves(level, pos);
        if (halves.isEmpty()) return;
        if (halves.stream().anyMatch(LibrarianChest::isNamed)) {
            LibrarianChest.process(level, halves, player);
            return;
        }
        for (ChestBlockEntity half : halves) {
            net.storagenexus.essentials.farm.FarmKind kind = net.storagenexus.essentials.farm.FarmChest.kindOf(half);
            if (kind != null) {
                net.storagenexus.essentials.farm.FarmChest.process(level, kind, halves, player);
                return;
            }
        }
        if (halves.stream().noneMatch(IronFarmChest::isNamed)) return;

        List<ItemStack> slots = new ArrayList<>(IronFarmRecipe.CHEST_SLOTS);
        for (ChestBlockEntity half : halves) {
            for (int i = 0; i < half.getContainerSize(); i++) slots.add(half.getItem(i));
        }
        IronFarmRecipe.Check check = IronFarmRecipe.check(slots);
        if (check.complete()) {
            build(level, halves);
        } else if (player != null) {
            player.sendSystemMessage(Component.translatable("message.nexusessentials.iron_farm_not_ready").withStyle(ChatFormatting.GOLD));
            for (Component line : check.problems()) player.sendSystemMessage(line);
        }
    }

    private static List<ChestBlockEntity> halves(Level level, BlockPos pos) {
        List<ChestBlockEntity> halves = new ArrayList<>(2);
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !(level.getBlockEntity(pos) instanceof ChestBlockEntity first)) return halves;
        halves.add(first);
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = ChestBlock.getConnectedBlockPos(pos, state);
            if (level.isLoaded(otherPos) && level.getBlockState(otherPos).is(state.getBlock()) && level.getBlockEntity(otherPos) instanceof ChestBlockEntity other) {
                halves.add(other);
            }
        }
        return halves;
    }

    /** The chest and everything in it disappear, and an Iron Farm pops out where it stood. */
    private static void build(ServerLevel level, List<ChestBlockEntity> halves) {
        Vec3 center = Vec3.ZERO;
        for (ChestBlockEntity half : halves) center = center.add(Vec3.atCenterOf(half.getBlockPos()));
        center = center.scale(1.0 / halves.size());
        for (ChestBlockEntity half : halves) half.clearContent();
        for (ChestBlockEntity half : halves) level.removeBlock(half.getBlockPos(), false);

        ItemEntity farm = new ItemEntity(level, center.x, center.y, center.z, new ItemStack(ModItems.IRON_FARM));
        farm.setDefaultPickUpDelay();
        level.addFreshEntity(farm);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 30, 0.6, 0.4, 0.6, 0.02);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private IronFarmChest() {}
}
