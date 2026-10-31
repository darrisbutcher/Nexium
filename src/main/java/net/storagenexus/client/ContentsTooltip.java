package net.storagenexus.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.storagenexus.drawer.CompactingDrawerBlockEntity;
import net.storagenexus.drawer.DrawerBlockEntity;
import net.storagenexus.fluid.FluidDrawerBlockEntity;
import net.storagenexus.registry.ModComponents;
import net.storagenexus.util.Fmt;

import java.util.List;

/** Shows what a broken drawer, fluid drawer or tank is carrying. */
public final class ContentsTooltip {
    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, lines));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        List<DrawerBlockEntity.SlotData> slots = stack.get(ModComponents.DRAWER_SLOTS);
        if (slots != null) {
            for (DrawerBlockEntity.SlotData s : slots) {
                if (!s.variant().isBlank()) {
                    lines.add(line(Fmt.compact(s.amount()), s.variant().toStack().getHoverName()));
                } else if (!s.locked().isBlank()) {
                    lines.add(Component.translatable("tooltip.storagenexus.contents_locked", s.locked().toStack().getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        CompactingDrawerBlockEntity.Contents compacting = stack.get(ModComponents.COMPACTING_CONTENTS);
        if (compacting != null) {
            for (int i = 0; i < compacting.tierCount() && i < compacting.tiers().size(); i++) {
                long ratio = Math.max(1, compacting.ratios().get(i));
                lines.add(line(Fmt.compact(compacting.units() / ratio), compacting.tiers().get(i).toStack().getHoverName()));
            }
        }

        List<FluidDrawerBlockEntity.SlotData> fluids = stack.get(ModComponents.FLUID_SLOTS);
        if (fluids != null) {
            for (FluidDrawerBlockEntity.SlotData s : fluids) {
                if (!s.variant().isBlank()) {
                    lines.add(line(Fmt.fluid(s.amount()), FluidVariantAttributes.getName(s.variant())));
                } else if (!s.locked().isBlank()) {
                    lines.add(Component.translatable("tooltip.storagenexus.contents_locked", FluidVariantAttributes.getName(s.locked()))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        ItemContainerContents upgrades = stack.get(ModComponents.UPGRADES);
        if (upgrades != null) {
            for (ItemStack up : upgrades.nonEmptyItems()) {
                lines.add(Component.literal("+ ").append(up.getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }

    private static Component line(String amount, Component name) {
        return Component.translatable("tooltip.storagenexus.contents", amount, name).withStyle(ChatFormatting.GRAY);
    }

    private ContentsTooltip() {}
}
