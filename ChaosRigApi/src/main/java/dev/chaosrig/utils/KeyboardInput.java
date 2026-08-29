package dev.chaosrig.utils;

import dev.chaosrig.ChaosRigApiClient;
import dev.chaosrig.utils.ping.PingRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyboardInput {
    public final static KeyBinding PING = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.chaosrig.api.ping",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "ChaosRig"));
    protected static int pressPingTick = 0;
    protected static boolean hasKeepingPressPing = false;

    public static void register() {
        if (ChaosRigApiClient.isInit()) throw new RuntimeException("重复注册");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            onPingKey();
        });
    }

    protected static void onPingKey() {
        if (PING.isPressed()) {
            pressPingTick++;
            if (pressPingTick >= 30 && !hasKeepingPressPing) {
                PingRenderer.getInstance().regroup();
                hasKeepingPressPing = true;
            }
        } else {
            if (pressPingTick > 0 && pressPingTick <= 30) {
                PingRenderer.getInstance().ping();
            }
            pressPingTick = 0;
            hasKeepingPressPing = false;
        }
    }

    public static int getPressPingTick() {
        return pressPingTick;
    }
}
