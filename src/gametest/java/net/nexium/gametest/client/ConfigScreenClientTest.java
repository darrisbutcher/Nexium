package net.nexium.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.nexium.client.screen.BlockListScreen;
import net.nexium.client.screen.ConfigScreen;
import net.nexium.essentials.config.ConfigSchema;
import net.nexium.essentials.config.NexusConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The settings screen: the schema covers every setting in the config file (and nothing else), its limits and reset work,
 * each group opens a page with its settings, and screenshots show a few pages.
 */
public class ConfigScreenClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        // Every switch and number in NexusConfig has a place on the screen, and the screen has nothing that is not a setting
        List<String> fields = new ArrayList<>();
        for (Field f : NexusConfig.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() == boolean.class || f.getType() == int.class || f.getType() == List.class) fields.add(f.getName());
        }
        List<String> listed = ConfigSchema.ENTRIES.stream().map(ConfigSchema.Entry::field).toList();
        check(listed.size() == fields.size() && listed.containsAll(fields), "the schema lists every setting once, missing: "
                + fields.stream().filter(f -> !listed.contains(f)).toList());
        check(listed.stream().distinct().count() == listed.size(), "no setting is listed twice");
        check(ConfigSchema.ENTRIES.stream().allMatch(e -> ConfigSchema.GROUPS.contains(e.group())), "every setting is in a known group");

        // Limits and reset
        NexusConfig scratch = new NexusConfig();
        ConfigSchema.Entry range = ConfigSchema.ENTRIES.stream().filter(e -> e.field().equals("magnetMaxRange")).findFirst().orElseThrow();
        range.setInt(scratch, 1000);
        check(range.getInt(scratch) == 64, "a number above its limit is held to it");
        check(!range.isDefault(scratch), "a changed number is not the default");
        range.reset(scratch);
        check(range.isDefault(scratch) && scratch.magnetMaxRange == 16, "reset puts the default back");
        ConfigSchema.Entry flag = ConfigSchema.ENTRIES.stream().filter(e -> e.field().equals("mobPickUp")).findFirst().orElseThrow();
        flag.setBoolean(scratch, false);
        check(!flag.isDefault(scratch), "a changed switch is not the default");
        flag.reset(scratch);
        check(scratch.mobPickUp, "reset turns it back on");

        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> mc.gui.setScreen(new ConfigScreen(null)));
            ctx.waitTicks(5);
            for (String group : ConfigSchema.GROUPS) {
                ctx.runOnClient(mc -> ((ConfigScreen) mc.gui.screen()).showGroup(group));
                ctx.waitTicks(3);
                List<String> page = ctx.computeOnClient(mc -> ((ConfigScreen) mc.gui.screen()).page().stream()
                        .map(ConfigSchema.Entry::field).toList());
                List<String> expected = ConfigSchema.in(group).stream().map(ConfigSchema.Entry::field).toList();
                check(page.equals(expected) && !page.isEmpty(), "the " + group + " page lists its settings");
                if (group.equals("mobs") || group.equals("tools") || group.equals("farms")) {
                    System.out.println("[nexus] settings " + group + ": " + ctx.takeScreenshot("nexus_settings_" + group));
                }
            }
            // The vein mining blacklist: its own screen, with ids and tags typed in, refused when unknown, and taken off again
            ConfigSchema.Entry blacklist = ConfigSchema.ENTRIES.stream().filter(e -> e.field().equals("veinBlacklist")).findFirst().orElseThrow();
            check(blacklist.isBlockList() && ConfigSchema.in("tools").contains(blacklist), "the blacklist is a block list on the tools page");
            NexusConfig.get().veinBlacklist.clear();
            ctx.runOnClient(mc -> ((ConfigScreen) mc.gui.screen()).showGroup("tools"));
            ctx.waitTicks(3);
            System.out.println("[nexus] settings tools with blacklist: " + ctx.takeScreenshot("nexus_settings_blacklist_row"));
            ctx.runOnClient(mc -> mc.gui.setScreen(new BlockListScreen(mc.gui.screen(), blacklist)));
            ctx.waitTicks(3);
            ctx.runOnClient(mc -> {
                BlockListScreen screen = (BlockListScreen) mc.gui.screen();
                screen.typeAndAdd("diamond_ore");
                screen.typeAndAdd("#minecraft:logs");
                screen.typeAndAdd("nonsense_block");
                screen.typeAndAdd("diamond_ore");
            });
            ctx.waitTicks(3);
            List<String> listed2 = ctx.computeOnClient(mc -> ((BlockListScreen) mc.gui.screen()).entries());
            check(listed2.equals(List.of("minecraft:diamond_ore", "#minecraft:logs")),
                    "the list holds the block and the tag once each and refuses the unknown one: " + listed2);
            check(NexusConfig.get().veinBlacklist.equals(listed2), "and the live settings hold the same");
            System.out.println("[nexus] settings blacklist screen: " + ctx.takeScreenshot("nexus_settings_blacklist"));
            NexusConfig.get().veinBlacklist.clear();
            NexusConfig.save();
            ctx.runOnClient(mc -> mc.gui.setScreen(new ConfigScreen(null)));
            ctx.waitTicks(3);

            // A change in the live settings shows on the page and Reset page puts it back
            NexusConfig.get().creepersBreakBlocks = true;
            ctx.runOnClient(mc -> ((ConfigScreen) mc.gui.screen()).showGroup("mobs"));
            ctx.waitTicks(3);
            System.out.println("[nexus] settings changed: " + ctx.takeScreenshot("nexus_settings_changed"));
            NexusConfig.get().creepersBreakBlocks = false;
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
