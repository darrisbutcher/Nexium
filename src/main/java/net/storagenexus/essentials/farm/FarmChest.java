package net.storagenexus.essentials.farm;

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
import net.storagenexus.essentials.registry.ModItems;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a double chest named like one of the farms ({@link FarmKind#chestName()}) into that farm as soon as it holds
 * exactly the recipe ({@link FarmRecipe}). Looked at by {@link net.storagenexus.essentials.ironfarm.IronFarmChest},
 * which watches every chest.
 */
public final class FarmChest {
    /** The farm a chest is named for, or null. */
    public static @Nullable FarmKind kindOf(ChestBlockEntity chest) {
        Component name = chest.getCustomName();
        if (name == null) return null;
        String text = name.getString().trim();
        for (FarmKind kind : FarmKind.values()) if (text.equalsIgnoreCase(kind.chestName().trim())) return kind;
        return null;
    }

    public static boolean isNamed(ChestBlockEntity chest) {
        return kindOf(chest) != null;
    }

    /** Builds the farm if the chest is ready, otherwise tells {@code player} what's wrong. */
    public static void process(ServerLevel level, FarmKind kind, List<ChestBlockEntity> halves, @Nullable ServerPlayer player) {
        FarmRecipe recipe = FarmRecipe.of(kind);
        List<ItemStack> slots = new ArrayList<>(FarmRecipe.CHEST_SLOTS);
        for (ChestBlockEntity half : halves) {
            for (int i = 0; i < half.getContainerSize(); i++) slots.add(half.getItem(i));
        }
        FarmRecipe.Check check = recipe.check(slots);
        if (recipe.complete(check)) {
            build(level, kind, halves);
        } else if (player != null) {
            player.sendSystemMessage(Component.translatable("message.nexusessentials.farm_not_ready",
                    Component.translatable(ModItems.FARMS.get(kind).getDescriptionId())).withStyle(ChatFormatting.GOLD));
            for (Component line : recipe.problems(check)) player.sendSystemMessage(line);
        }
    }

    /** The chest and everything in it disappear, and the farm pops out where it stood. */
    private static void build(ServerLevel level, FarmKind kind, List<ChestBlockEntity> halves) {
        Vec3 center = Vec3.ZERO;
        for (ChestBlockEntity half : halves) center = center.add(Vec3.atCenterOf(half.getBlockPos()));
        center = center.scale(1.0 / halves.size());
        for (ChestBlockEntity half : halves) half.clearContent();
        for (ChestBlockEntity half : halves) level.removeBlock(half.getBlockPos(), false);

        ItemEntity farm = new ItemEntity(level, center.x, center.y, center.z, new ItemStack(ModItems.FARMS.get(kind)));
        farm.setDefaultPickUpDelay();
        level.addFreshEntity(farm);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 30, 0.6, 0.4, 0.6, 0.02);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private FarmChest() {}
}
