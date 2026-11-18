package net.nexium.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jspecify.annotations.Nullable;

/**
 * What a shulker box's tooltip shows: a grid of what is inside it, 9 by 3 like the box itself, drawn by
 * {@link ClientShulkerPreview}. Only a box with something in it has one, and the normal item list under its name ("Iron Ingot x64
 * ... and 12 more") is hidden when it does (see {@link ContentsTooltip}).
 */
public record ShulkerPreview(ItemContainerContents contents, @Nullable DyeColor color) implements TooltipComponent {
    public static final int COLUMNS = 9, ROWS = 3;

    /** The preview for this stack, or null if it is not a shulker box or has nothing in it. */
    public static @Nullable ShulkerPreview of(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock box)) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || contents.nonEmptyItemCopyStream().findAny().isEmpty()) return null;
        return new ShulkerPreview(contents, box.getColor());
    }
}
