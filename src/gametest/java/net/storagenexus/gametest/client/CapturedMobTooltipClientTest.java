package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.storagenexus.essentials.capture.CapturedMob;
import net.storagenexus.essentials.capture.MobCapture;

import java.util.List;
import java.util.Optional;

/** A Captured Mob's tooltip in a real game: what it looks like always, and its age, owner, gear and status while Shift is held. */
public class CapturedMobTooltipClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                // A newborn cow that is in love and has Speed II
                CompoundTag cow = new CompoundTag();
                cow.putString("variant", "minecraft:temperate");
                cow.putInt("Age", -24_000);
                cow.putInt("InLove", 600);
                ListTag effects = new ListTag();
                CompoundTag speed = new CompoundTag();
                speed.putString("id", "minecraft:speed");
                speed.putByte("amplifier", (byte) 1);
                speed.putInt("duration", 600);
                effects.add(speed);
                cow.put("active_effects", effects);
                player.getInventory().setItem(0, MobCapture.toItem(new CapturedMob(EntityTypes.COW, cow, Optional.empty(), 5.0F, 10.0F, true)));
                // A tamed, sitting wolf owned by this player
                CompoundTag wolf = new CompoundTag();
                wolf.putString("variant", "minecraft:pale");
                wolf.putIntArray("Owner", UUIDUtil.uuidToIntArray(player.getUUID()));
                wolf.putByte("CollarColor", (byte) 14);
                wolf.putBoolean("Sitting", true);
                player.getInventory().setItem(1, MobCapture.toItem(new CapturedMob(EntityTypes.WOLF, wolf, Optional.empty(), 40.0F, 40.0F, false)));
                // A creeper has nothing to add
                player.getInventory().setItem(2, MobCapture.toItem(new CapturedMob(EntityTypes.CREEPER, new CompoundTag(), Optional.empty(), 20.0F, 20.0F, false)));
            });
            ctx.waitTicks(5);
            ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            ctx.waitTicks(5);

            // Without Shift: the variant, and a hint about the rest
            List<String> plain = tooltip(ctx, 0);
            check(plain.contains("Variant: Temperate"), "the variant is always there: " + plain);
            check(plain.contains("Hold Shift for details"), "with a hint about the rest: " + plain);
            check(plain.stream().noneMatch(l -> l.startsWith("Grows up")), "but not the details: " + plain);
            hover(ctx, 0);
            System.out.println("[nexus] mob tooltip: " + ctx.takeScreenshot("nexus_mob_tooltip"));

            // With Shift: the details, and no hint
            ctx.getInput().holdShift();
            ctx.waitTicks(3);
            List<String> cow = tooltip(ctx, 0);
            check(cow.contains("Grows up in 20:00"), "a baby's growing time: " + cow);
            check(cow.contains("In love"), "in love: " + cow);
            check(cow.contains("Speed II (0:30)"), "its effect: " + cow);
            check(!cow.contains("Hold Shift for details"), "no hint any more: " + cow);
            System.out.println("[nexus] mob tooltip shift: " + ctx.takeScreenshot("nexus_mob_tooltip_shift"));

            hover(ctx, 1);
            List<String> wolf = tooltip(ctx, 1);
            String me = ctx.computeOnClient(mc -> mc.player.getName().getString());
            check(wolf.contains("Variant: Pale"), "the wolf's variant: " + wolf);
            check(wolf.contains("Tamed by " + me), "its owner by name: " + wolf);
            check(wolf.contains("Collar: Red") && wolf.contains("Sitting"), "collar and sitting: " + wolf);
            System.out.println("[nexus] mob tooltip wolf: " + ctx.takeScreenshot("nexus_mob_tooltip_wolf"));
            ctx.getInput().releaseShift();

            // Nothing to add: no hint either
            List<String> creeper = tooltip(ctx, 2);
            check(creeper.stream().noneMatch(l -> l.contains("Hold Shift")), "a creeper has nothing hidden: " + creeper);
        }
    }

    /** The tooltip of the stack in a hotbar slot, as text. */
    private static List<String> tooltip(ClientGameTestContext ctx, int slot) {
        return ctx.computeOnClient(mc -> {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            return stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
        });
    }

    /** Puts the mouse over a hotbar slot of the inventory screen. */
    private static void hover(ClientGameTestContext ctx, int slot) {
        double[] at = ctx.computeOnClient(mc -> {
            var screen = (AbstractContainerScreen<?>) mc.gui.screen();
            double scale = mc.getWindow().getGuiScale();
            return new double[]{(screen.leftPos + 8 + 18 * slot + 8) * scale, (screen.topPos + 142 + 8) * scale};
        });
        ctx.getInput().setCursorPos(at[0], at[1]);
        ctx.waitTicks(4);
        ctx.runOnClient(mc -> mc.gui.toastManager().clear());
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("Expected: " + what);
    }
}
