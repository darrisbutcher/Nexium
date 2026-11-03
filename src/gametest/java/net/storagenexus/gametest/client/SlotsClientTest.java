package net.storagenexus.gametest.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.storagenexus.essentials.capture.MobCapture;
import net.storagenexus.essentials.registry.ModItems;
import net.storagenexus.essentials.slots.ExtraSlots;

/**
 * Opens the survival and creative inventories with the trinket and wings slots filled, and shows a player wearing
 * a chestplate with an elytra in the wings slot, taking screenshots of each for a visual check.
 */
public class SlotsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            ctx.waitTicks(20);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getInventory().clearContent();
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                ExtraSlots.of(player).setItem(ExtraSlots.WINGS, new ItemStack(Items.ELYTRA));
                ExtraSlots.of(player).setItem(ExtraSlots.TRINKET, new ItemStack(ModItems.MAGNET));
                ExtraSlots.of(player).setItem(ExtraSlots.ARROWS, new ItemStack(Items.ARROW, 40));
                ExtraSlots.of(player).setItem(ExtraSlots.TOTEM, new ItemStack(Items.TOTEM_OF_UNDYING));
                Cow cow = EntityTypes.COW.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                cow.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
                player.level().addFreshEntity(cow);
                player.getInventory().setItem(0, MobCapture.capture(cow));
                player.getInventory().setItem(1, new ItemStack(ModItems.MAGNET));
                player.getInventory().setItem(2, new ItemStack(ModItems.BEDROCK_BREAKER_ELIXIR, 3));
            });
            ctx.waitTicks(20);

            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            System.out.println("[nexus] survival inventory: " + ctx.takeScreenshot("nexus_inventory_survival"));
            ctx.setScreen(() -> null);

            world.getServer().runCommand("gamemode creative @a");
            ctx.waitTicks(10);
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            ctx.runOnClient(mc -> {
                if (mc.gui.screen() instanceof CreativeModeInventoryScreen screen) {
                    CreativeModeTab inventoryTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INVENTORY);
                    try {
                        var selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                        selectTab.setAccessible(true);
                        selectTab.invoke(screen, inventoryTab);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            ctx.waitTicks(5);
            System.out.println("[nexus] creative inventory: " + ctx.takeScreenshot("nexus_inventory_creative"));
            // The two slots sit right of the armor in the creative inventory tab, not on the hotbar row
            String placed = ctx.computeOnClient(mc -> {
                StringBuilder found = new StringBuilder();
                if (mc.gui.screen() instanceof CreativeModeInventoryScreen screen) {
                    for (Slot slot : screen.getMenu().slots) {
                        if (slot.container instanceof ExtraSlots) found.append(slot.getContainerSlot()).append('@').append(slot.x).append(',').append(slot.y).append(' ');
                    }
                }
                return found.toString().trim();
            });
            System.out.println("[nexus] creative extra slots: " + placed);
            // None of them may sit on top of another slot (the inventory rows, armor, offhand, hotbar)
            String overlaps = ctx.computeOnClient(mc -> {
                StringBuilder bad = new StringBuilder();
                if (mc.gui.screen() instanceof CreativeModeInventoryScreen screen) {
                    var slots = screen.getMenu().slots;
                    for (Slot a : slots) {
                        if (!(a.container instanceof ExtraSlots)) continue;
                        for (Slot b : slots) {
                            if (a != b && Math.abs(a.x - b.x) < 16 && Math.abs(a.y - b.y) < 16) {
                                bad.append(a.getContainerSlot()).append(" overlaps slot ").append(b.getContainerSlot()).append(" at ").append(b.x).append(',').append(b.y).append("; ");
                            }
                        }
                    }
                }
                return bad.toString();
            });
            if (!overlaps.isEmpty()) throw new AssertionError("creative extra slots overlap other slots: " + overlaps);
            if (!placed.equals("46@127,6 47@127,33 48@145,6 49@145,33")) throw new AssertionError("creative inventory places the extra slots at " + placed);
            String survival = ctx.computeOnClient(mc -> {
                Slot trinket = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_TRINKET);
                Slot wings = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_WINGS);
                return trinket.x + "," + trinket.y + " " + wings.x + "," + wings.y + " " + wings.getItem() + " " + trinket.getItem();
            });
            System.out.println("[nexus] survival extra slots: " + survival);
            if (!survival.startsWith("77,26 77,44 1 minecraft:elytra 1 nexusessentials:magnet")) {
                throw new AssertionError("survival extra slots: " + survival);
            }
            String ammoAndTotem = ctx.computeOnClient(mc -> {
                Slot arrows = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_ARROWS);
                Slot totem = mc.player.inventoryMenu.getSlot(ExtraSlots.MENU_TOTEM);
                return arrows.getItem() + " " + totem.getItem();
            });
            if (!ammoAndTotem.equals("40 minecraft:arrow 1 minecraft:totem_of_undying")) throw new AssertionError("arrow and totem slots: " + ammoAndTotem);
            ctx.setScreen(() -> null);

            world.getServer().runCommand("gamemode survival @a");
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            ctx.waitTicks(20);
            System.out.println("[nexus] wings from behind: " + ctx.takeScreenshot("nexus_wings_back"));
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            ctx.waitTicks(20);
            System.out.println("[nexus] wings from the front: " + ctx.takeScreenshot("nexus_wings_front"));
            ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        }
    }
}
