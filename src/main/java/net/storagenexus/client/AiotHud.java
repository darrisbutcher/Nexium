package net.storagenexus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
import net.storagenexus.builder.BuilderItem;
import net.storagenexus.builder.BuilderSettings;
import net.storagenexus.essentials.config.NexusConfig;
import net.storagenexus.registry.ModComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * A small readout in the bottom right corner, by the hotbar, for the AIOT in your hand: Silk Touch or Fortune (when it holds
 * both), and the state of each module, such as "3x3: tunnel 8", "Vein: up to 64 (hold V)" or "Nexus Link: off". Green is on,
 * grey is off. A Construction Builder gets its mode, plane, size and Nexus Link. It shows for {@code aiotHudSeconds} (20) seconds after you switch to an AIOT, press the 3x3, tunnel or vein key,
 * or something on it changes (from the tool menu, say), stays up while the vein key is held, then fades out. 0 seconds keeps
 * it up while you hold one; the client's {@code aiotHud} setting turns it off.
 */
public final class AiotHud {
    private static final int ON = 0xFF9FE8A8;
    private static final int OFF = 0xFF8A8A8A;
    private static final int MODE = 0xFFE8E0B0;
    /** The last second fades out. */
    private static final int FADE_TICKS = 20;

    /** Client ticks so far, and the tick the readout hides at. */
    private static long now, shownUntil;
    private static int lastSlot = -1;
    private static ItemStack lastTool = ItemStack.EMPTY;
    private static List<String> lastLines = List.of();

    /** One line of the readout. */
    public record Line(Component text, int color) {}

    /** The lines shown for this tool, empty if it is not an AIOT (also used by the client test). */
    public static List<Line> lines(Minecraft client, ItemStack tool) {
        List<Line> out = new ArrayList<>();
        if (tool.getItem() instanceof BuilderItem builder) {
            // The Construction Builder: mode and plane, batch size, and its Nexus Link
            out.add(new Line(Component.translatable("hud.storagenexus.builder.mode",
                    Component.translatable("screen.storagenexus.builder.mode." + BuilderSettings.mode(tool).name().toLowerCase()),
                    Component.translatable("screen.storagenexus.builder.plane." + BuilderSettings.plane(tool).name().toLowerCase())), MODE));
            out.add(new Line(Component.translatable("hud.storagenexus.builder.size", BuilderSettings.size(tool), builder.tier().maxBlocks()), ON));
            if (!BuilderSettings.module(tool).isEmpty()) {
                boolean on = BuilderSettings.activeLink(tool) != null;
                out.add(new Line(Component.translatable(on ? "hud.storagenexus.aiot.link_on" : "hud.storagenexus.aiot.link_off"), on ? ON : OFF));
            }
            return out;
        }
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
        ClientTickEvents.END_CLIENT_TICK.register(AiotHud::tick);
    }

    /** Shows the readout for the set time from now (the 3x3, tunnel and vein keys call this). */
    public static void show() {
        shownUntil = now + Math.max(1, NexusConfig.get().aiotHudSeconds) * 20L;
    }

    /** Whether the readout is up (it also needs an AIOT in hand to draw anything). */
    public static boolean visible() {
        return NexusConfig.get().aiotHudSeconds <= 0 || now < shownUntil;
    }

    private static void tick(Minecraft client) {
        now++;
        if (client.player == null) {
            lastSlot = -1;
            lastTool = ItemStack.EMPTY;
            lastLines = List.of();
            return;
        }
        ItemStack tool = client.player.getMainHandItem();
        int slot = client.player.getInventory().getSelectedSlot();
        boolean aiot = tool.getItem() instanceof AiotItem || tool.getItem() instanceof BuilderItem;
        List<String> text = new ArrayList<>();
        if (aiot) for (Line line : lines(client, tool)) text.add(line.text().getString());
        // Switched to an AIOT (another slot, or another item in the same slot), or what it says changed
        if (aiot && (slot != lastSlot || !ItemStack.isSameItem(tool, lastTool) || !text.equals(lastLines))) show();
        if (aiot && ToolMenuClient.VEIN_MINE.isDown()) show();
        lastSlot = slot;
        lastTool = tool.copy();
        lastLines = text;
    }

    private static void draw(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !NexusConfig.get().aiotHud || !visible()) return;
        List<Line> lines = lines(client, client.player.getMainHandItem());
        if (lines.isEmpty()) return;
        // 1 while shown, fading to 0 over the last second
        float alpha = NexusConfig.get().aiotHudSeconds <= 0 ? 1.0F
                : Math.min(1.0F, (shownUntil - now - delta.getGameTimeDeltaPartialTick(true)) / FADE_TICKS);
        if (alpha <= 0.05F) return;
        var font = client.font;
        int width = 0;
        for (Line line : lines) width = Math.max(width, font.width(line.text()));
        int lineHeight = font.lineHeight + 2;
        int boxHeight = lines.size() * lineHeight + 4;
        int x = g.guiWidth() - width - 10;
        int y = g.guiHeight() - boxHeight - 30;
        g.fill(x - 4, y - 2, x + width + 4, y + boxHeight - 2, Math.round(0x66 * alpha) << 24);
        int a = Math.max(0x10, Math.round(0xFF * alpha)) << 24;
        for (Line line : lines) {
            g.text(font, line.text(), x, y, (line.color() & 0xFFFFFF) | a, true);
            y += lineHeight;
        }
    }

    private AiotHud() {}
}
