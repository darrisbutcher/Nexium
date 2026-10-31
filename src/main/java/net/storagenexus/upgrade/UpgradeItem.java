package net.storagenexus.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class UpgradeItem extends Item {
    public enum Kind {
        CAPACITY, VOID, LOCK, TRANSFER_SPEED, INTAKE_RANGE, CONDUIT_SPEED;

        /** Upgrades that go in drawer / tank upgrade slots. */
        public boolean isStorageUpgrade() {
            return this == CAPACITY || this == VOID || this == LOCK;
        }
    }

    private final Kind kind;

    public UpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
