package net.storagenexus.essentials.capture;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.essentials.registry.ModComponents;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.registry.ModTags;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Sneak + right-click a mob with an empty main hand to pick it up as an item that keeps everything about it
 * (health, name, equipment, villager trades, owner, horse stats...). The Ender Dragon and the Wither can't be
 * picked up (entity type tag {@code nexusessentials:cannot_be_picked_up}).
 */
public final class MobCapture {
    private static final List<MemoryModuleType<net.minecraft.core.GlobalPos>> VILLAGER_POIS = List.of(
            MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT);

    public static void init() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> interact(player, hand, entity));
        // A captured pet must not vanish if it is dropped and left on the ground
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && item.getItem().is(ModItems.CAPTURED_MOB)) item.setUnlimitedLifetime();
        });
    }

    /** The checks both sides can make. */
    public static boolean canTry(Player player, InteractionHand hand, Entity entity) {
        return NexusConfig.get().mobPickUp
                && hand == InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && !player.isSpectator()
                && player.getMainHandItem().isEmpty()
                && entity instanceof Mob mob
                && mob.isAlive()
                && !mob.getType().builtInRegistryHolder().is(ModTags.CANNOT_BE_PICKED_UP)
                && ownerAllows(player, mob);
    }

    private static boolean ownerAllows(Player player, Mob mob) {
        if (!NexusConfig.get().onlyOwnersPickUpPets || !(mob instanceof OwnableEntity pet)) return true;
        EntityReference<LivingEntity> owner = pet.getOwnerReference();
        return owner == null || owner.getUUID().equals(player.getUUID());
    }

    private static InteractionResult interact(Player player, InteractionHand hand, Entity entity) {
        if (!canTry(player, hand, entity)) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Mob mob = (Mob) entity;
        if (!player.mayBuild() || level.getServer().isUnderSpawnProtection(level, mob.blockPosition(), player)) {
            player.sendOverlayMessage(Component.translatable("message.nexusessentials.capture_not_here"));
            return InteractionResult.FAIL;
        }

        ItemStack stack = capture(mob);
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY(0.5), mob.getZ(), 12, mob.getBbWidth() / 2, mob.getBbHeight() / 3, mob.getBbWidth() / 2, 0.02);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    /** Removes the mob from the world and returns it as an item. */
    public static ItemStack capture(Mob mob) {
        ServerLevel level = (ServerLevel) mob.level();
        // Riders, the mob's own ride and its lead stay behind; the lead drops as an item
        mob.ejectPassengers();
        mob.stopRiding();
        if (mob.isLeashed()) mob.dropLeash();
        // A villager gives up its bed, workstation and meeting point so other villagers can use them; it finds new ones when let out
        if (mob instanceof Villager villager) {
            for (var memory : VILLAGER_POIS) {
                villager.releasePoi(memory);
                villager.getBrain().eraseMemory(memory);
            }
        }

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mob.save(output);
        CompoundTag data = output.buildResult();
        CapturedMob captured = new CapturedMob(mob.getType(), data, java.util.Optional.ofNullable(mob.getCustomName()),
                mob.getHealth(), mob.getMaxHealth(), mob.isBaby());
        mob.remove(Entity.RemovalReason.DISCARDED);
        return toItem(captured);
    }

    /** The item for a captured mob: looks like the mob's spawn egg (with a glint) when it has one. */
    public static ItemStack toItem(CapturedMob captured) {
        ItemStack stack = new ItemStack(ModItems.CAPTURED_MOB);
        stack.set(ModComponents.CAPTURED_MOB, captured);
        SpawnEggItem.byId(captured.type()).ifPresent(egg -> {
            Identifier model = egg.value().components().get(DataComponents.ITEM_MODEL);
            if (model != null) stack.set(DataComponents.ITEM_MODEL, model);
        });
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /** Puts the mob back into the world at {@code pos}. False if it can't be let out here (a monster on Peaceful, for instance). */
    public static boolean release(ServerLevel level, CapturedMob captured, Vec3 pos, float yaw, @Nullable Player player) {
        if (!captured.type().canSpawn(level)) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.nexusessentials.release_peaceful"));
            return false;
        }
        Entity entity = EntityType.loadEntityRecursive(captured.data().copy(), level, new EntitySpawnRequest(EntitySpawnReason.SPAWN_ITEM_USE, false), e -> {
            e.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
            return e;
        });
        if (entity == null) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.nexusessentials.release_failed"));
            return false;
        }
        // Creative players don't use up the item, so the same mob can be let out twice
        if (level.getEntity(entity.getUUID()) != null) entity.setUUID(Mth.createInsecureUUID(level.getRandom()));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
        if (entity instanceof Mob mob) {
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setPersistenceRequired(); // placed on purpose, so it never despawns
        }
        if (!level.addFreshEntity(entity)) return false;
        level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY(0.5), entity.getZ(), 12, entity.getBbWidth() / 2, entity.getBbHeight() / 3, entity.getBbWidth() / 2, 0.02);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private MobCapture() {}
}
