package net.storagenexus.essentials.librarian;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.ironfarm.IronFarmRecipe;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A chest (single or double) named "Nexus Librarian" (the {@code librarianChestName} setting) that holds exactly the
 * set number of villagers (12 by default; spawn eggs or Captured Mobs) and nothing else is used up and replaced by a
 * Master Librarian ({@link Librarian}). Looked at by {@link net.storagenexus.essentials.ironfarm.IronFarmChest}, which
 * watches every chest.
 */
public final class LibrarianChest {
    private static final int MAX_STRAYS_LISTED = 5;

    public static boolean isNamed(ChestBlockEntity chest) {
        Component name = chest.getCustomName();
        return name != null && name.getString().trim().equalsIgnoreCase(NexusConfig.get().librarianChestName.trim());
    }

    /** How many villagers the chest takes. */
    public static int needed() {
        return NexusConfig.get().librarianVillagers;
    }

    /** Builds the librarian if the chest is ready, otherwise tells {@code player} what is missing. */
    public static void process(ServerLevel level, List<ChestBlockEntity> halves, @Nullable ServerPlayer player) {
        int villagers = 0;
        Map<String, ItemStack> strays = new LinkedHashMap<>();
        for (ChestBlockEntity half : halves) {
            for (int i = 0; i < half.getContainerSize(); i++) {
                ItemStack stack = half.getItem(i);
                if (stack.isEmpty()) continue;
                if (IronFarmRecipe.partOf(stack) == IronFarmRecipe.Part.VILLAGERS) villagers += stack.getCount();
                else strays.putIfAbsent(stack.getItem() + "|" + stack.getHoverName().getString(), stack);
            }
        }
        if (villagers == needed() && strays.isEmpty()) {
            build(level, halves);
        } else if (player != null) {
            player.sendSystemMessage(Component.translatable("message.storagenexus.librarian_not_ready", needed()).withStyle(ChatFormatting.GOLD));
            if (villagers < needed()) {
                player.sendSystemMessage(Component.translatable("message.storagenexus.librarian_missing", needed() - villagers).withStyle(ChatFormatting.YELLOW));
            } else if (villagers > needed()) {
                player.sendSystemMessage(Component.translatable("message.storagenexus.librarian_too_many", villagers - needed()).withStyle(ChatFormatting.YELLOW));
            }
            if (!strays.isEmpty()) {
                List<ItemStack> list = new ArrayList<>(strays.values());
                Component names = Component.empty();
                for (int i = 0; i < Math.min(MAX_STRAYS_LISTED, list.size()); i++) {
                    if (i > 0) names = Component.empty().append(names).append(", ");
                    names = Component.empty().append(names).append(list.get(i).getHoverName());
                }
                player.sendSystemMessage(Component.translatable("message.storagenexus.librarian_remove", names).withStyle(ChatFormatting.RED));
            }
        }
    }

    /** The chest and the villagers in it disappear, and the librarian pops out where it stood. */
    private static void build(ServerLevel level, List<ChestBlockEntity> halves) {
        Vec3 center = Vec3.ZERO;
        for (ChestBlockEntity half : halves) center = center.add(Vec3.atCenterOf(half.getBlockPos()));
        center = center.scale(1.0 / halves.size());
        ItemStack librarian = Librarian.create(level);
        if (librarian.isEmpty()) return;
        for (ChestBlockEntity half : halves) half.clearContent();
        for (ChestBlockEntity half : halves) level.removeBlock(half.getBlockPos(), false);
        ItemEntity item = new ItemEntity(level, center.x, center.y, center.z, librarian);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y, center.z, 30, 0.6, 0.4, 0.6, 0.02);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private LibrarianChest() {}
}
