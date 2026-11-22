package net.nexium.jetpack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.nexium.energy.CellHolder;
import net.nexium.energy.CellSource;
import net.nexium.energy.ItemEnergy;
import net.nexium.util.Menus;
import net.nexium.energy.CellMenu;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The Jetpack: worn in the wings slot (or the chest slot), it lets you fly on a battery cell kept in its own screen. Right-click wears it;
 * sneak and right-click opens the screen for the cell. See {@link JetpackFlight} for how it flies.
 */
public class JetpackItem extends Item implements CellHolder {
    public JetpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public String useKey() {
        return "screen.nexium.cell.jetpack";
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
            open(sp, CellSource.of(hand), player.getItemInHand(hand));
            return InteractionResult.SUCCESS;
        }
        // Worn by a right-click (the wings slot's own handler does it first); here only in the chest slot
        return super.use(level, player, hand);
    }

    public static void open(ServerPlayer player, int source, ItemStack jetpack) {
        Menus.open(player, jetpack.getHoverName(), source, (id, inv, p) -> new CellMenu(id, inv, source));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        long capacity = ItemEnergy.capacity(stack);
        lines.accept(capacity <= 0 ? Component.translatable("tooltip.nexium.jetpack.no_cell")
                : Component.translatable("tooltip.nexium.cell", f.format(ItemEnergy.stored(stack)), f.format(capacity)));
    }
}
