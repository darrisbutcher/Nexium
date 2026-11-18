package net.nexium.dog;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The dog: a tamed-wolf-style companion that follows its owner, sits (lying down), teleports to the owner when far away and
 * fights for them, and that nothing in the world can kill. It is a {@link Wolf} underneath, so it keeps all of a wolf's
 * behaviour, wolf armor, collar and sounds; skeletons and llamas keep their distance from it as from a wolf.
 * Fire, lava, drowning, falling, explosions and every mob's attacks do nothing to it. The owner's own blows hurt it as they would
 * any wolf, but never to death; only the kill command and the void remove it. Creepers flee from it and cannot blow up near it
 * (see the Creeper mixin).
 */
public class DogEntity extends Wolf {
    public DogEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    /** Zoe's collar is green, not a wolf's red. */
    @Override
    public void tame(Player player) {
        super.tame(player);
        ((net.nexium.essentials.mixin.WolfAccess) this).nexium$setCollarColor(net.minecraft.world.item.DyeColor.GREEN);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.isInvulnerableTo(level, source);
        // Anything but a player's own blow does nothing at all
        return !(source.getEntity() instanceof Player) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // A player's blow can hurt but never kill; the kill command and the void still can
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount >= getHealth()) amount = Math.max(0.0F, getHealth() - 1.0F);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** There is only the one kind of dog, and it comes from the egg. */
    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    /** Wolf Armor and the Nexium Dog Armor both fit the dog's body slot. */
    @Override
    public boolean isEquippableInSlot(ItemStack stack, EquipmentSlot slot) {
        if (slot == EquipmentSlot.BODY) return stack.is(Items.WOLF_ARMOR) || stack.is(net.nexium.registry.ModItems.NEXIUM_DOG_ARMOR);
        return super.isEquippableInSlot(stack, slot);
    }
}
