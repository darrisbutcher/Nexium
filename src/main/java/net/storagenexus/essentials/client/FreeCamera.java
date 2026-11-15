package net.storagenexus.essentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The free camera: a key (K by default) detaches the camera from the player. The player stands where they are, the movement
 * keys and the mouse fly the camera through everything (forward goes where it looks, jump and sneak go up and down, sprint is
 * faster, the scroll wheel changes the speed), and the key again puts the camera back. Nothing you click reaches the world while
 * it is on. It ends by itself if the player is hurt or dies, or the world changes. All on the client; the server is not told.
 */
public final class FreeCamera {
    /** Switches the free camera on and off. K by default; change it under Controls > Key Binds. */
    public static final KeyMapping TOGGLE = new KeyMapping("key.nexusessentials.free_camera", InputConstants.KEY_K, NexusEssentialsClient.CATEGORY);

    /** Blocks per tick at the start (10 a second), the slowest and the fastest the wheel can make it, and what sprint multiplies by. */
    private static final double START_SPEED = 0.5, MIN_SPEED = 0.05, MAX_SPEED = 5.0, SPRINT = 3.0;

    private static @Nullable Entity camera;
    private static @Nullable ClientInput savedInput;
    private static double speed = START_SPEED;

    /** Whether the camera is detached right now. */
    public static boolean active() {
        return camera != null;
    }

    /** The detached camera, or null when it is on the player. */
    public static @Nullable Entity camera() {
        return camera;
    }

    public static double speed() {
        return speed;
    }

    public static void init() {
        KeyMappingHelper.registerKeyMapping(TOGGLE);
        ClientTickEvents.END_CLIENT_TICK.register(FreeCamera::tick);
        // Leaving the world drops it (the player it belonged to is gone with the world)
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            camera = null;
            savedInput = null;
        });
    }

    private static void tick(Minecraft mc) {
        while (TOGGLE.consumeClick()) toggle(mc);
        if (camera == null) return;
        LocalPlayer player = mc.player;
        // Hurt, dead or in another world: back to the player at once
        if (player == null || mc.level == null || camera.level() != mc.level || !player.isAlive() || player.hurtTime > 0) {
            disable(mc);
            return;
        }
        move(mc.options);
    }

    public static void toggle(Minecraft mc) {
        if (camera != null) disable(mc);
        else enable(mc);
    }

    private static void enable(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()) return;
        Marker marker = new Marker(EntityTypes.MARKER, mc.level);
        marker.noPhysics = true;
        marker.setPos(player.getEyePosition());
        marker.setYRot(player.getYRot());
        marker.setXRot(player.getXRot());
        marker.setOldPosAndRot();
        camera = marker;
        // The player's own keys do nothing while the camera is away: an input that never presses anything
        savedInput = player.input;
        player.input = new ClientInput();
        mc.setCameraEntity(marker);
        player.sendOverlayMessage(Component.translatable("message.nexusessentials.free_camera_on"));
    }

    private static void disable(Minecraft mc) {
        camera = null;
        if (mc.player != null) {
            if (savedInput != null) mc.player.input = savedInput;
            mc.setCameraEntity(mc.player);
            mc.player.sendOverlayMessage(Component.translatable("message.nexusessentials.free_camera_off"));
        }
        savedInput = null;
    }

    private static void move(Options options) {
        Entity cam = camera;
        cam.setOldPosAndRot();
        double forward = (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0);
        double strafe = (options.keyLeft.isDown() ? 1 : 0) - (options.keyRight.isDown() ? 1 : 0);
        double vertical = (options.keyJump.isDown() ? 1 : 0) - (options.keyShift.isDown() ? 1 : 0);
        if (forward == 0 && strafe == 0 && vertical == 0) return;
        double yaw = Math.toRadians(cam.getYRot());
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 direction = cam.getLookAngle().scale(forward).add(left.scale(strafe)).add(0, vertical, 0);
        if (direction.lengthSqr() > 1.0E-6) direction = direction.normalize();
        cam.setPos(cam.position().add(direction.scale(speed * (options.keySprint.isDown() ? SPRINT : 1.0))));
    }

    /** The scroll wheel while the camera is away changes how fast it flies instead of the hotbar slot. Returns whether it took the scroll. */
    public static boolean scroll(double amount) {
        if (camera == null || amount == 0) return false;
        speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed * Math.pow(1.25, amount)));
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendOverlayMessage(Component.translatable("message.nexusessentials.free_camera_speed", String.format("%.2f", speed * 20)));
        return true;
    }

    private FreeCamera() {}
}
