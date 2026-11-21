package net.nexium.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.nexium.essentials.hostile.HostileMobSwitch;
import net.nexium.essentials.hostile.HostileMobSwitchBlock;
import net.nexium.essentials.hostile.HostileMobSwitchMenu;
import net.nexium.essentials.registry.ModBlocks;

import java.util.Arrays;

/**
 * The Hostile Mob Switch: one setting for the whole world, flipped by the menu's button or by a redstone signal on a switch, that turns
 * off the spawn checks Peaceful turns off (the spawner's enemy flag and the spawn_monsters game rule's answer), is saved with the
 * world, and counts only hostile mobs when the Farm Assembler makes one. The setting lives in the world's saved data, so each test
 * puts it back as it found it.
 */
public class HostileMobSwitchGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(POS)));
        return player;
    }

    private static HostileMobSwitch setting(GameTestHelper h) {
        return HostileMobSwitch.get(h.getLevel().getServer());
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theMenuButtonFlipsTheWorldsSetting(GameTestHelper h) {
        HostileMobSwitch setting = setting(h);
        boolean before = setting.isBlocking();
        try {
            setting.setBlocking(false);
            ServerPlayer player = player(h);
            HostileMobSwitchMenu menu = new HostileMobSwitchMenu(3, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(POS)));
            h.assertFalse(menu.blocking(), "the menu shows spawning allowed to begin with");
            h.assertTrue(menu.clickMenuButton(player, HostileMobSwitchMenu.BUTTON_TOGGLE), "the button works");
            h.assertTrue(setting.isBlocking() && menu.blocking(), "and turns hostile spawning off, for the world and in the menu");
            h.assertTrue(menu.clickMenuButton(player, HostileMobSwitchMenu.BUTTON_TOGGLE), "pressing again");
            h.assertFalse(setting.isBlocking() || menu.blocking(), "turns it back on");
            h.assertFalse(menu.clickMenuButton(player, 7), "a button number it does not have does nothing");
        } finally {
            setting.setBlocking(before);
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void whileItIsOnNoHostileSpawningIsAllowed(GameTestHelper h) {
        HostileMobSwitch setting = setting(h);
        boolean before = setting.isBlocking();
        try {
            ServerLevel level = h.getLevel();
            setting.setBlocking(false);
            boolean vanilla = level.isSpawningMonsters();
            h.assertTrue(HostileMobSwitch.spawnEnemies(level, true), "with it off, the spawner's flag is vanilla's");
            h.assertFalse(HostileMobSwitch.spawnEnemies(level, false), "and vanilla's no stays no");
            setting.setBlocking(true);
            h.assertFalse(HostileMobSwitch.spawnEnemies(level, true), "with it on, the spawner is told enemies may not spawn");
            h.assertFalse(level.isSpawningMonsters(), "and the spawn_monsters rule's answer (portals, reinforcements, endermites) is no");
            setting.setBlocking(false);
            h.assertTrue(level.isSpawningMonsters() == vanilla, "turned off again, that answer is vanilla's again");
        } finally {
            setting.setBlocking(before);
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theHooksAreInTheGame(GameTestHelper h) {
        // The two mixins merge their handlers into the classes they change; this checks they were applied
        h.assertTrue(Arrays.stream(ServerChunkCache.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("nexium$hostileMobSwitch")),
                "the chunk cache's enemy flag is changed (natural spawning, phantoms and patrols)");
        h.assertTrue(Arrays.stream(ServerLevel.class.getDeclaredMethods()).anyMatch(m -> m.getName().contains("nexium$hostileMobSwitch")),
                "and the spawn_monsters rule's answer");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void aRightClickOpensTheMenuAndRedstoneFollows(GameTestHelper h) {
        HostileMobSwitch setting = setting(h);
        boolean before = setting.isBlocking();
        try {
            setting.setBlocking(false);
            BlockPos pos = h.absolutePos(POS);
            ServerLevel level = h.getLevel();
            level.setBlockAndUpdate(pos, ModBlocks.HOSTILE_MOB_SWITCH.defaultBlockState());
            ServerPlayer player = player(h);
            BlockState state = level.getBlockState(pos);
            state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            h.assertTrue(player.containerMenu instanceof HostileMobSwitchMenu, "a right-click opens its menu, got " + player.containerMenu.getClass().getSimpleName());
            h.assertTrue(player.containerMenu.stillValid(player), "which stays valid while the block is there");
            player.closeContainer();
            h.assertFalse(setting.isBlocking(), "opening it changes nothing");

            BlockPos power = pos.east();
            level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(level.getBlockState(pos).getValue(HostileMobSwitchBlock.POWERED), "a redstone block beside it powers it");
            h.assertTrue(setting.isBlocking(), "and a powered switch turns hostile spawning off");
            level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
            h.assertFalse(level.getBlockState(pos).getValue(HostileMobSwitchBlock.POWERED), "taking the power away");
            h.assertFalse(setting.isBlocking(), "lets it start again");
            h.assertFalse(level.getBlockState(pos).requiresCorrectToolForDrops(), "it drops with any tool, since it costs 140 mobs");
        } finally {
            setting.setBlocking(before);
        }
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void theSettingIsSavedWithTheWorld(GameTestHelper h) {
        HostileMobSwitch on = new HostileMobSwitch();
        on.setBlocking(true);
        var codec = HostileMobSwitch.TYPE.codec();
        var saved = codec.encodeStart(NbtOps.INSTANCE, on).getOrThrow();
        HostileMobSwitch loaded = codec.parse(NbtOps.INSTANCE, saved).getOrThrow();
        h.assertTrue(loaded.isBlocking(), "a switch that was on loads as on");
        HostileMobSwitch offLoaded = codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, new HostileMobSwitch()).getOrThrow()).getOrThrow();
        h.assertFalse(offLoaded.isBlocking(), "and one that was off loads as off");
        h.succeed();
    }

    @GameTest(environment = EssentialsEnvironment.ID)
    public void onlyHostileMobsCount(GameTestHelper h) {
        h.assertTrue(HostileMobSwitch.isHostileMob(new ItemStack(Items.ZOMBIE_SPAWN_EGG)), "a zombie egg is hostile");
        h.assertTrue(HostileMobSwitch.isHostileMob(new ItemStack(Items.CREEPER_SPAWN_EGG)), "a creeper egg is");
        h.assertTrue(HostileMobSwitch.isHostileMob(new ItemStack(Items.ENDERMAN_SPAWN_EGG)), "an enderman egg is");
        h.assertFalse(HostileMobSwitch.isHostileMob(new ItemStack(Items.COW_SPAWN_EGG)), "a cow egg is not");
        h.assertFalse(HostileMobSwitch.isHostileMob(new ItemStack(Items.VILLAGER_SPAWN_EGG)), "a villager egg is not");
        h.assertFalse(HostileMobSwitch.isHostileMob(new ItemStack(Items.STONE)), "stone is not a mob");
        h.succeed();
    }
}
