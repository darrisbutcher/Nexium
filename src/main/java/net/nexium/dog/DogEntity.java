package net.nexium.dog;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.nexium.Nexium;

/**
 * The dog: a tamed-wolf-style companion that follows its owner, sits (lying down), teleports to the owner when far away and
 * fights for them, and that nothing in the world can kill. It is a {@link Wolf} underneath, so it keeps all of a wolf's
 * behaviour, wolf armor, collar and sounds; skeletons and llamas keep their distance from it as from a wolf.
 * Fire, lava, drowning, falling, explosions and every mob's attacks do nothing to it. The owner's own blows hurt it as they would
 * any wolf, but never to death; only the kill command and the void remove it. Creepers flee from it and cannot blow up near it
 * (see the Creeper mixin).
 */
public class DogEntity extends Wolf {
    /** Zoe bites 1.5 times as hard as a wolf (6 damage instead of 4), whatever else changes her attack. */
    public static final double DAMAGE_MULTIPLIER = 1.5;
    private static final Identifier DAMAGE_BOOST = Nexium.id("zoe_damage");

    public DogEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        ensureDamageBoost();
    }

    /**
     * Puts her damage bonus on her attack. It is a permanent modifier, so it is saved with her and in her egg, but a dog saved
     * before it existed, or one whose attributes were reloaded without it, gets it back here the next time she ticks.
     */
    public void ensureDamageBoost() {
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && !damage.hasModifier(DAMAGE_BOOST)) {
            damage.addPermanentModifier(new AttributeModifier(DAMAGE_BOOST, DAMAGE_MULTIPLIER - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) ensureDamageBoost();
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
