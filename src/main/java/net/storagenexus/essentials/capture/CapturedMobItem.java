package net.storagenexus.essentials.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.storagenexus.essentials.registry.ModComponents;

import java.util.Locale;
import java.util.function.Consumer;

/** A mob picked up with sneak + right-click. Right-click a block to let it out again, exactly as it was. */
public class CapturedMobItem extends Item {
    public CapturedMobItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CapturedMob mob = stack.get(ModComponents.CAPTURED_MOB);
        if (mob == null) return super.getName(stack);
        Component typeName = mob.type().getDescription();
        return mob.customName()
                .map(name -> (Component) Component.translatable("item.nexusessentials.captured_mob.named", name, typeName))
                .orElseGet(() -> Component.translatable("item.nexusessentials.captured_mob.of", typeName));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CapturedMob mob = stack.get(ModComponents.CAPTURED_MOB);
        if (mob == null) return InteractionResult.FAIL;
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos at = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
        Player player = context.getPlayer();
        float yaw = player == null ? 0.0F : player.getYRot() + 180.0F; // face the player
        if (!MobCapture.release(serverLevel, mob, Vec3.atBottomCenterOf(at), yaw, player)) return InteractionResult.FAIL;

        stack.consume(1, player);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CapturedMob mob = stack.get(ModComponents.CAPTURED_MOB);
        if (mob == null) return;
        if (mob.customName().isPresent()) tooltip.accept(mob.type().getDescription().copy().withStyle(ChatFormatting.GRAY));
        if (mob.baby()) tooltip.accept(Component.translatable("tooltip.nexusessentials.captured_mob_baby").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.captured_mob_health",
                String.format(Locale.ROOT, "%.1f", mob.health()), String.format(Locale.ROOT, "%.0f", mob.maxHealth())).withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.nexusessentials.captured_mob_release").withStyle(ChatFormatting.DARK_GRAY));
    }
}
