package net.nexium.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.nexium.autocraft.AutocraftPayloads;
import net.nexium.util.Fmt;

import java.util.ArrayList;
import java.util.List;

/** The Autocrafting Monitor: every request on the network with its progress and status, and a cancel button for the unfinished ones. */
public class AutocraftMonitorScreen extends Screen {
    private static final int W = 280, ROW = 24, MAX = 6;
    private static final int PANEL = 0xE0101016, EDGE = 0xFF50505F, TEXT = 0xFFE0E0E0, DIM = 0xFF8C8C98, GOLD = 0xFFFFBE3C,
            RED = 0xFFFF5555, GREEN = 0xFF55D86B, ORANGE = 0xFFFFA23C, BAR = 0xFF2A2A34;

    private final BlockPos anchor;
    private List<AutocraftPayloads.JobEntry> jobs = List.of();
    private final List<Button> cancels = new ArrayList<>();
    private int left, top, since = 100;

    public AutocraftMonitorScreen(BlockPos anchor) {
        super(Component.translatable("screen.nexium.autocraft.monitor"));
        this.anchor = anchor;
    }

    public void update(AutocraftPayloads.Jobs data) {
        jobs = data.jobs();
        rebuild();
    }

    public List<AutocraftPayloads.JobEntry> jobs() {
        return jobs;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - (30 + ROW * MAX + 8)) / 2;
        rebuild();
    }

    private void rebuild() {
        for (Button b : cancels) removeWidget(b);
        cancels.clear();
        for (int i = 0; i < Math.min(MAX, jobs.size()); i++) {
            var job = jobs.get(i);
            if (job.finished()) continue;
            Button b = Button.builder(Component.literal("X").withStyle(net.minecraft.ChatFormatting.RED),
                    x -> ClientPlayNetworking.send(new AutocraftPayloads.Cancel(anchor, job.id())))
                    .bounds(left + W - 24, top + 28 + i * ROW + 2, 16, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.nexium.autocraft.cancel"))).build();
            cancels.add(addRenderableWidget(b));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++since >= 10) {
            since = 0;
            ClientPlayNetworking.send(new AutocraftPayloads.RequestJobs(anchor));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int h = 30 + ROW * MAX + 8;
        g.fill(left, top, left + W, top + h, EDGE);
        g.fill(left + 1, top + 1, left + W - 1, top + h - 1, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, left + 8, top + 8, GOLD, false);
        if (jobs.isEmpty()) g.text(font, Component.translatable("screen.nexium.autocraft.no_jobs"), left + 8, top + 34, DIM, false);
        for (int i = 0; i < Math.min(MAX, jobs.size()); i++) {
            var job = jobs.get(i);
            int y = top + 28 + i * ROW;
            g.item(job.target(), left + 8, y + 3);
            g.text(font, job.target().getHoverName(), left + 30, y + 2, TEXT, false);
            boolean bad = job.status().startsWith("failed") || job.status().equals("cancelled") || job.status().equals("no_network");
            int colour = job.status().equals("done") ? GREEN : bad ? RED : job.status().equals("crafting") ? GREEN : ORANGE;
            g.text(font, Component.translatable("autocraft.status." + job.status()), left + 30, y + 12, colour, false);
            int barX = left + 150, barW = W - 150 - 30;
            g.fill(barX, y + 4, barX + barW, y + 12, BAR);
            long total = Math.max(1, job.total());
            g.fill(barX, y + 4, barX + (int) (barW * Math.min(job.done(), total) / total), y + 12, job.finished() && !bad ? GREEN : GOLD);
            g.text(font, Fmt.compact(job.done()) + "/" + Fmt.compact(job.total()), barX, y + 14, DIM, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
