package net.storagenexus.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.storagenexus.StorageNexus;
import net.storagenexus.aiot.AiotEnchanting;
import net.storagenexus.aiot.AiotItem;
import net.storagenexus.aiot.MiningMode;
import net.storagenexus.aiot.module.AiotModules;
import net.storagenexus.aiot.module.ModuleType;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * A small readout in the bottom right corner while you hold an AIOT: Silk Touch or Fortune (when it holds both), and the state of
 * each module, such as "3x3: tunnel 8", "Vein: up to 64 (hold V)" or "Nexus Link: off". Green is on, grey is off. Turn it off
 * with the client's {@code aiotHud} setting.
 */
public final class AiotHud {
    private static final int ON = 0xFF9FE8A8;
    private static final int OFF = 0xFF8A8A8A;
    private static final int MODE = 0xFFE8E0B0;

    /** One line of the readout. */
    public record Line(Component text, int color) {}

    /** The lines shown for this tool, empty if it is not an AIOT (also used by the client test). */
    public static List<Line> lines(Minecraft client, ItemStack tool) {
        List<Line> out = new ArrayList<>();
        if (!(tool.getItem() instanceof AiotItem)) return out;
        MiningMode mode = tool.get(ModComponents.MINING_MODE);
        if (mode != null && AiotEnchanting.canSwap(tool) && client.level != null) {
            var enchantment = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.enchantment());
            out.add(new Line(Enchantment.getFullname(enchantment, AiotEnchanting.activeLevel(tool, mode)), MODE));
        }
        if (!AiotModules.find(tool, ModuleType.AREA_MINING).isEmpty()) {
            if (!AiotModules.active(tool, ModuleType.AREA_MINING)) {
                out.add(new Line(Component.translatable("hud.storagenexus.aiot.area_off"), OFF));
            } else {
                int length = AiotModules.tunnelLength(tool);
                out.add(new Line(length > 1 ? Component.translatable("hud.storagenexus.aiot.tunnel", length)
                        : Component.translatable("hud.storagenexus.aiot.area_on"), ON));
            }
        }
        if (NexusConfig.get().aiotVeinMining) {
            out.add(new Line(Component.translatable("hud.storagenexus.aiot.vein", AiotModules.veinSize(tool),
                    ToolMenuClient.VEIN_MINE.getTranslatedKeyMessage()), ON));
        }
        if (!AiotModules.find(tool, ModuleType.NEXUS_LINK).isEmpty()) {
            out.add(new Line(Component.translatable(AiotModules.activeLink(tool) != null
                    ? "hud.storagenexus.aiot.link_on" : "hud.storagenexus.aiot.link_off"),
                    AiotModules.activeLink(tool) != null ? ON : OFF));
        }
        return out;
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, StorageNexus.id("aiot_hud"), AiotHud::draw);
    }

    private static void draw(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !NexusConfig.get().aiotHud) return;
        List<Line> lines = lines(client, client.player.getMainHandItem());
        if (lines.isEmpty()) return;
        var font = client.font;
        int width = 0;
        for (Line line : lines) width = Math.max(width, font.width(line.text()));
        int lineHeight = font.lineHeight + 2;
        int boxHeight = lines.size() * lineHeight + 4;
        int x = g.guiWidth() - width - 10;
        int y = g.guiHeight() - boxHeight - 30;
        g.fill(x - 4, y - 2, x + width + 4, y + boxHeight - 2, 0x66000000);
        for (Line line : lines) {
            g.text(font, line.text(), x, y, line.color(), true);
            y += lineHeight;
        }
    }

    private AiotHud() {}
}
