package net.nexium.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.nexium.drawer.CompactingDrawerBlockEntity;
import net.nexium.drawer.DrawerBlockEntity;
import net.nexium.fluid.FluidDrawerBlockEntity;
import net.nexium.registry.ModComponents;
import net.nexium.util.Fmt;

import java.util.List;

/** Shows what a broken drawer, fluid drawer or tank is carrying, and what a shulker box holds. */
public final class ContentsTooltip {
    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, lines));

        // A shulker box with something in it shows a grid of its contents, and not the item list vanilla puts under its name
        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerPreview preview ? new ClientShulkerPreview(preview) : null);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (ShulkerPreview.of(stack) != null) lines.removeIf(ContentsTooltip::isVanillaItemList);
        });
    }

    private static void append(ItemStack stack, List<Component> lines) {
        List<DrawerBlockEntity.SlotData> slots = stack.get(ModComponents.DRAWER_SLOTS);
        if (slots != null) {
            for (DrawerBlockEntity.SlotData s : slots) {
                if (!s.variant().isBlank()) {
                    lines.add(line(Fmt.compact(s.amount()), s.variant().toStack().getHoverName()));
                } else if (!s.locked().isBlank()) {
                    lines.add(Component.translatable("tooltip.nexium.contents_locked", s.locked().toStack().getHoverName())
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
                    lines.add(Component.translatable("tooltip.nexium.contents_locked", FluidVariantAttributes.getName(s.locked()))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        Long charge = stack.get(ModComponents.BATTERY_CHARGE);
        if (charge != null) {
            lines.add(Component.translatable("tooltip.nexium.battery.charge", String.format("%,d", charge)).withStyle(ChatFormatting.GRAY));
        }

        ItemContainerContents upgrades = stack.get(ModComponents.UPGRADES);
        if (upgrades != null) {
            upgrades.nonEmptyItemCopyStream().forEach(up ->
                lines.add(Component.literal("+ ").append(up.getHoverName()).withStyle(ChatFormatting.DARK_AQUA)));
        }
    }

    /** The lines vanilla lists a container's items with: "Iron Ingot x64" for the first five, then "and 12 more". */
    private static boolean isVanillaItemList(Component line) {
        return line.getContents() instanceof TranslatableContents t
                && (t.getKey().equals("item.container.item_count") || t.getKey().equals("item.container.more_items"));
    }

    private static Component line(String amount, Component name) {
        return Component.translatable("tooltip.nexium.contents", amount, name).withStyle(ChatFormatting.GRAY);
    }

    private ContentsTooltip() {}
}
